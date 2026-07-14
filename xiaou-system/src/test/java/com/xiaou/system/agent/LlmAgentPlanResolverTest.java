package com.xiaou.system.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
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
import static org.mockito.Mockito.when;

class LlmAgentPlanResolverTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPreferLlmPlannerBeforeDeterministicFallback() {
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(resolvingTool("system.operationLog.list")));
        LlmAgentPlanResolver resolver = resolver(registry, aiExecutionSupport);
        stubAiResponse(aiExecutionSupport, """
                {
                  "toolName": "system.operationLog.list",
                  "input": {},
                  "confidence": 0.95,
                  "missingFields": []
                }
                """);

        AgentPlanResolution resolution = resolver.resolvePlan("查最近3条操作日志");

        assertTrue(resolution.isResolved());
        assertEquals("system.operationLog.list", resolution.getResolvedCall().call().getToolName());
        assertEquals("LLM planner candidate", resolution.getResolvedCall().call().getSummary());
        verify(aiExecutionSupport).chatWithFallback(
                eq("admin.agent.plan"),
                eq(AdminAgentPromptSpecs.PLAN),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                org.mockito.ArgumentMatchers.<Function<String, AgentPlanResolution>>any(),
                org.mockito.ArgumentMatchers.<Supplier<AgentPlanResolution>>any()
        );
    }

    @Test
    void shouldUseDeterministicResolverOnlyWhenLlmPlannerHasNoResolution() {
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(resolvingTool("system.operationLog.list")));
        LlmAgentPlanResolver resolver = resolver(registry, aiExecutionSupport);
        when(aiExecutionSupport.chatWithFallback(
                eq("admin.agent.plan"),
                eq(AdminAgentPromptSpecs.PLAN),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                org.mockito.ArgumentMatchers.<Function<String, AgentPlanResolution>>any(),
                org.mockito.ArgumentMatchers.<Supplier<AgentPlanResolution>>any()
        )).thenReturn(AgentPlanResolution.empty());

        AgentPlanResolution resolution = resolver.resolvePlan("查最近3条操作日志");

        assertTrue(resolution.isResolved());
        assertEquals("system.operationLog.list", resolution.getResolvedCall().call().getToolName());
        assertEquals("deterministic", resolution.getResolvedCall().call().getSummary());
    }

    @Test
    void shouldResolveLlmCandidateWithRegisteredToolAndSchemaFilteredInput() {
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool("chat.userBan.unban")));
        LlmAgentPlanResolver resolver = resolver(registry, aiExecutionSupport);
        stubAiResponse(aiExecutionSupport, """
                {
                  "toolName": "chat.userBan.unban",
                  "input": {"userId": 88, "sql": "drop table sys_user"},
                  "confidence": 0.92,
                  "missingFields": []
                }
                """);

        AgentPlanResolution resolution = resolver.resolvePlan("解除 88 的禁言");

        assertTrue(resolution.isResolved());
        assertEquals("chat.userBan.unban", resolution.getResolvedCall().call().getToolName());
        assertEquals(Map.of("userId", 88), resolution.getResolvedCall().call().getInput());
    }

    @Test
    void shouldAskForClarificationWhenPlannerReportsMissingFields() {
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool("chat.userBan.unban")));
        LlmAgentPlanResolver resolver = resolver(registry, aiExecutionSupport);
        stubAiResponse(aiExecutionSupport, """
                {
                  "toolName": "chat.userBan.unban",
                  "input": {},
                  "confidence": 0.88,
                  "missingFields": ["userId"]
                }
                """);

        AgentPlanResolution resolution = resolver.resolvePlan("解除禁言");

        assertFalse(resolution.isResolved());
        assertEquals(AgentChatErrorCode.PLAN_CLARIFICATION_REQUIRED, resolution.getErrorCode());
        assertTrue(resolution.getMessage().contains("userId"));
    }

    @Test
    void shouldAskForClarificationWhenRegisteredToolRequiresMoreInput() {
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool("chat.userBan.unban")));
        LlmAgentPlanResolver resolver = resolver(registry, aiExecutionSupport);
        stubAiResponse(aiExecutionSupport, """
                {
                  "toolName": "chat.userBan.unban",
                  "input": {},
                  "confidence": 0.88,
                  "missingFields": []
                }
                """);

        AgentPlanResolution resolution = resolver.resolvePlan("解除禁言");

        assertFalse(resolution.isResolved());
        assertEquals(AgentChatErrorCode.PLAN_CLARIFICATION_REQUIRED, resolution.getErrorCode());
        assertTrue(resolution.getNextActions().contains("请补充字段：userId"));
    }

    @Test
    void shouldPassSessionContextToPlannerPrompt() {
        AiExecutionSupport aiExecutionSupport = mock(AiExecutionSupport.class);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool("chat.userBan.unban")));
        LlmAgentPlanResolver resolver = resolver(registry, aiExecutionSupport);
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        when(aiExecutionSupport.chatWithFallback(
                eq("admin.agent.plan"),
                eq(AdminAgentPromptSpecs.PLAN),
                variablesCaptor.capture(),
                org.mockito.ArgumentMatchers.<Function<String, AgentPlanResolution>>any(),
                org.mockito.ArgumentMatchers.<Supplier<AgentPlanResolution>>any()
        )).thenReturn(AgentPlanResolution.empty());

        AgentSessionSnapshot snapshot = new AgentSessionSnapshot();
        snapshot.setSessionId("session-1");
        AgentSessionTurn turn = new AgentSessionTurn();
        turn.setMessage("查 88 的禁言状态");
        turn.setStatus("answered");
        turn.setToolName("chat.userBan.active");
        turn.setAnswer("用户 88 当前处于禁言中");
        snapshot.setRecentTurns(List.of(turn));

        resolver.resolvePlan(new AgentExecutionContext(
                "session-1",
                "那就解除",
                new AgentOperator(1L, "admin"),
                AgentRuntimeTrace.start(),
                snapshot
        ));

        Map<String, Object> variables = variablesCaptor.getValue();
        assertEquals("那就解除", variables.get("message"));
        assertTrue(String.valueOf(variables.get("sessionContextJson")).contains("chat.userBan.active"));
        assertTrue(String.valueOf(variables.get("sessionContextJson")).contains("查 88 的禁言状态"));
    }

    private LlmAgentPlanResolver resolver(AgentToolRegistry registry, AiExecutionSupport aiExecutionSupport) {
        return new LlmAgentPlanResolver(
                new DeterministicAgentPlanResolver(registry),
                registry,
                aiExecutionSupport,
                objectMapper
        );
    }

    private void stubAiResponse(AiExecutionSupport aiExecutionSupport, String content) {
        when(aiExecutionSupport.chatWithFallback(
                eq("admin.agent.plan"),
                eq(AdminAgentPromptSpecs.PLAN),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                org.mockito.ArgumentMatchers.<Function<String, AgentPlanResolution>>any(),
                org.mockito.ArgumentMatchers.<Supplier<AgentPlanResolution>>any()
        )).thenAnswer(invocation -> {
            Function<String, AgentPlanResolution> parser = invocation.getArgument(3);
            return parser.apply(content);
        });
    }

    private AgentTool resolvingTool(String name) {
        return new AgentTool() {
            @Override
            public AgentToolDefinition definition() {
                return toolDefinition(name, Map.of(), List.of());
            }

            @Override
            public Optional<AgentToolCall> resolve(String message) {
                AgentToolCall call = new AgentToolCall();
                call.setToolName(name);
                call.setSummary("deterministic");
                call.setInput(new LinkedHashMap<>());
                return Optional.of(call);
            }

            @Override
            public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
                return new AgentToolPreview();
            }

            @Override
            public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
                AgentToolResult result = new AgentToolResult();
                result.setSuccess(true);
                result.setSummary("done");
                return result;
            }
        };
    }

    private AgentTool tool(String name) {
        return new AgentTool() {
            @Override
            public AgentToolDefinition definition() {
                return toolDefinition(name, Map.of(
                        "userId", Map.of("type", "integer", "minimum", 1)
                ), List.of("userId"));
            }

            @Override
            public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
                return new AgentToolPreview();
            }

            @Override
            public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
                AgentToolResult result = new AgentToolResult();
                result.setSuccess(true);
                result.setSummary("done");
                return result;
            }
        };
    }

    private AgentToolDefinition toolDefinition(String name, Map<String, Object> inputSchema, List<String> requiredInputKeys) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle(name);
        definition.setDescription("测试工具");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(inputSchema);
        definition.setRequiredInputKeys(requiredInputKeys);
        return definition;
    }
}
