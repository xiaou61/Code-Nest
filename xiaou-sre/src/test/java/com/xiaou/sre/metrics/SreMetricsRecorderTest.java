package com.xiaou.sre.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SreMetricsRecorderTest {

    @Test
    void recordsIngestionDuplicatesAndEvidenceOutcomes() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SreMetricsRecorder recorder = new SreMetricsRecorder(registry);

        recorder.recordAlertIngestion("FIRING", "critical");
        recorder.recordDuplicate();
        recorder.recordAlertIngestionError();
        recorder.recordEvidenceCollection("outbox", "error", 1_000_000L);

        assertThat(registry.get("xiaou.sre.alerts.ingested")
                .tag("status", "FIRING")
                .tag("severity", "critical")
                .counter().count()).isEqualTo(1D);
        assertThat(registry.get("xiaou.sre.alerts.duplicates").counter().count()).isEqualTo(1D);
        assertThat(registry.get("xiaou.sre.alerts.ingestion.errors").counter().count()).isEqualTo(1D);
        assertThat(registry.get("xiaou.sre.evidence.collections")
                .tag("source", "outbox")
                .tag("outcome", "error")
                .counter().count()).isEqualTo(1D);
        assertThat(registry.get("xiaou.sre.evidence.collection.errors")
                .tag("source", "outbox")
                .counter().count()).isEqualTo(1D);
    }
}
