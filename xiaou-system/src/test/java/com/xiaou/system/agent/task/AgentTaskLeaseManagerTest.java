package com.xiaou.system.agent.task;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AgentTaskLeaseManagerTest {

    @Test
    void shouldCreateFreshBoundedTokenForEveryClaim() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try (AgentTaskLeaseManager manager = manager(scheduler, Duration.ofSeconds(1))) {
            String longWorkerId = "agent-task-worker-" + "x".repeat(200);

            String first = manager.newClaimToken(longWorkerId);
            String second = manager.newClaimToken(longWorkerId);

            assertNotEquals(first, second);
            assertTrue(first.startsWith("agent-task-worker-"));
            assertTrue(first.length() <= 120);
            assertTrue(second.length() <= 120);
        }
    }

    @Test
    void shouldRenewLeaseWhileGuardedWorkIsStillRunning() throws Exception {
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskMetricsRecorder metricsRecorder = mock(AgentTaskMetricsRecorder.class);
        CountDownLatch renewed = new CountDownLatch(1);
        when(stateService.renewLease("agent-task-1", "lease-1")).thenAnswer(invocation -> {
            renewed.countDown();
            return true;
        });
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        try (AgentTaskLeaseManager manager = new AgentTaskLeaseManager(
                stateService,
                metricsRecorder,
                scheduler,
                Duration.ofMillis(5)
        )) {
            String result = manager.callWithHeartbeat("agent-task-1", "lease-1", () -> {
                try {
                    assertTrue(renewed.await(1, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                return "done";
            });

            assertEquals("done", result);
            verify(stateService).renewLease("agent-task-1", "lease-1");
            verify(metricsRecorder, timeout(1_000)).recordLeaseRenewal("success");
            verifyNoMoreInteractions(stateService);
        }
    }

    @Test
    void shouldRejectGuardedWorkWithoutCompleteLeaseIdentity() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try (AgentTaskLeaseManager manager = manager(scheduler, Duration.ofSeconds(1))) {
            assertThrows(IllegalArgumentException.class,
                    () -> manager.callWithHeartbeat("", "lease-1", () -> "done"));
            assertThrows(IllegalArgumentException.class,
                    () -> manager.callWithHeartbeat("agent-task-1", " ", () -> "done"));
        }
    }

    private AgentTaskLeaseManager manager(
            ScheduledExecutorService scheduler,
            Duration renewalInterval
    ) {
        return new AgentTaskLeaseManager(
                mock(AgentTaskStateService.class),
                mock(AgentTaskMetricsRecorder.class),
                scheduler,
                renewalInterval
        );
    }
}
