package com.xiaou.system.agent.task;

import com.xiaou.system.agent.AgentChatErrorCode;
import com.xiaou.system.agent.AgentChatOrchestrator;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentOperatorResolver;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolRegistry;
import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskStep;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.dto.AgentTaskResponse;
import com.xiaou.system.service.SysAgentAuditService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentTaskCycleRuntimeTest {

    @Test
    void shouldStopBeforePlanningWhenStepLimitIsExhausted() {
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskPlanner planner = mock(AgentTaskPlanner.class);
        SysAgentTask task = runningTask();
        task.setCompletedSteps(5);
        task.setMaxSteps(5);
        when(stateService.loadExecutionSnapshot("agent-task-1", "worker-1"))
                .thenReturn(new AgentTaskSnapshot(task, List.of()));

        AgentTaskPlanDecision decision = runtime(stateService, planner, List.of())
                .planNext("agent-task-1", "worker-1");

        assertTrue(decision.blocked());
        assertEquals("STEP_LIMIT_EXHAUSTED", decision.code());
        verifyNoInteractions(planner);
    }

    @Test
    void shouldRejectDuplicateFingerprintWithoutExecutingTool() {
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskPlanner planner = mock(AgentTaskPlanner.class);
        AgentTool tool = tool();
        AgentToolCall call = new AgentToolCall();
        call.setToolName("system.health.read");
        call.setInput(Map.of("scope", "runtime"));
        when(stateService.hasFingerprint("agent-task-1", call)).thenReturn(true);
        when(stateService.failTask(eq("agent-task-1"), eq("worker-1"),
                eq("DUPLICATE_TASK_STEP"), any())).thenReturn(true);

        AgentTaskCycleRuntime runtime = runtime(stateService, planner, List.of(tool));
        AgentTaskCycleOutcome outcome = runtime.executeOrFinish(
                "agent-task-1",
                "worker-1",
                AgentTaskPlanDecision.execute(call, "health", false)
        );

        assertEquals(AgentTaskCycleOutcome.Status.FAILED, outcome.status());
        verify(stateService).failTask(eq("agent-task-1"), eq("worker-1"),
                eq("DUPLICATE_TASK_STEP"), any());
        verify(stateService, never()).startStep(any(), any(), any(), any(), any(Boolean.class));
    }

    @Test
    void shouldExecuteReadonlyStepThroughSharedOrchestratorAndContinue() {
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskPlanner planner = mock(AgentTaskPlanner.class);
        AgentTool tool = tool();
        AgentToolCall call = new AgentToolCall();
        call.setToolName("system.health.read");
        call.setInput(Map.of("scope", "runtime"));
        SysAgentTask task = runningTask();
        SysAgentTaskStep step = new SysAgentTaskStep();
        step.setTaskId(task.getTaskId());
        step.setStepOrder(1);
        step.setToolName("system.health.read");
        step.setStatus(AgentTaskStepStatus.RUNNING.name());
        when(stateService.load("agent-task-1")).thenReturn(task);
        when(stateService.hasFingerprint("agent-task-1", call)).thenReturn(false);
        when(stateService.startStep("agent-task-1", "worker-1", call, tool.definition(), false))
                .thenReturn(new AgentTaskStepExecution(step, call));
        when(stateService.completeStep(eq("agent-task-1"), eq("worker-1"), eq(1),
                eq(AgentTaskStepStatus.RUNNING), any(), eq(AgentTaskStatus.RUNNING))).thenReturn(true);

        AgentChatResponse response = new AgentChatResponse();
        response.setStatus("answered");
        response.setAnswer("healthy");
        AgentChatOrchestrator orchestrator = mock(AgentChatOrchestrator.class);
        when(orchestrator.executeRegisteredCall(any(), any(), eq(call))).thenReturn(response);
        AgentOperatorResolver operatorResolver = mock(AgentOperatorResolver.class);
        when(operatorResolver.resolve(7L)).thenReturn(new AgentOperator(7L, "admin"));

        AgentTaskCycleRuntime runtime = new AgentTaskCycleRuntime(
                stateService,
                planner,
                new AgentToolRegistry(List.of(tool)),
                orchestrator,
                operatorResolver,
                mock(SysAgentAuditService.class),
                inlineLeaseManager()
        );
        AgentTaskCycleOutcome outcome = runtime.executeOrFinish(
                "agent-task-1",
                "worker-1",
                AgentTaskPlanDecision.execute(call, "health", false)
        );

        assertEquals(AgentTaskCycleOutcome.Status.CONTINUE, outcome.status());
        verify(orchestrator).executeRegisteredCall(any(), any(), eq(call));
        verify(stateService).completeStep(eq("agent-task-1"), eq("worker-1"), eq(1),
                eq(AgentTaskStepStatus.RUNNING), eq(response), eq(AgentTaskStatus.RUNNING));
    }

    @Test
    void shouldRestoreConfirmationWaitWhenStrongConfirmationDoesNotMatch() {
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentChatOrchestrator orchestrator = mock(AgentChatOrchestrator.class);
        AgentOperatorResolver operatorResolver = mock(AgentOperatorResolver.class);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentTaskConfirmationClaim claim = confirmationClaim();
        AgentAuditResponse preview = audit("PREVIEW");
        AgentChatResponse rejected = new AgentChatResponse();
        rejected.setStatus("rejected");
        rejected.setErrorCode(AgentChatErrorCode.CONFIRMATION_MISMATCH.code().toLowerCase());
        rejected.setErrorMessage("confirmation mismatch");
        AgentTaskResponse detail = new AgentTaskResponse();
        detail.setTaskId("agent-task-1");
        detail.setStatus(AgentTaskStatus.WAITING_CONFIRMATION.name());
        when(stateService.beginConfirmation("agent-task-1", 7L)).thenReturn(claim);
        when(auditService.getByAuditId("audit-1")).thenReturn(preview);
        when(operatorResolver.resolve(7L)).thenReturn(new AgentOperator(7L, "admin"));
        when(orchestrator.chat(any(), any())).thenReturn(rejected);
        when(stateService.detail("agent-task-1", 7L)).thenReturn(detail);

        AgentTaskResponse response = new AgentTaskCycleRuntime(
                stateService,
                mock(AgentTaskPlanner.class),
                new AgentToolRegistry(List.of()),
                orchestrator,
                operatorResolver,
                auditService,
                inlineLeaseManager()
        ).confirm("agent-task-1", new AgentOperator(7L, "admin"), "wrong text");

        assertEquals(AgentTaskStatus.WAITING_CONFIRMATION.name(), response.getStatus());
        verify(stateService).restoreConfirmationWait(claim);
        verify(stateService, never()).completeStep(any(), any(), anyInt(), any(), any(), any());
        verify(stateService, never()).markRequiresReview(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldCompleteConfirmedStepAndQueueTaskForNextCycle() {
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentChatOrchestrator orchestrator = mock(AgentChatOrchestrator.class);
        AgentOperatorResolver operatorResolver = mock(AgentOperatorResolver.class);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentTaskConfirmationClaim claim = confirmationClaim();
        AgentChatResponse executed = new AgentChatResponse();
        executed.setStatus("executed");
        executed.setAnswer("write completed");
        AgentTaskResponse detail = new AgentTaskResponse();
        detail.setTaskId("agent-task-1");
        detail.setStatus(AgentTaskStatus.QUEUED.name());
        when(stateService.beginConfirmation("agent-task-1", 7L)).thenReturn(claim);
        when(auditService.getByAuditId("audit-1")).thenReturn(audit("PREVIEW"));
        when(operatorResolver.resolve(7L)).thenReturn(new AgentOperator(7L, "admin"));
        when(orchestrator.chat(any(), any())).thenReturn(executed);
        when(stateService.detail("agent-task-1", 7L)).thenReturn(detail);
        AgentTaskLeaseManager leaseManager = inlineLeaseManager();

        AgentTaskResponse response = new AgentTaskCycleRuntime(
                stateService,
                mock(AgentTaskPlanner.class),
                new AgentToolRegistry(List.of()),
                orchestrator,
                operatorResolver,
                auditService,
                leaseManager
        ).confirm("agent-task-1", new AgentOperator(7L, "admin"), "CONFIRM-WRITE");

        assertEquals(AgentTaskStatus.QUEUED.name(), response.getStatus());
        verify(stateService).completeStep(
                "agent-task-1",
                claim.leaseOwner(),
                1,
                AgentTaskStepStatus.CONFIRMING,
                executed,
                AgentTaskStatus.QUEUED
        );
        verify(stateService, never()).restoreConfirmationWait(claim);
        verify(leaseManager).callWithHeartbeat(eq("agent-task-1"),
                eq(claim.leaseOwner()), any());
    }

    @Test
    void shouldRequireReviewWithoutExecutingWhenAuditWasAlreadyConfirmed() {
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentChatOrchestrator orchestrator = mock(AgentChatOrchestrator.class);
        AgentOperatorResolver operatorResolver = mock(AgentOperatorResolver.class);
        SysAgentAuditService auditService = mock(SysAgentAuditService.class);
        AgentTaskConfirmationClaim claim = confirmationClaim();
        AgentTaskResponse detail = new AgentTaskResponse();
        detail.setTaskId("agent-task-1");
        detail.setStatus(AgentTaskStatus.REQUIRES_REVIEW.name());
        when(stateService.beginConfirmation("agent-task-1", 7L)).thenReturn(claim);
        when(auditService.getByAuditId("audit-1")).thenReturn(audit("CONFIRMED"));
        when(stateService.detail("agent-task-1", 7L)).thenReturn(detail);

        AgentTaskResponse response = new AgentTaskCycleRuntime(
                stateService,
                mock(AgentTaskPlanner.class),
                new AgentToolRegistry(List.of()),
                orchestrator,
                operatorResolver,
                auditService,
                inlineLeaseManager()
        ).confirm("agent-task-1", new AgentOperator(7L, "admin"), "CONFIRM-WRITE");

        assertEquals(AgentTaskStatus.REQUIRES_REVIEW.name(), response.getStatus());
        verify(stateService).markRequiresReview(
                "agent-task-1",
                claim.leaseOwner(),
                1,
                AgentTaskStepStatus.CONFIRMING,
                "AMBIGUOUS_CONFIRMED_WRITE",
                "写步骤已经确认但没有持久化终态，禁止自动重放。"
        );
        verifyNoInteractions(orchestrator, operatorResolver);
    }

    private AgentTaskCycleRuntime runtime(
            AgentTaskStateService stateService,
            AgentTaskPlanner planner,
            List<AgentTool> tools
    ) {
        return new AgentTaskCycleRuntime(
                stateService,
                planner,
                new AgentToolRegistry(tools),
                mock(AgentChatOrchestrator.class),
                mock(AgentOperatorResolver.class),
                mock(SysAgentAuditService.class),
                inlineLeaseManager()
        );
    }

    private SysAgentTask runningTask() {
        SysAgentTask task = new SysAgentTask();
        task.setTaskId("agent-task-1");
        task.setGoal("inspect runtime");
        task.setSessionId("session-1");
        task.setStatus(AgentTaskStatus.RUNNING.name());
        task.setOperatorId(7L);
        task.setOperatorName("admin");
        task.setMaxSteps(5);
        task.setCompletedSteps(0);
        task.setCurrentStepOrder(0);
        task.setLeaseOwner("worker-1");
        return task;
    }

    private AgentTool tool() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.health.read");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of("scope", Map.of("type", "string")));
        AgentTool tool = mock(AgentTool.class);
        when(tool.definition()).thenReturn(definition);
        return tool;
    }

    private AgentTaskConfirmationClaim confirmationClaim() {
        SysAgentTask task = runningTask();
        task.setPendingAuditId("audit-1");
        SysAgentTaskStep step = new SysAgentTaskStep();
        step.setTaskId("agent-task-1");
        step.setStepOrder(1);
        step.setAuditId("audit-1");
        step.setStatus(AgentTaskStepStatus.CONFIRMING.name());
        return new AgentTaskConfirmationClaim(task, step, "confirmation-worker-1");
    }

    private AgentAuditResponse audit(String status) {
        AgentAuditResponse audit = new AgentAuditResponse();
        audit.setAuditId("audit-1");
        audit.setStatus(status);
        return audit;
    }

    private AgentTaskLeaseManager inlineLeaseManager() {
        AgentTaskLeaseManager leaseManager = mock(AgentTaskLeaseManager.class);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get())
                .when(leaseManager).callWithHeartbeat(any(), any(), any());
        return leaseManager;
    }
}
