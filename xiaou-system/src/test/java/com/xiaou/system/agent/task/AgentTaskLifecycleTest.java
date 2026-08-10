package com.xiaou.system.agent.task;

import com.xiaou.system.agent.AgentChatOrchestrator;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentOperatorResolver;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolRegistry;
import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskStep;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.service.SysAgentAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentTaskLifecycleTest {

    @Test
    void shouldExecuteTwoReadonlyStepsInOrderThenComplete() {
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskPlanner planner = mock(AgentTaskPlanner.class);
        AgentChatOrchestrator orchestrator = mock(AgentChatOrchestrator.class);
        AgentOperatorResolver operatorResolver = mock(AgentOperatorResolver.class);
        AgentTool firstTool = tool("system.health.read");
        AgentTool secondTool = tool("system.release.read");
        AgentToolCall firstCall = call("system.health.read", "runtime");
        AgentToolCall secondCall = call("system.release.read", "current");
        SysAgentTask task0 = task(0);
        SysAgentTask task1 = task(1);
        SysAgentTask task2 = task(2);
        SysAgentTaskStep completed1 = completedStep(1, firstCall.getToolName());
        SysAgentTaskStep completed2 = completedStep(2, secondCall.getToolName());
        SysAgentTaskStep running1 = runningStep(1, firstCall.getToolName());
        SysAgentTaskStep running2 = runningStep(2, secondCall.getToolName());

        when(stateService.loadExecutionSnapshot("agent-task-1", "worker-1")).thenReturn(
                new AgentTaskSnapshot(task0, List.of()),
                new AgentTaskSnapshot(task1, List.of(completed1)),
                new AgentTaskSnapshot(task2, List.of(completed1, completed2))
        );
        when(planner.plan(any())).thenReturn(
                AgentTaskPlanDecision.execute(firstCall, "read health", false),
                AgentTaskPlanDecision.execute(secondCall, "read release", false),
                AgentTaskPlanDecision.complete("goal complete", false)
        );
        when(stateService.hasFingerprint(any(), any())).thenReturn(false);
        when(stateService.load("agent-task-1")).thenReturn(task0, task1);
        when(stateService.startStep("agent-task-1", "worker-1", firstCall,
                firstTool.definition(), false)).thenReturn(new AgentTaskStepExecution(running1, firstCall));
        when(stateService.startStep("agent-task-1", "worker-1", secondCall,
                secondTool.definition(), false)).thenReturn(new AgentTaskStepExecution(running2, secondCall));
        when(operatorResolver.resolve(7L)).thenReturn(new AgentOperator(7L, "admin"));
        AgentChatResponse firstResponse = answered("healthy");
        AgentChatResponse secondResponse = answered("release ready");
        when(orchestrator.executeRegisteredCall(any(), any(), eq(firstCall))).thenReturn(firstResponse);
        when(orchestrator.executeRegisteredCall(any(), any(), eq(secondCall))).thenReturn(secondResponse);
        when(stateService.completeStep(eq("agent-task-1"), eq("worker-1"), eq(1),
                eq(AgentTaskStepStatus.RUNNING), eq(firstResponse), eq(AgentTaskStatus.RUNNING))).thenReturn(true);
        when(stateService.completeStep(eq("agent-task-1"), eq("worker-1"), eq(2),
                eq(AgentTaskStepStatus.RUNNING), eq(secondResponse), eq(AgentTaskStatus.RUNNING))).thenReturn(true);
        when(stateService.completeTask("agent-task-1", "worker-1", "goal complete")).thenReturn(true);

        AgentTaskCycleRuntime runtime = new AgentTaskCycleRuntime(
                stateService,
                planner,
                new AgentToolRegistry(List.of(firstTool, secondTool)),
                orchestrator,
                operatorResolver,
                mock(SysAgentAuditService.class),
                mock(AgentTaskLeaseManager.class)
        );
        AgentTaskGraphRunner graphRunner = new AgentTaskGraphRunner(runtime);

        assertEquals(AgentTaskCycleOutcome.Status.CONTINUE,
                graphRunner.runCycle("agent-task-1", "worker-1").status());
        assertEquals(AgentTaskCycleOutcome.Status.CONTINUE,
                graphRunner.runCycle("agent-task-1", "worker-1").status());
        assertEquals(AgentTaskCycleOutcome.Status.COMPLETED,
                graphRunner.runCycle("agent-task-1", "worker-1").status());

        InOrder executionOrder = inOrder(orchestrator);
        executionOrder.verify(orchestrator).executeRegisteredCall(any(), any(), eq(firstCall));
        executionOrder.verify(orchestrator).executeRegisteredCall(any(), any(), eq(secondCall));
        executionOrder.verifyNoMoreInteractions();
        ArgumentCaptor<AgentTaskPlanningContext> contexts = ArgumentCaptor.forClass(AgentTaskPlanningContext.class);
        verify(planner, times(3)).plan(contexts.capture());
        assertEquals(List.of(0, 1, 2), contexts.getAllValues().stream()
                .map(context -> context.completedSteps().size())
                .toList());
        verify(stateService).completeTask("agent-task-1", "worker-1", "goal complete");
    }

    private AgentTool tool(String name) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of("scope", Map.of("type", "string")));
        AgentTool tool = mock(AgentTool.class);
        when(tool.definition()).thenReturn(definition);
        return tool;
    }

    private AgentToolCall call(String toolName, String scope) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(toolName);
        call.setSummary("read " + scope);
        call.setInput(Map.of("scope", scope));
        return call;
    }

    private SysAgentTask task(int completedSteps) {
        SysAgentTask task = new SysAgentTask();
        task.setTaskId("agent-task-1");
        task.setGoal("inspect health and release");
        task.setSessionId("session-1");
        task.setStatus(AgentTaskStatus.RUNNING.name());
        task.setOperatorId(7L);
        task.setOperatorName("admin");
        task.setMaxSteps(5);
        task.setCompletedSteps(completedSteps);
        task.setCurrentStepOrder(completedSteps);
        task.setLeaseOwner("worker-1");
        return task;
    }

    private SysAgentTaskStep completedStep(int order, String toolName) {
        SysAgentTaskStep step = runningStep(order, toolName);
        step.setStatus(AgentTaskStepStatus.COMPLETED.name());
        step.setResultSummary("completed " + toolName);
        return step;
    }

    private SysAgentTaskStep runningStep(int order, String toolName) {
        SysAgentTaskStep step = new SysAgentTaskStep();
        step.setTaskId("agent-task-1");
        step.setStepOrder(order);
        step.setToolName(toolName);
        step.setStatus(AgentTaskStepStatus.RUNNING.name());
        return step;
    }

    private AgentChatResponse answered(String answer) {
        AgentChatResponse response = new AgentChatResponse();
        response.setStatus("answered");
        response.setAnswer(answer);
        return response;
    }
}
