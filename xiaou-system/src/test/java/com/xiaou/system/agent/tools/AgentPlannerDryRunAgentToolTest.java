package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentPlanResolution;
import com.xiaou.system.agent.AgentPlanResolver;
import com.xiaou.system.agent.AgentResolvedToolCall;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentPlannerDryRunAgentToolTest {

    @Test
    void shouldResolvePlannerDryRunQuestionAndExtractTargetMessage() {
        AgentPlannerDryRunAgentTool tool = new AgentPlannerDryRunAgentTool(provider(null));

        AgentToolCall call = tool.resolve("planner 预演：帮我查最近3条操作日志").orElseThrow();

        assertEquals("system.agent.planner.dry_run", call.getToolName());
        assertEquals("帮我查最近3条操作日志", call.getInput().get("message"));
        assertTrue(tool.resolve("planner 结构化输出契约和命中规则是什么").isEmpty());
    }

    @Test
    void shouldReportResolvedCandidateWithoutExecutingTargetTool() {
        AgentPlanResolver resolver = mock(AgentPlanResolver.class);
        TrackingAgentTool targetTool = new TrackingAgentTool();
        AgentToolCall targetCall = new AgentToolCall();
        targetCall.setToolName("chat.userBan.unban");
        targetCall.setSummary("解除聊天禁言");
        targetCall.setInput(Map.of("userId", 88));
        when(resolver.resolvePlan(argThat((AgentExecutionContext context) -> "帮我把用户88解除禁言".equals(context.message()))))
                .thenReturn(AgentPlanResolution.resolved(new AgentResolvedToolCall(targetTool, targetCall)));
        AgentPlannerDryRunAgentTool tool = new AgentPlannerDryRunAgentTool(provider(resolver));

        AgentToolResult result = tool.execute(call(tool, "帮我把用户88解除禁言"),
                new AgentExecutionContext("session-1", "planner 预演", null));

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("chat.userBan.unban"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentPlannerDryRun", artifact.getType());
        assertEquals("RESOLVED", artifact.getData().get("status"));
        assertEquals("chat.userBan.unban", artifact.getData().get("toolName"));
        assertEquals(Map.of("userId", 88), artifact.getData().get("input"));
        assertEquals(true, artifact.getData().get("dryRunOnly"));
        assertFalse(targetTool.previewCalled);
        assertFalse(targetTool.executeCalled);
    }

    @Test
    void shouldReportClarificationWhenPlannerNeedsMoreInput() {
        AgentPlanResolver resolver = mock(AgentPlanResolver.class);
        when(resolver.resolvePlan("帮我解除禁言"))
                .thenReturn(AgentPlanResolution.clarification("还需要补充这些信息后才能继续：userId", List.of("请补充字段：userId")));
        AgentPlannerDryRunAgentTool tool = new AgentPlannerDryRunAgentTool(provider(resolver));

        AgentToolResult result = tool.execute(call(tool, "帮我解除禁言"), null);

        assertTrue(result.isSuccess());
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("CLARIFICATION", artifact.getData().get("status"));
        assertTrue(String.valueOf(artifact.getData().get("message")).contains("userId"));
        assertEquals(List.of("请补充字段：userId"), artifact.getData().get("nextActions"));
    }

    @Test
    void shouldReportEmptyWhenPlannerDoesNotResolveTool() {
        AgentPlanResolver resolver = mock(AgentPlanResolver.class);
        when(resolver.resolvePlan("随便聊聊")).thenReturn(AgentPlanResolution.empty());
        AgentPlannerDryRunAgentTool tool = new AgentPlannerDryRunAgentTool(provider(resolver));

        AgentToolResult result = tool.execute(call(tool, "随便聊聊"), null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("未命中"));
        assertEquals("EMPTY", result.getArtifacts().get(0).getData().get("status"));
    }

    @Test
    void shouldReturnErrorWhenPlannerResolverUnavailable() {
        AgentPlannerDryRunAgentTool tool = new AgentPlannerDryRunAgentTool(provider(null));

        AgentToolResult result = tool.execute(call(tool, "帮我查日志"), null);

        assertFalse(result.isSuccess());
        assertTrue(result.getSummary().contains("planner 不可用"));
    }

    private AgentToolCall call(AgentPlannerDryRunAgentTool tool, String message) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of("message", message));
        return call;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AgentPlanResolver> provider(AgentPlanResolver resolver) {
        ObjectProvider<AgentPlanResolver> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(resolver);
        return provider;
    }

    private AgentToolDefinition targetDefinition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("chat.userBan.unban");
        definition.setTitle("解除聊天禁言");
        definition.setDescription("解除指定用户当前生效的聊天禁言");
        definition.setIntent("chat.userBan.unban");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("medium");
        definition.setRiskCategory("WRITE");
        definition.setConfirmationRequired(true);
        definition.setConfirmationText("确认解除禁言");
        definition.setInputSchema(Map.of("userId", Map.of("type", "integer", "minimum", 1)));
        definition.setRequiredInputKeys(List.of("userId"));
        definition.setRequiredPermissions(List.of("agent:chat:user-ban:write"));
        return definition;
    }

    private class TrackingAgentTool implements AgentTool {
        private boolean previewCalled;
        private boolean executeCalled;

        @Override
        public AgentToolDefinition definition() {
            return targetDefinition();
        }

        @Override
        public Optional<AgentToolCall> resolve(String message) {
            return Optional.empty();
        }

        @Override
        public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
            executeCalled = true;
            return new AgentToolResult();
        }

        @Override
        public com.xiaou.system.agent.AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
            previewCalled = true;
            return new com.xiaou.system.agent.AgentToolPreview();
        }
    }
}
