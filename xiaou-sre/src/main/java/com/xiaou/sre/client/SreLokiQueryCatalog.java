package com.xiaou.sre.client;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SRE LogQL 固定白名单。
 *
 * <p>告警名称只用于选择预先审核过的查询，绝不直接进入 LogQL。</p>
 *
 * @author xiaou
 */
@Component
public class SreLokiQueryCatalog {

    private static final String DEFAULT_ALERT = "__default__";

    private final Map<String, QuerySpec> specs = createSpecs();

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

    private Map<String, QuerySpec> createSpecs() {
        Map<String, QuerySpec> result = new LinkedHashMap<>();
        result.put("CodeNestTargetDown", new QuerySpec(
                "application_errors", "{job=\"code-nest\"} |~ \"(?i)(error|exception|timeout)\""));
        result.put("CodeNestPublicEndpointDown", new QuerySpec(
                "public_probe_errors", "{job=\"blackbox-http\"} |~ \"(?i)(error|timeout|5[0-9]{2})\""));
        result.put("CodeNestHighHttpErrorRatio", new QuerySpec(
                "application_http_errors", "{job=\"code-nest\"} |~ \"(?i)(error|exception|5[0-9]{2})\""));
        result.put("CodeNestHighHttpLatencyP95", new QuerySpec(
                "application_latency_errors", "{job=\"code-nest\"} |~ \"(?i)(slow|timeout|deadline|latency)\""));
        result.put("CodeNestHighJvmHeapUsage", new QuerySpec(
                "application_memory_errors", "{job=\"code-nest\"} |~ \"(?i)(outofmemory|heap|gc|memory)\""));
        result.put("CodeNestHostDiskLow", new QuerySpec(
                "host_disk_errors", "{job=\"node-exporter\"} |~ \"(?i)(disk|filesystem|no space|read-only)\""));
        result.put("CodeNestHostDiskCritical", result.get("CodeNestHostDiskLow"));
        result.put("CodeNestHostCpuHigh", new QuerySpec(
                "host_cpu_errors", "{job=\"node-exporter\"} |~ \"(?i)(cpu|load|oom|killed)\""));
        result.put(DEFAULT_ALERT, new QuerySpec(
                "application_errors", "{job=\"code-nest\"} |~ \"(?i)(error|exception|timeout)\""));
        return result;
    }

    public record QuerySpec(String sourceRef, String logQl) {
    }
}
