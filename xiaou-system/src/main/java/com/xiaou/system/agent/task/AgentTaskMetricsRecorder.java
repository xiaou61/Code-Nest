package com.xiaou.system.agent.task;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded-cardinality metrics for the durable administrator-agent task runtime.
 */
@Component
public class AgentTaskMetricsRecorder {

    private static final String PREFIX = "xiaou.agent.task";

    private final MeterRegistry meterRegistry;
    private final AtomicLong queueDepth = new AtomicLong();
    private final Counter confirmationWaits;
    private final Counter cancellations;
    private final Counter reviewRequired;

    public AgentTaskMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder(PREFIX + ".queue.depth", queueDepth, AtomicLong::get)
                .description("等待执行的持久化管理员智能体任务数")
                .register(meterRegistry);
        confirmationWaits = Counter.builder(PREFIX + ".confirmation.waits")
                .description("持久化管理员智能体任务进入写确认等待的次数")
                .register(meterRegistry);
        cancellations = Counter.builder(PREFIX + ".cancellations")
                .description("持久化管理员智能体任务取消次数")
                .register(meterRegistry);
        reviewRequired = Counter.builder(PREFIX + ".review.required")
                .description("持久化管理员智能体任务需要人工复核的次数")
                .register(meterRegistry);
    }

    public void recordQueueDepth(long value) {
        queueDepth.set(Math.max(0L, value));
    }

    public void recordTaskOutcome(AgentTaskStatus status, long durationNanos) {
        String outcome = tag(status);
        Counter.builder(PREFIX + ".outcomes")
                .description("持久化管理员智能体任务终态次数")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
        Timer.builder(PREFIX + ".duration")
                .description("持久化管理员智能体任务端到端耗时")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(Math.max(0L, durationNanos), TimeUnit.NANOSECONDS);
    }

    public void recordStepOutcome(AgentTaskStepStatus status) {
        Counter.builder(PREFIX + ".step.outcomes")
                .description("持久化管理员智能体任务步骤状态次数")
                .tag("outcome", tag(status))
                .register(meterRegistry)
                .increment();
    }

    public void recordConfirmationWait() {
        confirmationWaits.increment();
    }

    public void recordCancellation() {
        cancellations.increment();
    }

    public void recordStaleRecovery(AgentTaskRecoveryResult result) {
        if (result == null) {
            return;
        }
        incrementRecovery("recovered", result.recovered());
        incrementRecovery("requires_review", result.requiresReview());
        incrementRecovery("conflict", result.conflicts());
        incrementRecovery("error", result.errors());
    }

    public void recordLeaseRenewal(String outcome) {
        String normalized = switch (outcome == null ? "" : outcome.trim().toLowerCase(Locale.ROOT)) {
            case "success" -> "success";
            case "lost" -> "lost";
            default -> "error";
        };
        Counter.builder(PREFIX + ".lease.renewals")
                .description("持久化管理员智能体任务租约续期结果")
                .tag("outcome", normalized)
                .register(meterRegistry)
                .increment();
    }

    public void recordReviewRequired() {
        reviewRequired.increment();
    }

    public void recordCycleOutcome(AgentTaskCycleOutcome.Status status) {
        Counter.builder(PREFIX + ".cycles")
                .description("持久化管理员智能体任务图周期结果")
                .tag("outcome", tag(status))
                .register(meterRegistry)
                .increment();
    }

    public void recordWorkerRun(String outcome, long durationNanos) {
        String normalized = safeTag(outcome);
        Counter.builder(PREFIX + ".worker.runs")
                .description("持久化管理员智能体任务 Worker 扫描次数")
                .tag("outcome", normalized)
                .register(meterRegistry)
                .increment();
        Timer.builder(PREFIX + ".worker.duration")
                .description("持久化管理员智能体任务 Worker 扫描耗时")
                .tag("outcome", normalized)
                .register(meterRegistry)
                .record(Math.max(0L, durationNanos), TimeUnit.NANOSECONDS);
    }

    private void incrementRecovery(String outcome, int amount) {
        if (amount <= 0) {
            return;
        }
        Counter.builder(PREFIX + ".stale.recoveries")
                .description("持久化管理员智能体任务租约恢复结果")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment(amount);
    }

    private String tag(Enum<?> value) {
        return value == null ? "unknown" : value.name().toLowerCase(Locale.ROOT);
    }

    private String safeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("[a-z0-9_.:-]{1,64}") ? normalized : "unknown";
    }
}
