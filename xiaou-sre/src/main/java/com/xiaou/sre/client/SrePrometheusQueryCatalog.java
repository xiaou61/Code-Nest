package com.xiaou.sre.client;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SRE PromQL 固定白名单。
 *
 * <p>告警名称只用于选择预先审核过的查询，绝不直接进入 PromQL。</p>
 *
 * @author xiaou
 */
@Component
public class SrePrometheusQueryCatalog {

    private static final String DEFAULT_ALERT = "__default__";

    private static final String TOOL_PREFIX = "prom_";

    private final Map<String, QuerySpec> specs = createSpecs();
    private final Map<String, QuerySpec> toolSpecs = createToolSpecs(specs);

    public QuerySpec find(String alertName) {
        if (StringUtils.hasText(alertName)) {
            QuerySpec exact = specs.get(alertName.trim());
            if (exact != null) {
                return exact;
            }
        }
        return specs.get(DEFAULT_ALERT);
    }

    public boolean isAllowed(QuerySpec querySpec) {
        return querySpec != null && specs.containsValue(querySpec);
    }

    public QuerySpec findByToolKey(String toolKey) {
        if (!StringUtils.hasText(toolKey)) {
            return null;
        }
        return toolSpecs.get(toolKey.trim().toLowerCase(Locale.ROOT));
    }

    public List<String> toolKeys() {
        return List.copyOf(toolSpecs.keySet());
    }

    public String toolKeyForAlert(String alertName) {
        QuerySpec querySpec = find(alertName);
        return querySpec == null ? null : TOOL_PREFIX + querySpec.sourceRef();
    }

    private Map<String, QuerySpec> createSpecs() {
        Map<String, QuerySpec> result = new LinkedHashMap<>();
        result.put("CodeNestTargetDown", new QuerySpec("target_up", "up{job=\"code-nest\"}"));
        result.put("CodeNestPublicEndpointDown", new QuerySpec("public_probe_up", "probe_success{job=\"blackbox-http\"}"));
        result.put("CodeNestHighHttpErrorRatio", new QuerySpec(
                "http_5xx_rate",
                "sum(rate(http_server_requests_seconds_count{job=\"code-nest\",status=~\"5..\"}[5m]))"));
        result.put("CodeNestHighHttpLatencyP95", new QuerySpec(
                "http_latency_p95",
                "histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job=\"code-nest\"}[5m])))"));
        result.put("CodeNestHighJvmHeapUsage", new QuerySpec(
                "jvm_heap_usage",
                "sum(jvm_memory_used_bytes{job=\"code-nest\",area=\"heap\"}) / clamp_min(sum(jvm_memory_max_bytes{job=\"code-nest\",area=\"heap\"}), 1)"));
        result.put("CodeNestHostDiskLow", new QuerySpec(
                "host_disk_available_ratio",
                "node_filesystem_avail_bytes{job=\"node-exporter\",fstype!~\"tmpfs|overlay\"} / node_filesystem_size_bytes{job=\"node-exporter\",fstype!~\"tmpfs|overlay\"}"));
        result.put("CodeNestHostDiskCritical", result.get("CodeNestHostDiskLow"));
        result.put("CodeNestHostCpuHigh", new QuerySpec(
                "host_cpu_usage",
                "1 - avg by (instance) (rate(node_cpu_seconds_total{job=\"node-exporter\",mode=\"idle\"}[5m]))"));
        result.put(DEFAULT_ALERT, new QuerySpec("target_up", "up{job=\"code-nest\"}"));
        return result;
    }

    private Map<String, QuerySpec> createToolSpecs(Map<String, QuerySpec> alertSpecs) {
        Map<String, QuerySpec> result = new LinkedHashMap<>();
        alertSpecs.values().forEach(spec -> result.putIfAbsent(TOOL_PREFIX + spec.sourceRef(), spec));
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(result));
    }

    public record QuerySpec(String sourceRef, String promQl) {
    }
}
