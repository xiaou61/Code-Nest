package com.xiaou.system.agent.task;

import com.xiaou.system.agent.AgentToolCall;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AgentTaskGraphRunnerTest {

    @Test
    void shouldRunExactlyOnePlanThenExecuteCycleInOrder() {
        AgentTaskCycleRuntime runtime = mock(AgentTaskCycleRuntime.class);
        AgentToolCall call = new AgentToolCall();
        call.setToolName("system.health.read");
        call.setInput(Map.of());
        AgentTaskPlanDecision decision = AgentTaskPlanDecision.execute(call, "health", false);
        AgentTaskCycleOutcome expected = AgentTaskCycleOutcome.continueRunning("step completed");
        when(runtime.planNext("agent-task-1", "worker-1")).thenReturn(decision);
        when(runtime.executeOrFinish("agent-task-1", "worker-1", decision)).thenReturn(expected);

        AgentTaskCycleOutcome actual = new AgentTaskGraphRunner(runtime)
                .runCycle("agent-task-1", "worker-1");

        assertEquals(expected, actual);
        InOrder inOrder = inOrder(runtime);
        inOrder.verify(runtime).planNext("agent-task-1", "worker-1");
        inOrder.verify(runtime).executeOrFinish("agent-task-1", "worker-1", decision);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldStopBeforePlanningWhenLeaseIsNoLongerActive() {
        AgentTaskCycleRuntime runtime = mock(AgentTaskCycleRuntime.class);
        AgentTaskCycleContext inactive = new AgentTaskCycleContext(
                "agent-task-1", "worker-1", false);
        when(runtime.loadContext("agent-task-1", "worker-1")).thenReturn(inactive);

        AgentTaskCycleOutcome actual = new AgentTaskGraphRunner(runtime)
                .runCycle("agent-task-1", "worker-1");

        assertEquals(AgentTaskCycleOutcome.Status.LOST_LEASE, actual.status());
        verify(runtime).loadContext("agent-task-1", "worker-1");
        verifyNoMoreInteractions(runtime);
    }

    @Test
    void shouldPersistWaitingInputWithoutEnteringPolicyOrToolExecution() {
        AgentTaskCycleRuntime runtime = mock(AgentTaskCycleRuntime.class);
        AgentTaskCycleContext context = new AgentTaskCycleContext(
                "agent-task-1", "worker-1", true);
        AgentTaskPlanDecision decision = AgentTaskPlanDecision.waitingInput("region is required");
        AgentTaskCycleOutcome expected = AgentTaskCycleOutcome.waitingInput("region is required");
        when(runtime.loadContext("agent-task-1", "worker-1")).thenReturn(context);
        when(runtime.planNext("agent-task-1", "worker-1")).thenReturn(decision);
        when(runtime.waitForInput(any(AgentTaskCycleContext.class),
                org.mockito.ArgumentMatchers.eq(decision))).thenReturn(expected);

        AgentTaskCycleOutcome actual = new AgentTaskGraphRunner(runtime)
                .runCycle("agent-task-1", "worker-1");

        assertEquals(expected, actual);
        verify(runtime).loadContext("agent-task-1", "worker-1");
        verify(runtime).planNext("agent-task-1", "worker-1");
        verify(runtime).waitForInput(any(AgentTaskCycleContext.class),
                org.mockito.ArgumentMatchers.eq(decision));
        verify(runtime, never()).policyGuard(any(), any());
        verify(runtime, never()).executeOrFinish(any(AgentTaskCycleContext.class), any());
        verify(runtime, never()).executeOrFinish(any(String.class), any(String.class), any());
        verifyNoMoreInteractions(runtime);
    }
}
