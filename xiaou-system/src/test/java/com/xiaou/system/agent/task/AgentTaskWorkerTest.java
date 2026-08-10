package com.xiaou.system.agent.task;

import com.xiaou.system.domain.SysAgentTask;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentTaskWorkerTest {

    @Test
    void shouldRemainInactiveUntilExplicitlyEnabled() {
        AgentTaskProperties properties = new AgentTaskProperties();
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskRecoveryCoordinator recoveryCoordinator = mock(AgentTaskRecoveryCoordinator.class);
        AgentTaskGraphRunner graphRunner = mock(AgentTaskGraphRunner.class);
        AgentTaskLeaseManager leaseManager = mock(AgentTaskLeaseManager.class);
        AgentTaskMetricsRecorder metricsRecorder = mock(AgentTaskMetricsRecorder.class);

        new AgentTaskWorker(properties, stateService, recoveryCoordinator,
                graphRunner, leaseManager, metricsRecorder).drain();

        verifyNoInteractions(stateService, recoveryCoordinator, graphRunner, leaseManager, metricsRecorder);
    }

    @Test
    void shouldRecoverClaimAndRunSequentialCyclesUntilCompletion() {
        AgentTaskProperties properties = enabledProperties();
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskRecoveryCoordinator recoveryCoordinator = mock(AgentTaskRecoveryCoordinator.class);
        AgentTaskGraphRunner graphRunner = mock(AgentTaskGraphRunner.class);
        AgentTaskLeaseManager leaseManager = inlineLeaseManager("lease-1");
        AgentTaskMetricsRecorder metricsRecorder = mock(AgentTaskMetricsRecorder.class);
        SysAgentTask task = task();
        AgentTaskRecoveryResult recovery = new AgentTaskRecoveryResult(2, 1, 0, 0);
        when(recoveryCoordinator.recoverStaleTasks()).thenReturn(recovery);
        when(stateService.listClaimableIds(2)).thenReturn(List.of("agent-task-1"));
        when(stateService.claim(eq("agent-task-1"), anyString())).thenReturn(task);
        when(graphRunner.runCycle(eq("agent-task-1"), anyString())).thenReturn(
                AgentTaskCycleOutcome.continueRunning("step 1 complete"),
                AgentTaskCycleOutcome.completed("done")
        );
        when(stateService.countQueuedTasks()).thenReturn(3L);

        new AgentTaskWorker(properties, stateService, recoveryCoordinator,
                graphRunner, leaseManager, metricsRecorder).drain();

        verify(graphRunner, times(2)).runCycle(eq("agent-task-1"), anyString());
        verify(metricsRecorder).recordStaleRecovery(recovery);
        verify(metricsRecorder).recordCycleOutcome(AgentTaskCycleOutcome.Status.CONTINUE);
        verify(metricsRecorder).recordCycleOutcome(AgentTaskCycleOutcome.Status.COMPLETED);
        verify(metricsRecorder).recordQueueDepth(3L);
        verify(metricsRecorder).recordWorkerRun(eq("success"), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldStopClaimedTaskAsSoonAsConfirmationIsRequired() {
        AgentTaskProperties properties = enabledProperties();
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskRecoveryCoordinator recoveryCoordinator = mock(AgentTaskRecoveryCoordinator.class);
        AgentTaskGraphRunner graphRunner = mock(AgentTaskGraphRunner.class);
        AgentTaskLeaseManager leaseManager = inlineLeaseManager("lease-1");
        AgentTaskMetricsRecorder metricsRecorder = mock(AgentTaskMetricsRecorder.class);
        when(recoveryCoordinator.recoverStaleTasks()).thenReturn(AgentTaskRecoveryResult.empty());
        when(stateService.listClaimableIds(2)).thenReturn(List.of("agent-task-1"));
        when(stateService.claim(eq("agent-task-1"), anyString())).thenReturn(task());
        when(graphRunner.runCycle(eq("agent-task-1"), anyString()))
                .thenReturn(AgentTaskCycleOutcome.waiting("confirm"));

        new AgentTaskWorker(properties, stateService, recoveryCoordinator,
                graphRunner, leaseManager, metricsRecorder).drain();

        verify(graphRunner, times(1)).runCycle(eq("agent-task-1"), anyString());
        verify(metricsRecorder).recordCycleOutcome(AgentTaskCycleOutcome.Status.WAITING_CONFIRMATION);
    }

    @Test
    void shouldStopClaimedTaskAsSoonAsPauseWinsTheStateRace() {
        AgentTaskProperties properties = enabledProperties();
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskRecoveryCoordinator recoveryCoordinator = mock(AgentTaskRecoveryCoordinator.class);
        AgentTaskGraphRunner graphRunner = mock(AgentTaskGraphRunner.class);
        AgentTaskLeaseManager leaseManager = inlineLeaseManager("lease-1");
        AgentTaskMetricsRecorder metricsRecorder = mock(AgentTaskMetricsRecorder.class);
        when(recoveryCoordinator.recoverStaleTasks()).thenReturn(AgentTaskRecoveryResult.empty());
        when(stateService.listClaimableIds(2)).thenReturn(List.of("agent-task-1"));
        when(stateService.claim(eq("agent-task-1"), anyString())).thenReturn(task());
        when(graphRunner.runCycle(eq("agent-task-1"), anyString()))
                .thenReturn(AgentTaskCycleOutcome.paused("operator paused task"));

        new AgentTaskWorker(properties, stateService, recoveryCoordinator,
                graphRunner, leaseManager, metricsRecorder).drain();

        verify(graphRunner, times(1)).runCycle(eq("agent-task-1"), anyString());
        verify(metricsRecorder).recordCycleOutcome(AgentTaskCycleOutcome.Status.PAUSED);
        verify(stateService, never()).failTask(
                eq("agent-task-1"), anyString(), eq("WORKER_CYCLE_LIMIT"), anyString());
    }

    @Test
    void shouldFailTaskWhenGraphExceedsBoundedCycleAllowance() {
        AgentTaskProperties properties = enabledProperties();
        properties.setMaxSteps(2);
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskRecoveryCoordinator recoveryCoordinator = mock(AgentTaskRecoveryCoordinator.class);
        AgentTaskGraphRunner graphRunner = mock(AgentTaskGraphRunner.class);
        AgentTaskLeaseManager leaseManager = inlineLeaseManager("lease-1");
        AgentTaskMetricsRecorder metricsRecorder = mock(AgentTaskMetricsRecorder.class);
        SysAgentTask task = task();
        task.setMaxSteps(2);
        when(recoveryCoordinator.recoverStaleTasks()).thenReturn(AgentTaskRecoveryResult.empty());
        when(stateService.listClaimableIds(2)).thenReturn(List.of("agent-task-1"));
        when(stateService.claim(eq("agent-task-1"), anyString())).thenReturn(task);
        when(graphRunner.runCycle(eq("agent-task-1"), anyString()))
                .thenReturn(AgentTaskCycleOutcome.continueRunning("unexpected loop"));

        new AgentTaskWorker(properties, stateService, recoveryCoordinator,
                graphRunner, leaseManager, metricsRecorder).drain();

        verify(graphRunner, times(3)).runCycle(eq("agent-task-1"), anyString());
        verify(stateService).failTask(
                eq("agent-task-1"),
                anyString(),
                eq("WORKER_CYCLE_LIMIT"),
                eq("任务 Worker 达到有界图周期上限，已停止继续执行。")
        );
    }

    @Test
    void shouldUseFreshLeaseTokenForEveryClaimInTheSameDrain() {
        AgentTaskProperties properties = enabledProperties();
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskRecoveryCoordinator recoveryCoordinator = mock(AgentTaskRecoveryCoordinator.class);
        AgentTaskGraphRunner graphRunner = mock(AgentTaskGraphRunner.class);
        AgentTaskLeaseManager leaseManager = inlineLeaseManager("lease-1", "lease-2");
        AgentTaskMetricsRecorder metricsRecorder = mock(AgentTaskMetricsRecorder.class);
        SysAgentTask first = task();
        SysAgentTask second = task();
        second.setTaskId("agent-task-2");
        when(recoveryCoordinator.recoverStaleTasks()).thenReturn(AgentTaskRecoveryResult.empty());
        when(stateService.listClaimableIds(2)).thenReturn(List.of("agent-task-1", "agent-task-2"));
        when(stateService.claim("agent-task-1", "lease-1")).thenReturn(first);
        when(stateService.claim("agent-task-2", "lease-2")).thenReturn(second);
        when(graphRunner.runCycle("agent-task-1", "lease-1"))
                .thenReturn(AgentTaskCycleOutcome.completed("done"));
        when(graphRunner.runCycle("agent-task-2", "lease-2"))
                .thenReturn(AgentTaskCycleOutcome.completed("done"));

        new AgentTaskWorker(properties, stateService, recoveryCoordinator,
                graphRunner, leaseManager, metricsRecorder).drain();

        verify(stateService).claim("agent-task-1", "lease-1");
        verify(stateService).claim("agent-task-2", "lease-2");
        verify(leaseManager, times(2)).newClaimToken(anyString());
    }

    private AgentTaskProperties enabledProperties() {
        AgentTaskProperties properties = new AgentTaskProperties();
        properties.setEnabled(true);
        properties.setBatchSize(2);
        properties.setMaxSteps(5);
        return properties;
    }

    private SysAgentTask task() {
        SysAgentTask task = new SysAgentTask();
        task.setTaskId("agent-task-1");
        task.setStatus(AgentTaskStatus.RUNNING.name());
        task.setMaxSteps(5);
        task.setCreatedTime(LocalDateTime.now().minusSeconds(5));
        return task;
    }

    private AgentTaskLeaseManager inlineLeaseManager(String... claimTokens) {
        AgentTaskLeaseManager leaseManager = mock(AgentTaskLeaseManager.class);
        when(leaseManager.newClaimToken(anyString())).thenReturn(
                claimTokens[0],
                java.util.Arrays.copyOfRange(claimTokens, 1, claimTokens.length)
        );
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get())
                .when(leaseManager).callWithHeartbeat(anyString(), anyString(), any());
        return leaseManager;
    }
}
