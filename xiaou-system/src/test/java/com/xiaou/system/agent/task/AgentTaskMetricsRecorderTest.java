package com.xiaou.system.agent.task;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentTaskMetricsRecorderTest {

    @Test
    void shouldExposeBoundedTaskLifecycleMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AgentTaskMetricsRecorder recorder = new AgentTaskMetricsRecorder(registry);

        recorder.recordQueueDepth(4);
        recorder.recordTaskOutcome(AgentTaskStatus.COMPLETED, Duration.ofSeconds(3).toNanos());
        recorder.recordStepOutcome(AgentTaskStepStatus.COMPLETED);
        recorder.recordConfirmationWait();
        recorder.recordCancellation();
        recorder.recordStaleRecovery(new AgentTaskRecoveryResult(2, 1, 3, 1));
        recorder.recordLeaseRenewal("success");
        recorder.recordLeaseRenewal("lost");
        recorder.recordLeaseRenewal("unexpected-value");
        recorder.recordReviewRequired();
        recorder.recordCycleOutcome(AgentTaskCycleOutcome.Status.COMPLETED);
        recorder.recordWorkerRun("success", Duration.ofMillis(25).toNanos());

        assertEquals(4D, registry.get("xiaou.agent.task.queue.depth").gauge().value());
        assertEquals(1D, registry.get("xiaou.agent.task.outcomes")
                .tag("outcome", "completed").counter().count());
        assertEquals(1L, registry.get("xiaou.agent.task.duration")
                .tag("outcome", "completed").timer().count());
        assertEquals(1D, registry.get("xiaou.agent.task.step.outcomes")
                .tag("outcome", "completed").counter().count());
        assertEquals(1D, registry.get("xiaou.agent.task.confirmation.waits").counter().count());
        assertEquals(1D, registry.get("xiaou.agent.task.cancellations").counter().count());
        assertEquals(2D, registry.get("xiaou.agent.task.stale.recoveries")
                .tag("outcome", "recovered").counter().count());
        assertEquals(1D, registry.get("xiaou.agent.task.stale.recoveries")
                .tag("outcome", "requires_review").counter().count());
        assertEquals(3D, registry.get("xiaou.agent.task.stale.recoveries")
                .tag("outcome", "conflict").counter().count());
        assertEquals(1D, registry.get("xiaou.agent.task.stale.recoveries")
                .tag("outcome", "error").counter().count());
        assertEquals(1D, registry.get("xiaou.agent.task.lease.renewals")
                .tag("outcome", "success").counter().count());
        assertEquals(1D, registry.get("xiaou.agent.task.lease.renewals")
                .tag("outcome", "lost").counter().count());
        assertEquals(1D, registry.get("xiaou.agent.task.lease.renewals")
                .tag("outcome", "error").counter().count());
        assertEquals(1D, registry.get("xiaou.agent.task.review.required").counter().count());
        assertEquals(1D, registry.get("xiaou.agent.task.cycles")
                .tag("outcome", "completed").counter().count());
        assertNotNull(registry.get("xiaou.agent.task.worker.duration")
                .tag("outcome", "success").timer());
    }
}
