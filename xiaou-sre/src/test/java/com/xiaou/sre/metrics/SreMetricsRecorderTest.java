package com.xiaou.sre.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SreMetricsRecorderTest {

    @Test
    void recordsStatusDuplicateErrorAndSourceEvidenceMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SreMetricsRecorder recorder = new SreMetricsRecorder(registry);

        recorder.recordAlertIngestion("FIRING", "critical");
        recorder.recordDuplicate();
        recorder.recordAlertIngestionError();
        recorder.recordEvidenceCollection("outbox", "error", 1_000_000L);

        assertEquals(1D, registry.get("xiaou.sre.alerts.ingested")
                .tag("status", "FIRING")
                .tag("severity", "critical")
                .counter().count());
        assertEquals(1D, registry.get("xiaou.sre.alerts.duplicates").counter().count());
        assertEquals(1D, registry.get("xiaou.sre.alerts.ingestion.errors").counter().count());
        assertEquals(1D, registry.get("xiaou.sre.evidence.collections")
                .tag("source", "outbox")
                .tag("outcome", "error")
                .counter().count());
    }

    @Test
    void shouldRegisterZeroTrafficMetricFamiliesAtStartup() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new SreMetricsRecorder(registry);

        Timer ingestion = registry.find("xiaou.sre.alert.ingestion.duration")
                .tag("outcome", "succeeded")
                .timer();
        Timer evidence = registry.find("xiaou.sre.evidence.collection.duration")
                .tag("outcome", "failed")
                .timer();
        Timer evaluation = registry.find("xiaou.sre.evaluation.duration")
                .tag("outcome", "degraded")
                .timer();
        Timer investigation = registry.find("xiaou.sre.investigation.duration")
                .tag("outcome", "skipped")
                .tag("generation_mode", "fallback")
                .timer();
        DistributionSummary rounds = registry.find("xiaou.sre.investigation.rounds")
                .tag("outcome", "unavailable")
                .summary();
        Counter toolCalls = registry.find("xiaou.sre.investigation.tool.calls")
                .tag("tool", "unknown")
                .tag("outcome", "succeeded")
                .counter();
        Counter recoveries = registry.find("xiaou.sre.queue.events")
                .tag("queue", "evaluation")
                .tag("event", "lease_recovered")
                .counter();

        assertNotNull(ingestion);
        assertNotNull(evidence);
        assertNotNull(evaluation);
        assertNotNull(investigation);
        assertNotNull(rounds);
        assertNotNull(toolCalls);
        assertNotNull(recoveries);
        assertEquals(0, ingestion.count());
        assertEquals(0, evidence.count());
        assertEquals(0, evaluation.count());
        assertEquals(0, investigation.count());
        assertEquals(0, rounds.count());
        assertEquals(0D, toolCalls.count());
        assertEquals(0D, recoveries.count());
    }

    @Test
    void shouldRecordBoundedInvestigationAndQueueMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SreMetricsRecorder recorder = new SreMetricsRecorder(registry);

        recorder.recordInvestigation("SUCCEEDED", "AI", 3, 25_000_000L);
        recorder.recordReadOnlyTool("PROM_HTTP_5XX", "SUCCEEDED", 7_000_000L);
        recorder.incrementQueueEvent("evaluation", "lease_recovered", 2);

        Timer investigationDuration = registry.find("xiaou.sre.investigation.duration")
                .tag("outcome", "succeeded")
                .tag("generation_mode", "ai")
                .timer();
        DistributionSummary rounds = registry.find("xiaou.sre.investigation.rounds")
                .tag("outcome", "succeeded")
                .summary();
        Counter toolCalls = registry.find("xiaou.sre.investigation.tool.calls")
                .tag("tool", "prom_http_5xx")
                .tag("outcome", "succeeded")
                .counter();
        Counter recoveries = registry.find("xiaou.sre.queue.events")
                .tag("queue", "evaluation")
                .tag("event", "lease_recovered")
                .counter();

        assertNotNull(investigationDuration);
        assertNotNull(rounds);
        assertNotNull(toolCalls);
        assertNotNull(recoveries);
        assertEquals(1, investigationDuration.count());
        assertTrue(investigationDuration.totalTime(TimeUnit.MILLISECONDS) >= 25);
        assertEquals(3D, rounds.totalAmount());
        assertEquals(1D, toolCalls.count());
        assertEquals(2D, recoveries.count());
    }

    @Test
    void shouldPublishOperationalSnapshotAsGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SreMetricsRecorder recorder = new SreMetricsRecorder(registry);

        recorder.updateOperationalSnapshot(new SreOperationalMetricsSnapshot(
                4, 7, 81, 3, 42, 1));

        assertEquals(4D, registry.get("xiaou.sre.incidents.open").gauge().value());
        assertEquals(7D, registry.get("xiaou.sre.outbox.backlog").gauge().value());
        assertEquals(81D, registry.get("xiaou.sre.outbox.oldest.age").gauge().value());
        assertEquals(3D, registry.get("xiaou.sre.evaluation.backlog").gauge().value());
        assertEquals(42D, registry.get("xiaou.sre.evaluation.oldest.age").gauge().value());
        assertEquals(1D, registry.get("xiaou.sre.investigation.active").gauge().value());
    }
}
