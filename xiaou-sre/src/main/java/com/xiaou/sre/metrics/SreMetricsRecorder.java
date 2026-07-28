package com.xiaou.sre.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Low-cardinality metrics for SRE ingestion, evidence, investigation and queue processing.
 *
 * @author xiaou
 */
@Component
public class SreMetricsRecorder {

    private static final Set<String> OUTCOMES = Set.of(
            "succeeded", "degraded", "failed", "skipped", "unavailable", "duplicate");
    private static final Set<String> GENERATION_MODES = Set.of("ai", "fallback", "unknown");
    private static final Set<String> QUEUES = Set.of("outbox", "evaluation", "investigation");
    private static final Set<String> QUEUE_EVENTS = Set.of(
            "enqueued", "claimed", "retry", "lease_recovered", "terminal_failure", "deadline_exceeded");

    private final MeterRegistry meterRegistry;
    private final AtomicLong openIncidents = new AtomicLong();
    private final AtomicLong outboxBacklog = new AtomicLong();
    private final AtomicLong outboxOldestAgeSeconds = new AtomicLong();
    private final AtomicLong evaluationBacklog = new AtomicLong();
    private final AtomicLong evaluationOldestAgeSeconds = new AtomicLong();
    private final AtomicLong activeInvestigations = new AtomicLong();

    public SreMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        gauge("xiaou.sre.incidents.open", "当前未关闭事故数", openIncidents, "incidents");
        gauge("xiaou.sre.outbox.backlog", "SRE Outbox 待处理与处理中事件数", outboxBacklog, "events");
        gauge("xiaou.sre.outbox.oldest.age", "SRE Outbox 最老待处理事件年龄", outboxOldestAgeSeconds, "seconds");
        gauge("xiaou.sre.evaluation.backlog", "RCA 评测排队与执行中运行数", evaluationBacklog, "runs");
        gauge("xiaou.sre.evaluation.oldest.age", "RCA 评测最老排队运行年龄", evaluationOldestAgeSeconds, "seconds");
        gauge("xiaou.sre.investigation.active", "当前执行中的 RCA 调查数", activeInvestigations, "runs");
    }

    public void recordAlertIngestion(String outcome, int alertCount, long durationNanos) {
        String safeOutcome = outcome(outcome);
        recordDurationAndCount(
                "xiaou.sre.alert.ingestion.duration",
                "xiaou.sre.alert.ingestion.requests",
                "Alertmanager 告警接收耗时",
                "Alertmanager 告警接收次数",
                safeOutcome,
                durationNanos);
        DistributionSummary.builder("xiaou.sre.alert.ingestion.alerts")
                .description("单次 Webhook 接收的告警数")
                .baseUnit("alerts")
                .tag("outcome", safeOutcome)
                .register(meterRegistry)
                .record(Math.max(alertCount, 0));
    }

    public void recordEvidenceCollection(String outcome, long durationNanos) {
        recordDurationAndCount(
                "xiaou.sre.evidence.collection.duration",
                "xiaou.sre.evidence.collection.runs",
                "SRE 证据采集耗时",
                "SRE 证据采集次数",
                outcome(outcome),
                durationNanos);
    }

    public void recordEvaluation(String outcome, long durationNanos) {
        recordDurationAndCount(
                "xiaou.sre.evaluation.duration",
                "xiaou.sre.evaluation.runs",
                "RCA 离线评测耗时",
                "RCA 离线评测次数",
                outcome(outcome),
                durationNanos);
    }

    public void recordInvestigation(String outcome,
                                    String generationMode,
                                    int rounds,
                                    long durationNanos) {
        String safeOutcome = outcome(outcome);
        String safeMode = allowed(generationMode, GENERATION_MODES, "unknown");
        Timer.builder("xiaou.sre.investigation.duration")
                .description("SRE RCA 调查总耗时")
                .tag("outcome", safeOutcome)
                .tag("generation_mode", safeMode)
                .register(meterRegistry)
                .record(Duration.ofNanos(Math.max(durationNanos, 0L)));
        Counter.builder("xiaou.sre.investigation.runs")
                .description("SRE RCA 调查运行次数")
                .tag("outcome", safeOutcome)
                .tag("generation_mode", safeMode)
                .register(meterRegistry)
                .increment();
        DistributionSummary.builder("xiaou.sre.investigation.rounds")
                .description("SRE RCA 只读取证轮数")
                .baseUnit("rounds")
                .tag("outcome", safeOutcome)
                .register(meterRegistry)
                .record(Math.max(rounds, 0));
    }

    public void recordReadOnlyTool(String toolKey, String outcome, long durationNanos) {
        String safeTool = tool(toolKey);
        String safeOutcome = outcome(outcome);
        Timer.builder("xiaou.sre.investigation.tool.duration")
                .description("SRE 调查只读工具耗时")
                .tag("tool", safeTool)
                .tag("outcome", safeOutcome)
                .register(meterRegistry)
                .record(Duration.ofNanos(Math.max(durationNanos, 0L)));
        Counter.builder("xiaou.sre.investigation.tool.calls")
                .description("SRE 调查只读工具调用次数")
                .tag("tool", safeTool)
                .tag("outcome", safeOutcome)
                .register(meterRegistry)
                .increment();
    }

    public void incrementQueueEvent(String queue, String event, long amount) {
        if (amount <= 0) {
            return;
        }
        Counter.builder("xiaou.sre.queue.events")
                .description("SRE 持久化队列状态事件")
                .tag("queue", allowed(queue, QUEUES, "unknown"))
                .tag("event", allowed(event, QUEUE_EVENTS, "unknown"))
                .register(meterRegistry)
                .increment(amount);
    }

    public void updateOperationalSnapshot(SreOperationalMetricsSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        openIncidents.set(snapshot.openIncidents());
        outboxBacklog.set(snapshot.outboxBacklog());
        outboxOldestAgeSeconds.set(snapshot.outboxOldestAgeSeconds());
        evaluationBacklog.set(snapshot.evaluationBacklog());
        evaluationOldestAgeSeconds.set(snapshot.evaluationOldestAgeSeconds());
        activeInvestigations.set(snapshot.activeInvestigations());
    }

    private void recordDurationAndCount(String timerName,
                                        String counterName,
                                        String timerDescription,
                                        String counterDescription,
                                        String outcome,
                                        long durationNanos) {
        Timer.builder(timerName)
                .description(timerDescription)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(Duration.ofNanos(Math.max(durationNanos, 0L)));
        Counter.builder(counterName)
                .description(counterDescription)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    private void gauge(String name,
                       String description,
                       AtomicLong value,
                       String baseUnit) {
        Gauge.builder(name, value, AtomicLong::doubleValue)
                .description(description)
                .baseUnit(baseUnit)
                .register(meterRegistry);
    }

    private String outcome(String value) {
        return allowed(value, OUTCOMES, "failed");
    }

    private String tool(String value) {
        String normalized = normalize(value);
        if (normalized.matches("(?:prom|loki)_[a-z0-9_]{1,48}")) {
            return normalized;
        }
        return "unknown";
    }

    private String allowed(String value, Set<String> allowed, String fallback) {
        String normalized = normalize(value);
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value)
                ? value.trim().toLowerCase(Locale.ROOT)
                : "unknown";
    }
}
