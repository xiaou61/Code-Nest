package com.xiaou.sre.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Outbox worker runtime metrics.
 *
 * <p>Only bounded state and bounded outcome/event tags are exported. The
 * recorder deliberately does not expose payloads, incident ids, or arbitrary
 * database values as metric labels.</p>
 */
@Component
public class SreOutboxMetricsRecorder {

    private static final String METRIC_PREFIX = "xiaou.sre.outbox";
    private static final String UNKNOWN = "unknown";

    private final MeterRegistry meterRegistry;
    private final AtomicLong pending = new AtomicLong();
    private final AtomicLong processing = new AtomicLong();
    private final Counter leaseRecoveries;

    public SreOutboxMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        meterRegistry.gauge(METRIC_PREFIX + ".pending", pending);
        meterRegistry.gauge(METRIC_PREFIX + ".processing", processing);
        leaseRecoveries = Counter.builder(METRIC_PREFIX + ".lease.recoveries")
                .register(meterRegistry);
    }

    /** Create an isolated recorder for compatibility constructors and tests. */
    public static SreOutboxMetricsRecorder noop() {
        return new SreOutboxMetricsRecorder(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    public void recordPending(long value) {
        pending.set(Math.max(0L, value));
    }

    public void recordLeaseRecovered(long count) {
        if (count > 0) {
            leaseRecoveries.increment(count);
        }
    }

    public void beginProcessing() {
        processing.incrementAndGet();
    }

    public void endProcessing() {
        processing.updateAndGet(value -> Math.max(0L, value - 1L));
    }

    public void recordWorkerRun(String outcome, long durationNanos) {
        String normalizedOutcome = normalizeTag(outcome);
        Counter.builder(METRIC_PREFIX + ".worker.runs")
                .tag("outcome", normalizedOutcome)
                .register(meterRegistry)
                .increment();
        Timer.builder(METRIC_PREFIX + ".worker.duration")
                .tag("outcome", normalizedOutcome)
                .register(meterRegistry)
                .record(Math.max(0L, durationNanos), java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    public void recordEvent(String eventType, String outcome, long durationNanos) {
        String normalizedEventType = normalizeTag(eventType);
        String normalizedOutcome = normalizeTag(outcome);
        Counter.builder(METRIC_PREFIX + ".events")
                .tag("event_type", normalizedEventType)
                .tag("outcome", normalizedOutcome)
                .register(meterRegistry)
                .increment();
        Timer.builder(METRIC_PREFIX + ".event.duration")
                .tag("event_type", normalizedEventType)
                .tag("outcome", normalizedOutcome)
                .register(meterRegistry)
                .record(Math.max(0L, durationNanos), java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private String normalizeTag(String value) {
        if (value == null || value.isBlank() || value.length() > 64) {
            return UNKNOWN;
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '_' || current == '-' || current == '.' || current == ':')) {
                return UNKNOWN;
            }
        }
        return value;
    }
}
