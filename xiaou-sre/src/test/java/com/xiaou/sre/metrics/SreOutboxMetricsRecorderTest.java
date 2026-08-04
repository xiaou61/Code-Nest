package com.xiaou.sre.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SreOutboxMetricsRecorderTest {

    @Test
    void recordsBoundedQueueAndProcessingMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SreOutboxMetricsRecorder recorder = new SreOutboxMetricsRecorder(registry);

        recorder.recordPending(7);
        recorder.recordLeaseRecovered(2);
        recorder.beginProcessing();
        recorder.recordEvent("EVIDENCE_COLLECTION_REQUESTED", "success", 1_000_000L);
        recorder.endProcessing();
        recorder.recordWorkerRun("success", 2_000_000L);

        assertThat(registry.get("xiaou.sre.outbox.pending").gauge().value()).isEqualTo(7D);
        assertThat(registry.get("xiaou.sre.outbox.processing").gauge().value()).isZero();
        assertThat(registry.get("xiaou.sre.outbox.lease.recoveries").counter().count()).isEqualTo(2D);
        assertThat(registry.get("xiaou.sre.outbox.events").counter().count()).isEqualTo(1D);
        assertThat(registry.get("xiaou.sre.outbox.events")
                .tag("event_type", "EVIDENCE_COLLECTION_REQUESTED")
                .tag("outcome", "success")
                .counter().count()).isEqualTo(1D);
        assertThat(registry.get("xiaou.sre.outbox.worker.runs")
                .tag("outcome", "success")
                .counter().count()).isEqualTo(1D);
    }

    @Test
    void normalizesUnboundedEventTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SreOutboxMetricsRecorder recorder = new SreOutboxMetricsRecorder(registry);

        recorder.recordEvent("event with user-id=7", "retry", 0L);

        assertThat(registry.get("xiaou.sre.outbox.events")
                .tag("event_type", "unknown")
                .tag("outcome", "retry")
                .counter().count()).isEqualTo(1D);
    }
}
