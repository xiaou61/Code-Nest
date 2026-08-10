package com.xiaou.system.agent.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.system.agent.AgentResolvedToolCall;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolRegistry;
import com.xiaou.system.agent.DeterministicAgentPlanResolver;
import com.xiaou.system.domain.SysAgentTaskStep;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LlmAgentTaskPlannerTest {

    @Test
    void shouldFilterCandidateInputToRegisteredToolSchema() {
        AgentTool tool = tool("system.health.read");
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        stubSuccessfulResponse(aiExecutionSupport, """
                {
                  "decision": "execute",
                  "toolName": "system.health.read",
                  "input": {"scope": "runtime", "ignored": "drop-me"},
                  "summary": "Read runtime health",
                  "confidence": 0.95,
                  "missingFields": []
                }
                """);

        LlmAgentTaskPlanner planner = new LlmAgentTaskPlanner(
                mock(DeterministicAgentPlanResolver.class),
                registry,
                aiExecutionSupport,
                new ObjectMapper()
        );

        AgentTaskPlanDecision decision = planner.plan(
                new AgentTaskPlanningContext("inspect runtime", List.of(), 5)
        );

        assertTrue(decision.execute());
        assertEquals("system.health.read", decision.call().getToolName());
        assertEquals(Map.of("scope", "runtime"), decision.call().getInput());
        assertFalse(decision.fallback());
    }

    @Test
    void shouldUseDeterministicFallbackOnlyBeforeAnyCompletedStep() {
        AgentTool tool = tool("system.health.read");
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        DeterministicAgentPlanResolver deterministic = mock(DeterministicAgentPlanResolver.class);
        AgentToolCall call = new AgentToolCall();
        call.setToolName("system.health.read");
        call.setInput(Map.of("scope", "runtime"));
        when(deterministic.resolve("inspect runtime"))
                .thenReturn(Optional.of(new AgentResolvedToolCall(tool, call)));
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        stubFallback(aiExecutionSupport);

        LlmAgentTaskPlanner planner = new LlmAgentTaskPlanner(
                deterministic,
                registry,
                aiExecutionSupport,
                new ObjectMapper()
        );

        AgentTaskPlanDecision first = planner.plan(
                new AgentTaskPlanningContext("inspect runtime", List.of(), 5)
        );
        SysAgentTaskStep completed = new SysAgentTaskStep();
        completed.setToolName("system.health.read");
        completed.setStatus(AgentTaskStepStatus.COMPLETED.name());
        AgentTaskPlanDecision later = planner.plan(
                new AgentTaskPlanningContext("inspect runtime", List.of(completed), 4)
        );

        assertTrue(first.execute());
        assertTrue(first.fallback());
        assertTrue(later.complete());
    }

    @Test
    void shouldBlockWhenPlannerReportsMissingRequiredInput() {
        AgentTool tool = tool("system.health.read");
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        stubSuccessfulResponse(aiExecutionSupport, """
                {
                  "decision": "execute",
                  "toolName": "system.health.read",
                  "input": {},
                  "summary": "Need a scope",
                  "confidence": 0.95,
                  "missingFields": ["scope"]
                }
                """);
        DeterministicAgentPlanResolver deterministic = mock(DeterministicAgentPlanResolver.class);

        AgentTaskPlanDecision decision = new LlmAgentTaskPlanner(
                deterministic,
                registry,
                aiExecutionSupport,
                new ObjectMapper()
        ).plan(new AgentTaskPlanningContext("inspect runtime", List.of(), 5));

        assertTrue(decision.waitingInput());
        assertEquals("MISSING_INPUT", decision.code());
        assertTrue(decision.reason().contains("scope"));
        verifyNoInteractions(deterministic);
    }

    @Test
    void shouldExposeWorkflowContextToTaskPlannerPromptVariables() {
        AgentTool tool = tool("system.health.read");
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        stubSuccessfulResponse(aiExecutionSupport, """
                {
                  "decision": "complete",
                  "toolName": "",
                  "input": {},
                  "summary": "context received",
                  "confidence": 0.95,
                  "missingFields": []
                }
                """);

        LlmAgentTaskPlanner planner = new LlmAgentTaskPlanner(
                mock(DeterministicAgentPlanResolver.class),
                registry,
                aiExecutionSupport,
                new ObjectMapper()
        );
        planner.plan(new AgentTaskPlanningContext(
                "inspect runtime", List.of(), 5, Map.of("region", "cn")));

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(aiExecutionSupport).chatWithFallbackResult(
                eq("admin.agent.task.plan"),
                eq(AdminAgentPromptSpecs.TASK_NEXT_STEP),
                variables.capture(),
                org.mockito.ArgumentMatchers.<Function<String, AgentTaskPlanDecision>>any(),
                org.mockito.ArgumentMatchers.<Supplier<AgentTaskPlanDecision>>any()
        );
        assertEquals("{\"region\":\"cn\"}", variables.getValue().get("workflowContextJson"));
    }

    @SuppressWarnings("unchecked")
    private void stubSuccessfulResponse(AiExecutionSupport support, String response) {
        when(support.chatWithFallbackResult(
                eq("admin.agent.task.plan"),
                eq(AdminAgentPromptSpecs.TASK_NEXT_STEP),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                org.mockito.ArgumentMatchers.<Function<String, AgentTaskPlanDecision>>any(),
                org.mockito.ArgumentMatchers.<Supplier<AgentTaskPlanDecision>>any()
        )).thenAnswer(invocation -> {
            Function<String, AgentTaskPlanDecision> parser = invocation.getArgument(3);
            return new AiExecutionResult<>(parser.apply(response), "SUCCESS", "test", "test", "test");
        });
    }

    @SuppressWarnings("unchecked")
    private void stubFallback(AiExecutionSupport support) {
        when(support.chatWithFallbackResult(
                eq("admin.agent.task.plan"),
                eq(AdminAgentPromptSpecs.TASK_NEXT_STEP),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                org.mockito.ArgumentMatchers.<Function<String, AgentTaskPlanDecision>>any(),
                org.mockito.ArgumentMatchers.<Supplier<AgentTaskPlanDecision>>any()
        )).thenAnswer(invocation -> {
            Supplier<AgentTaskPlanDecision> fallback = invocation.getArgument(4);
            return new AiExecutionResult<>(fallback.get(), "MODEL_UNAVAILABLE", "test", "test", null);
        });
    }

    private AgentTool tool(String name) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle("Runtime health");
        definition.setDescription("Reads runtime health");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(new LinkedHashMap<>(Map.of(
                "scope", Map.of("type", "string", "minLength", 1)
        )));
        definition.setRequiredInputKeys(List.of("scope"));

        AgentTool tool = mock(AgentTool.class);
        when(tool.definition()).thenReturn(definition);
        return tool;
    }
}
