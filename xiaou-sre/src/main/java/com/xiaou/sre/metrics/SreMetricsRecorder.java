package com.xiaou.sre.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Bounded metrics for the synchronous SRE ingestion path and read-only
 * evidence collection. Payloads, incident IDs and user-controlled values are
 * deliberately excluded from labels.
 */
@Component
public class SreMetricsRecorder {

    private static final String PREFIX = "xiaou.sre";

    private final MeterRegistry meterRegistry;

    public SreMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordAlertIngestion(String status, String severity) {
        Counter.builder(PREFIX + ".alerts.ingested")
                .tag("status", normalize(status))
                .tag("severity", normalize(severity))
                .register(meterRegistry)
                .increment();
    }

    public void recordAlertIngestionError() {
        Counter.builder(PREFIX + ".alerts.ingestion.errors")
                .register(meterRegistry)
                .increment();
    }

    public void recordDuplicate() {
        Counter.builder(PREFIX + ".alerts.duplicates")
                .register(meterRegistry)
                .increment();
    }

    public void recordEvidenceCollection(String source, String outcome, long durationNanos) {
        String normalizedSource = normalize(source);
        String normalizedOutcome = normalize(outcome);
        Counter.builder(PREFIX + ".evidence.collections")
                .tag("source", normalizedSource)
                .tag("outcome", normalizedOutcome)
                .register(meterRegistry)
                .increment();
        Timer.builder(PREFIX + ".evidence.collection.duration")
                .tag("source", normalizedSource)
                .tag("outcome", normalizedOutcome)
                .register(meterRegistry)
                .record(Math.max(0L, durationNanos), TimeUnit.NANOSECONDS);
        if ("error".equals(normalizedOutcome)) {
            Counter.builder(PREFIX + ".evidence.collection.errors")
                    .tag("source", normalizedSource)
                    .register(meterRegistry)
                    .increment();
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank() || value.length() > 48) {
            return "unknown";
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '_' || current == '-' || current == '.')) {
                return "unknown";
            }
        }
        return value;
    }
}
