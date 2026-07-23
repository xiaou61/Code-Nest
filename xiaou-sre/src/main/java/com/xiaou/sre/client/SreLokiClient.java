package com.xiaou.sre.client;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.sre.config.SreLokiProperties;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Loki HTTP API 只读客户端。
 *
 * <p>客户端只接收服务端代码生成的固定 LogQL，并对返回流和日志行做硬限制。
 * 它不暴露任意日志查询接口，也不会把告警标签直接拼接到 LogQL。</p>
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class SreLokiClient {

    private static final String QUERY_RANGE_PATH = "/loki/api/v1/query_range";
    private static final int MAX_STREAM_ENTRIES = 50;
    private static final int MAX_LABEL_LENGTH = 256;
    private static final int MAX_TIMESTAMP_LENGTH = 64;
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)(\\bauthorization\\s*[:=]\\s*bearer\\s+)([^\\s,;\\\"'}]+)");
    private static final Pattern SENSITIVE_VALUE_PATTERN = Pattern.compile(
            "(?i)(\\\"?(?:password|passwd|secret|token|api[_-]?key|access[_-]?token|refresh[_-]?token)\\\"?\\s*[:=]\\s*\\\"?)([^\\s,;\\\"'}]+)");

    private final SreLokiProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final SreLokiQueryCatalog queryCatalog;

    private volatile RestClient restClient;

    public SreLokiQueryResult query(SreLokiQueryCatalog.QuerySpec querySpec,
                                    Instant start,
                                    Instant end) {
        if (!properties.isEnabled()) {
            throw new SreLokiException("Loki 证据采集未启用");
        }
        if (!queryCatalog.isAllowed(querySpec)) {
            throw new SreLokiException("LogQL 不在 SRE 固定查询白名单中");
        }
        if (querySpec == null || !StringUtils.hasText(querySpec.logQl()) || querySpec.logQl().length() > 2_000) {
            throw new SreLokiException("LogQL 为空或超过长度限制");
        }
        validateWindow(start, end);

        final String body;
        try {
            body = getRestClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(QUERY_RANGE_PATH)
                            .queryParam("query", "{logQl}")
                            .queryParam("start", toUnixNanos(start))
                            .queryParam("end", toUnixNanos(end))
                            .queryParam("limit", properties.normalizedMaxResults())
                            .queryParam("direction", "backward")
                            .build(querySpec.logQl()))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw new SreLokiException("Loki HTTP 查询失败", exception);
        } catch (RestClientException exception) {
            throw new SreLokiException("Loki 查询连接失败", exception);
        }

        if (!StringUtils.hasText(body)) {
            throw new SreLokiException("Loki 返回为空");
        }
        if (body.getBytes(StandardCharsets.UTF_8).length > properties.normalizedMaxResponseBytes()) {
            throw new SreLokiException("Loki 返回超过大小限制");
        }
        if (!JsonUtils.isValidJson(body)) {
            throw new SreLokiException("Loki 返回不是合法 JSON");
        }

        Map<String, Object> response = JsonUtils.parseMap(body);
        if (response == null || !"success".equals(response.get("status"))) {
            throw new SreLokiException("Loki 查询返回失败状态");
        }
        Map<String, Object> data = JsonUtils.toObjectMap(response.get("data"));
        if (data == null || !(data.get("result") instanceof List<?> rawResults)) {
            throw new SreLokiException("Loki 返回缺少 result");
        }

        SreLokiQueryResult result = new SreLokiQueryResult();
        result.setResultType(boundedText(data.get("resultType"), 64));
        result.setResults(sanitizeResults(rawResults, result));
        return result;
    }

    private void validateWindow(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new SreLokiException("Loki 查询时间窗口不合法");
        }
        Duration duration;
        try {
            duration = Duration.between(start, end);
        } catch (RuntimeException exception) {
            throw new SreLokiException("Loki 查询时间窗口不合法", exception);
        }
        if (duration.compareTo(Duration.ofMinutes(properties.normalizedMaxRangeMinutes())) > 0) {
            throw new SreLokiException("Loki 查询时间窗口超过限制");
        }
    }

    private long toUnixNanos(Instant instant) {
        try {
            return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1_000_000_000L), instant.getNano());
        } catch (ArithmeticException exception) {
            throw new SreLokiException("Loki 查询时间超出支持范围", exception);
        }
    }

    private List<Map<String, Object>> sanitizeResults(List<?> rawResults,
                                                       SreLokiQueryResult result) {
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
            item.put("stream", sanitizeLabels(raw.get("stream")));
            if (raw.containsKey("values")) {
                item.put("values", sanitizeValues(raw.get("values"), result, maxResults));
            }
            sanitized.add(item);
            accepted++;
        }
        if (rawResults.size() > maxResults) {
            result.setTruncated(true);
        }
        return sanitized;
    }

    private Map<String, String> sanitizeLabels(Object value) {
        Map<String, String> labels = JsonUtils.toStringMap(value);
        if (labels == null || labels.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        int accepted = 0;
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (accepted >= MAX_STREAM_ENTRIES) {
                break;
            }
            String key = boundedText(entry.getKey(), MAX_LABEL_LENGTH);
            String text = boundedText(entry.getValue(), MAX_LABEL_LENGTH);
            if (StringUtils.hasText(key)) {
                result.put(key, text);
                accepted++;
            }
        }
        return result;
    }

    private List<Object> sanitizeValues(Object value,
                                        SreLokiQueryResult result,
                                        int maxValues) {
        if (!(value instanceof List<?> rawValues)) {
            return List.of();
        }
        List<Object> sanitized = new ArrayList<>();
        for (int i = 0; i < rawValues.size() && i < maxValues; i++) {
            Object rawValue = rawValues.get(i);
            if (!(rawValue instanceof List<?> pair)) {
                continue;
            }
            List<Object> sample = new ArrayList<>();
            if (!pair.isEmpty()) {
                sample.add(boundedText(pair.get(0), MAX_TIMESTAMP_LENGTH));
            }
            if (pair.size() > 1) {
                sample.add(redactLogLine(pair.get(1)));
            }
            sanitized.add(sample);
        }
        if (rawValues.size() > maxValues) {
            result.setTruncated(true);
        }
        return sanitized;
    }

    private String redactLogLine(Object value) {
        String line = boundedText(value, properties.normalizedMaxLineLength());
        line = BEARER_PATTERN.matcher(line).replaceAll("$1[REDACTED]");
        return SENSITIVE_VALUE_PATTERN.matcher(line).replaceAll("$1[REDACTED]");
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
            throw new SreLokiException("Loki endpoint 未配置");
        }
        try {
            URI endpoint = URI.create(properties.getEndpoint().trim());
            if (!("http".equalsIgnoreCase(endpoint.getScheme())
                    || "https".equalsIgnoreCase(endpoint.getScheme()))
                    || !StringUtils.hasText(endpoint.getHost())
                    || endpoint.getUserInfo() != null
                    || endpoint.getQuery() != null
                    || endpoint.getFragment() != null) {
                throw new SreLokiException("Loki endpoint 不是受支持的 HTTP 地址");
            }
            return endpoint;
        } catch (IllegalArgumentException exception) {
            throw new SreLokiException("Loki endpoint 不是合法 URI", exception);
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
