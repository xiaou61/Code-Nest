package com.xiaou.system.agent.tools;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 智能体工具调用指标的统一只读视图。
 *
 * @author xiaou
 */
@RequiredArgsConstructor
final class AgentToolMetricsSnapshotSupport {

    static final double DEFAULT_SLOW_THRESHOLD_MS = 1000D;
    static final double DEFAULT_ERROR_THRESHOLD = 1D;
    static final double DEFAULT_BLOCKED_THRESHOLD = 1D;

    private static final String INVOCATIONS_METER = "xiaou.agent.tool.invocations";
    private static final String DURATION_METER = "xiaou.agent.tool.duration";

    private final MeterRegistry meterRegistry;

    MetricsSnapshot snapshot() {
        return snapshot(DEFAULT_SLOW_THRESHOLD_MS, DEFAULT_ERROR_THRESHOLD, DEFAULT_BLOCKED_THRESHOLD);
    }

    MetricsSnapshot snapshot(double slowThresholdMs, double errorThreshold, double blockedThreshold) {
        List<Map<String, Object>> series = metricSeries();
        double invocationCount = sumCount(series);
        double errorCount = sumOutcome(series, "error");
        double blockedCount = sumOutcome(series, "blocked");
        List<Map<String, Object>> slowSeries = series.stream()
                .filter(item -> ((Double) item.get("maxDurationMs")) >= slowThresholdMs)
                .toList();
        return new MetricsSnapshot(
                series,
                invocationCount,
                errorCount,
                blockedCount,
                slowSeries,
                slowThresholdMs,
                errorThreshold,
                blockedThreshold
        );
    }

    private List<Map<String, Object>> metricSeries() {
        return meterRegistry.getMeters().stream()
                .filter(meter -> INVOCATIONS_METER.equals(meter.getId().getName()))
                .filter(Counter.class::isInstance)
                .map(Counter.class::cast)
                .map(this::series)
                .sorted(Comparator
                        .<Map<String, Object>, String>comparing(item -> String.valueOf(item.get("outcome")))
                        .thenComparing(item -> String.valueOf(item.get("tool")))
                        .thenComparing(item -> String.valueOf(item.get("phase"))))
                .toList();
    }

    private Map<String, Object> series(Counter counter) {
        Meter.Id id = counter.getId();
        Timer timer = meterRegistry.find(DURATION_METER)
                .tag("tool", tag(id, "tool"))
                .tag("phase", tag(id, "phase"))
                .tag("outcome", tag(id, "outcome"))
                .tag("risk_level", tag(id, "risk_level"))
                .tag("risk_category", tag(id, "risk_category"))
                .timer();
        double count = counter.count();
        double totalDurationMs = timer == null ? 0D : timer.totalTime(TimeUnit.MILLISECONDS);
        double maxDurationMs = timer == null ? 0D : timer.max(TimeUnit.MILLISECONDS);
        double meanDurationMs = timer == null || timer.count() <= 0 ? 0D : timer.mean(TimeUnit.MILLISECONDS);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("tool", tag(id, "tool"));
        item.put("phase", tag(id, "phase"));
        item.put("outcome", tag(id, "outcome"));
        item.put("riskLevel", tag(id, "risk_level"));
        item.put("riskCategory", tag(id, "risk_category"));
        item.put("count", count);
        item.put("durationCount", timer == null ? 0D : (double) timer.count());
        item.put("totalDurationMs", totalDurationMs);
        item.put("maxDurationMs", maxDurationMs);
        item.put("meanDurationMs", meanDurationMs);
        return item;
    }

    private String tag(Meter.Id id, String key) {
        String value = id.getTag(key);
        return value == null ? "unknown" : value;
    }

    private double sumCount(List<Map<String, Object>> series) {
        return series.stream()
                .mapToDouble(item -> (Double) item.get("count"))
                .sum();
    }

    private double sumOutcome(List<Map<String, Object>> series, String outcome) {
        return series.stream()
                .filter(item -> outcome.equalsIgnoreCase(String.valueOf(item.get("outcome"))))
                .mapToDouble(item -> (Double) item.get("count"))
                .sum();
    }

    static final class MetricsSnapshot {

        private final List<Map<String, Object>> series;
        private final double invocationCount;
        private final double errorCount;
        private final double blockedCount;
        private final List<Map<String, Object>> slowSeries;
        private final double slowThresholdMs;
        private final double errorThreshold;
        private final double blockedThreshold;

        private MetricsSnapshot(List<Map<String, Object>> series,
                                double invocationCount,
                                double errorCount,
                                double blockedCount,
                                List<Map<String, Object>> slowSeries,
                                double slowThresholdMs,
                                double errorThreshold,
                                double blockedThreshold) {
            this.series = series;
            this.invocationCount = invocationCount;
            this.errorCount = errorCount;
            this.blockedCount = blockedCount;
            this.slowSeries = slowSeries;
            this.slowThresholdMs = slowThresholdMs;
            this.errorThreshold = errorThreshold;
            this.blockedThreshold = blockedThreshold;
        }

        double invocationCount() {
            return invocationCount;
        }

        double errorCount() {
            return errorCount;
        }

        double blockedCount() {
            return blockedCount;
        }

        int slowSeriesCount() {
            return slowSeries.size();
        }

        Map<String, Object> summaryData() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("invocationCount", invocationCount);
            data.put("seriesCount", series.size());
            data.put("hasMetrics", invocationCount > 0D);
            data.put("series", seriesByCountDesc());
            return data;
        }

        Map<String, Object> healthData() {
            List<Map<String, Object>> checks = healthChecks();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", healthStatus(checks));
            data.put("invocationCount", invocationCount);
            data.put("seriesCount", series.size());
            data.put("errorCount", errorCount);
            data.put("blockedCount", blockedCount);
            data.put("slowSeriesCount", slowSeries.size());
            data.put("slowSeriesThresholdMs", slowThresholdMs);
            data.put("checks", checks);
            data.put("series", series);
            data.put("slowSeries", slowSeries);
            return data;
        }

        Map<String, Object> alertsData() {
            List<Map<String, Object>> alerts = alerts();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", alertsStatus(alerts));
            data.put("invocationCount", invocationCount);
            data.put("seriesCount", series.size());
            data.put("errorCount", errorCount);
            data.put("blockedCount", blockedCount);
            data.put("slowSeriesCount", slowSeries.size());
            data.put("slowThresholdMs", slowThresholdMs);
            data.put("errorThreshold", errorThreshold);
            data.put("blockedThreshold", blockedThreshold);
            data.put("alertCount", alerts.size());
            data.put("alertCountBySeverity", alertCountBySeverity(alerts));
            data.put("rules", rules());
            data.put("alerts", alerts);
            data.put("series", series);
            data.put("slowSeries", slowSeries);
            return data;
        }

        private List<Map<String, Object>> seriesByCountDesc() {
            return series.stream()
                    .sorted(Comparator
                            .<Map<String, Object>, Double>comparing(item -> (Double) item.get("count"))
                            .reversed()
                            .thenComparing(item -> String.valueOf(item.get("tool"))))
                    .toList();
        }

        private List<Map<String, Object>> healthChecks() {
            List<Map<String, Object>> checks = new ArrayList<>();
            addCheck(checks,
                    "metrics.present",
                    invocationCount > 0D ? "HEALTHY" : "WARN",
                    invocationCount > 0D ? "已采集到智能体工具调用指标。" : "暂未采集到智能体工具调用指标，无法判断运行质量。",
                    Map.of("invocationCount", invocationCount));
            addCheck(checks,
                    "metrics.errors",
                    errorCount > 0D ? "ALERT" : "HEALTHY",
                    errorCount > 0D ? "存在工具执行或预览错误，需要优先排查。" : "未发现工具错误指标。",
                    Map.of("errorCount", errorCount));
            addCheck(checks,
                    "metrics.blocked",
                    blockedCount > 0D ? "WARN" : "HEALTHY",
                    blockedCount > 0D ? "存在预览阻断，建议结合审计确认是否为预期拦截。" : "未发现预览阻断指标。",
                    Map.of("blockedCount", blockedCount));
            addCheck(checks,
                    "metrics.slowSeries",
                    slowSeries.isEmpty() ? "HEALTHY" : "WARN",
                    slowSeries.isEmpty() ? "未发现超过阈值的慢调用指标。" : "存在超过阈值的慢调用指标。",
                    Map.of("slowSeriesCount", slowSeries.size(), "thresholdMs", slowThresholdMs));
            return checks;
        }

        private String healthStatus(List<Map<String, Object>> checks) {
            if (checks.stream().anyMatch(check -> "ALERT".equals(check.get("status")))) {
                return "ALERT";
            }
            if (checks.stream().anyMatch(check -> "WARN".equals(check.get("status")))) {
                return "WARN";
            }
            return "HEALTHY";
        }

        private void addCheck(List<Map<String, Object>> checks,
                              String id,
                              String status,
                              String message,
                              Map<String, Object> details) {
            Map<String, Object> check = new LinkedHashMap<>();
            check.put("id", id);
            check.put("status", status);
            check.put("message", message);
            check.put("details", details);
            checks.add(check);
        }

        private List<Map<String, Object>> rules() {
            List<Map<String, Object>> rules = new ArrayList<>();
            rules.add(rule("agent.tool.metrics.missing", "WARN", "invocationCount == 0", 1D,
                    "没有采集到工具调用指标时触发，表示观测链路缺少样本。"));
            rules.add(rule("agent.tool.errors", "ALERT", "errorCount >= errorThreshold", errorThreshold,
                    "错误调用次数达到阈值时触发，表示目标工具或外部依赖可能异常。"));
            rules.add(rule("agent.tool.blocked", "WARN", "blockedCount >= blockedThreshold", blockedThreshold,
                    "预览或策略阻断达到阈值时触发，表示需要结合审计确认是否为预期拦截。"));
            rules.add(rule("agent.tool.slow", "WARN", "maxDurationMs >= slowThresholdMs", slowThresholdMs,
                    "任一指标序列最大耗时达到慢调用阈值时触发。"));
            return rules;
        }

        private Map<String, Object> rule(String id, String severity, String condition, double threshold, String description) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("id", id);
            rule.put("severity", severity);
            rule.put("condition", condition);
            rule.put("threshold", threshold);
            rule.put("description", description);
            return rule;
        }

        private List<Map<String, Object>> alerts() {
            List<Map<String, Object>> alerts = new ArrayList<>();
            if (invocationCount <= 0D) {
                addAlert(alerts,
                        "agent.tool.metrics.missing",
                        "WARN",
                        "暂未采集到智能体工具调用指标，dashboard 或告警系统无法判断真实运行质量。",
                        Map.of("invocationCount", invocationCount, "expectedMinimum", 1D));
            }
            if (errorCount >= errorThreshold) {
                addAlert(alerts,
                        "agent.tool.errors",
                        "ALERT",
                        "智能体工具错误调用次数达到告警阈值。",
                        Map.of("errorCount", errorCount, "threshold", errorThreshold));
            }
            if (blockedCount >= blockedThreshold) {
                addAlert(alerts,
                        "agent.tool.blocked",
                        "WARN",
                        "智能体工具 blocked 调用次数达到告警阈值。",
                        Map.of("blockedCount", blockedCount, "threshold", blockedThreshold));
            }
            if (!slowSeries.isEmpty()) {
                addAlert(alerts,
                        "agent.tool.slow",
                        "WARN",
                        "存在超过慢调用阈值的智能体工具指标序列。",
                        Map.of("slowSeriesCount", slowSeries.size(), "thresholdMs", slowThresholdMs));
            }
            return alerts;
        }

        private void addAlert(List<Map<String, Object>> alerts,
                              String id,
                              String severity,
                              String message,
                              Map<String, Object> details) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("id", id);
            alert.put("severity", severity);
            alert.put("status", "FIRING");
            alert.put("message", message);
            alert.put("details", details);
            alerts.add(alert);
        }

        private Map<String, Long> alertCountBySeverity(List<Map<String, Object>> alerts) {
            Map<String, Long> counts = new LinkedHashMap<>();
            counts.put("ALERT", alerts.stream().filter(alert -> "ALERT".equals(alert.get("severity"))).count());
            counts.put("WARN", alerts.stream().filter(alert -> "WARN".equals(alert.get("severity"))).count());
            return counts;
        }

        private String alertsStatus(List<Map<String, Object>> alerts) {
            if (alerts.stream().anyMatch(alert -> "ALERT".equals(alert.get("severity")))) {
                return "ALERT";
            }
            if (alerts.stream().anyMatch(alert -> "WARN".equals(alert.get("severity")))) {
                return "WARN";
            }
            return "OK";
        }
    }
}
