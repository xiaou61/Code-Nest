package com.xiaou.sre.client;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.sre.config.SrePrometheusProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prometheus HTTP API 只读客户端。
 *
 * <p>客户端只接收服务端代码生成的固定 PromQL；没有对外暴露任意查询接口，
 * 也不会把告警标签直接拼接到 PromQL。</p>
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class SrePrometheusClient {

    private static final String QUERY_PATH = "/api/v1/query";
    private static final int MAX_METRIC_ENTRIES = 50;
    private static final int MAX_TEXT_LENGTH = 256;

    private final SrePrometheusProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final SrePrometheusQueryCatalog queryCatalog;

    private volatile RestClient restClient;

    public SrePrometheusQueryResult query(SrePrometheusQueryCatalog.QuerySpec querySpec) {
        if (!properties.isEnabled()) {
            throw new SrePrometheusException("Prometheus 证据采集未启用");
        }
        if (!queryCatalog.isAllowed(querySpec)) {
            throw new SrePrometheusException("PromQL 不在 SRE 固定查询白名单中");
        }
        String promQl = querySpec.promQl();
        if (!StringUtils.hasText(promQl) || promQl.length() > 2_000) {
            throw new SrePrometheusException("PromQL 为空或超过长度限制");
        }

        final String body;
        try {
            body = getRestClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(QUERY_PATH)
                            .queryParam("query", "{promQl}")
                            .build(promQl))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw new SrePrometheusException("Prometheus HTTP 查询失败", exception);
        } catch (RestClientException exception) {
            throw new SrePrometheusException("Prometheus 查询连接失败", exception);
        }

        if (!StringUtils.hasText(body)) {
            throw new SrePrometheusException("Prometheus 返回为空");
        }
        if (body.getBytes(StandardCharsets.UTF_8).length > properties.normalizedMaxResponseBytes()) {
            throw new SrePrometheusException("Prometheus 返回超过大小限制");
        }
        if (!JsonUtils.isValidJson(body)) {
            throw new SrePrometheusException("Prometheus 返回不是合法 JSON");
        }

        Map<String, Object> response = JsonUtils.parseMap(body);
        if (response == null || !"success".equals(response.get("status"))) {
            throw new SrePrometheusException("Prometheus 查询返回失败状态");
        }
        Map<String, Object> data = JsonUtils.toObjectMap(response.get("data"));
        if (data == null || !(data.get("result") instanceof List<?> rawResults)) {
            throw new SrePrometheusException("Prometheus 返回缺少 result");
        }

        SrePrometheusQueryResult result = new SrePrometheusQueryResult();
        result.setResultType(boundedText(data.get("resultType"), 64));
        result.setResults(sanitizeResults(rawResults, result));
        return result;
    }

    private List<Map<String, Object>> sanitizeResults(List<?> rawResults,
                                                       SrePrometheusQueryResult result) {
        int maxResults = properties.normalizedMaxResults();
        List<Map<String, Object>> sanitized = new ArrayList<>();
        int accepted = 0;
        for (Object rawResult : rawResults) {
            if (accepted >= maxResults) {
                result.setTruncated(true);
                break;
            }
            Map<String, Object> raw = JsonUtils.toObjectMap(rawResult);
            if (raw == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("metric", sanitizeMetric(raw.get("metric")));
            if (raw.containsKey("value")) {
                item.put("value", sanitizeSample(raw.get("value")));
            }
            if (raw.containsKey("values")) {
                item.put("values", sanitizeSamples(raw.get("values"), maxResults));
            }
            sanitized.add(item);
            accepted++;
        }
        if (rawResults.size() > maxResults) {
            result.setTruncated(true);
        }
        return sanitized;
    }

    private Map<String, String> sanitizeMetric(Object value) {
        Map<String, String> metric = JsonUtils.toStringMap(value);
        if (metric == null || metric.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        int accepted = 0;
        for (Map.Entry<String, String> entry : metric.entrySet()) {
            if (accepted >= MAX_METRIC_ENTRIES) {
                break;
            }
            String key = boundedText(entry.getKey(), MAX_TEXT_LENGTH);
            String text = boundedText(entry.getValue(), MAX_TEXT_LENGTH);
            if (StringUtils.hasText(key)) {
                result.put(key, text);
                accepted++;
            }
        }
        return result;
    }

    private List<Object> sanitizeSamples(Object value, int maxSamples) {
        if (!(value instanceof List<?> rawSamples)) {
            return List.of();
        }
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < rawSamples.size() && i < maxSamples; i++) {
            result.add(sanitizeSample(rawSamples.get(i)));
        }
        return result;
    }

    private Object sanitizeSample(Object value) {
        if (!(value instanceof List<?> rawSample)) {
            return boundedText(value, MAX_TEXT_LENGTH);
        }
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < rawSample.size() && i < 2; i++) {
            Object item = rawSample.get(i);
            result.add(item instanceof Number ? item : boundedText(item, MAX_TEXT_LENGTH));
        }
        return result;
    }

    private String boundedText(Object value, int maxLength) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private RestClient getRestClient() {
        RestClient localRef = restClient;
        if (localRef != null) {
            return localRef;
        }
        synchronized (this) {
            if (restClient == null) {
                URI endpoint = validatedEndpoint();
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout(Duration.ofMillis(properties.normalizedConnectTimeoutMillis()));
                requestFactory.setReadTimeout(Duration.ofMillis(properties.normalizedReadTimeoutMillis()));
                restClient = restClientBuilder.clone()
                        .baseUrl(trimTrailingSlash(endpoint.toString()))
                        .requestFactory(requestFactory)
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .build();
            }
            return restClient;
        }
    }

    private URI validatedEndpoint() {
        if (!StringUtils.hasText(properties.getEndpoint())) {
            throw new SrePrometheusException("Prometheus endpoint 未配置");
        }
        try {
            URI endpoint = URI.create(properties.getEndpoint().trim());
            if (!("http".equalsIgnoreCase(endpoint.getScheme())
                    || "https".equalsIgnoreCase(endpoint.getScheme()))
                    || !StringUtils.hasText(endpoint.getHost())
                    || endpoint.getUserInfo() != null
                    || endpoint.getQuery() != null
                    || endpoint.getFragment() != null) {
                throw new SrePrometheusException("Prometheus endpoint 不是受支持的 HTTP 地址");
            }
            return endpoint;
        } catch (IllegalArgumentException exception) {
            throw new SrePrometheusException("Prometheus endpoint 不是合法 URI", exception);
        }
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
