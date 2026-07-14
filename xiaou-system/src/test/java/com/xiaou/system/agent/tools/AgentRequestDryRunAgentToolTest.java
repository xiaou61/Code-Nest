package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentPlanResolution;
import com.xiaou.system.agent.AgentPlanResolver;
import com.xiaou.system.agent.AgentResolvedToolCall;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.dto.AgentChatDiffItem;
import com.xiaou.system.dto.AgentChatPlanStep;
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

class AgentRequestDryRunAgentToolTest {

    @Test
    void shouldResolveRequestDryRunAndExtractTargetMessage() {
        AgentRequestDryRunAgentTool tool = new AgentRequestDryRunAgentTool(provider(null), new com.xiaou.system.agent.AgentPolicyEngine());

        AgentToolCall call = tool.resolve("运行时预演：帮我查最近3条操作日志").orElseThrow();

        assertEquals("system.agent.request.dry_run", call.getToolName());
        assertEquals("帮我查最近3条操作日志", call.getInput().get("message"));
        assertTrue(tool.resolve("planner 预演：帮我查日志").isEmpty());
        assertTrue(tool.resolve("对 chat.userBan.unban 做策略预检").isEmpty());
    }

    @Test
    void shouldReportReadonlyRequestWithoutExecutingTargetTool() {
        AgentPlanResolver resolver = mock(AgentPlanResolver.class);
        TrackingAgentTool targetTool = new TrackingAgentTool(readonlyDefinition());
        AgentToolCall targetCall = targetCall("system.operationLog.list", Map.of("pageSize", 3));
        when(resolver.resolvePlan(argThat((AgentExecutionContext context) -> "帮我查最近3条操作日志".equals(context.message()))))
                .thenReturn(AgentPlanResolution.resolved(new AgentResolvedToolCall(targetTool, targetCall)));
        AgentRequestDryRunAgentTool tool = new AgentRequestDryRunAgentTool(provider(resolver), new com.xiaou.system.agent.AgentPolicyEngine());

        AgentToolResult result = tool.execute(call(tool, "帮我查最近3条操作日志"),
                new AgentExecutionContext("session-1", "运行时预演", operator("agent:system:operation-log:read")));

        assertTrue(result.isSuccess());
        assertFalse(targetTool.previewCalled);
        assertFalse(targetTool.executeCalled);
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentRequestDryRun", artifact.getType());
        assertEquals("WOULD_EXECUTE_READONLY", artifact.getData().get("status"));
        assertEquals("system.operationLog.list", artifact.getData().get("toolName"));
        assertEquals(true, artifact.getData().get("policyAllowed"));
        assertEquals(false, artifact.getData().get("confirmationRequired"));
        assertEquals(false, artifact.getData().get("targetExecuted"));
        assertEquals(false, artifact.getData().get("auditCreated"));
    }

    @Test
    void shouldPreviewWriteRequestWithoutExecutingOrAuditingTargetTool() {
        AgentPlanResolver resolver = mock(AgentPlanResolver.class);
        TrackingAgentTool targetTool = new TrackingAgentTool(writeDefinition());
        AgentToolCall targetCall = targetCall("chat.userBan.unban", Map.of("userId", 88));
        when(resolver.resolvePlan(argThat((AgentExecutionContext context) -> "帮我把用户88解除禁言".equals(context.message()))))
                .thenReturn(AgentPlanResolution.resolved(new AgentResolvedToolCall(targetTool, targetCall)));
        AgentRequestDryRunAgentTool tool = new AgentRequestDryRunAgentTool(provider(resolver), new com.xiaou.system.agent.AgentPolicyEngine());

        AgentToolResult result = tool.execute(call(tool, "帮我把用户88解除禁言"),
                new AgentExecutionContext("session-1", "运行时预演", operator("agent:chat:user-ban:write")));

        assertTrue(result.isSuccess());
        assertTrue(targetTool.previewCalled);
        assertFalse(targetTool.executeCalled);
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("CONFIRM_REQUIRED", artifact.getData().get("status"));
        assertEquals(true, artifact.getData().get("policyAllowed"));
        assertEquals(true, artifact.getData().get("confirmationRequired"));
        assertEquals("确认解除禁言", artifact.getData().get("confirmationText"));
        assertEquals(true, artifact.getData().get("targetPreviewed"));
        assertEquals(false, artifact.getData().get("targetExecuted"));
        assertEquals(false, artifact.getData().get("auditCreated"));
        assertTrue(result.getSummary().contains("需要强确认"));
        assertEquals(1, result.getDiff().size());
    }

    @Test
    void shouldRejectByPolicyWithoutPreviewingTargetTool() {
        AgentPlanResolver resolver = mock(AgentPlanResolver.class);
        TrackingAgentTool targetTool = new TrackingAgentTool(writeDefinition());
        AgentToolCall targetCall = targetCall("chat.userBan.unban", Map.of("userId", 88));
        when(resolver.resolvePlan(argThat((AgentExecutionContext context) -> "帮我把用户88解除禁言".equals(context.message()))))
                .thenReturn(AgentPlanResolution.resolved(new AgentResolvedToolCall(targetTool, targetCall)));
        AgentRequestDryRunAgentTool tool = new AgentRequestDryRunAgentTool(provider(resolver), new com.xiaou.system.agent.AgentPolicyEngine());

        AgentToolResult result = tool.execute(call(tool, "帮我把用户88解除禁言"),
                new AgentExecutionContext("session-1", "运行时预演", operator("agent:runtime:policy:read")));

        assertTrue(result.isSuccess());
        assertFalse(targetTool.previewCalled);
        assertFalse(targetTool.executeCalled);
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("POLICY_REJECTED", artifact.getData().get("status"));
        assertEquals(false, artifact.getData().get("policyAllowed"));
        assertTrue(String.valueOf(artifact.getData().get("rejectionReason")).contains("agent:chat:user-ban:write"));
    }

    @Test
    void shouldReportClarificationAndEmptyPlannerOutcomes() {
        AgentPlanResolver resolver = mock(AgentPlanResolver.class);
        when(resolver.resolvePlan("帮我解除禁言"))
                .thenReturn(AgentPlanResolution.clarification("还需要补充这些信息后才能继续：userId", List.of("请补充字段：userId")));
        when(resolver.resolvePlan("随便聊聊")).thenReturn(AgentPlanResolution.empty());
        AgentRequestDryRunAgentTool tool = new AgentRequestDryRunAgentTool(provider(resolver), new com.xiaou.system.agent.AgentPolicyEngine());

        AgentToolResult clarification = tool.execute(call(tool, "帮我解除禁言"), null);
        AgentToolResult empty = tool.execute(call(tool, "随便聊聊"), null);

        assertTrue(clarification.isSuccess());
        assertEquals("CLARIFICATION", clarification.getArtifacts().get(0).getData().get("status"));
        assertTrue(empty.isSuccess());
        assertEquals("EMPTY", empty.getArtifacts().get(0).getData().get("status"));
    }

    private AgentToolCall call(AgentRequestDryRunAgentTool tool, String message) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of("message", message));
        return call;
    }

    private AgentToolCall targetCall(String toolName, Map<String, Object> input) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(toolName);
        call.setSummary("target candidate");
        call.setInput(input);
        return call;
    }

    private AgentOperator operator(String... permissions) {
        return new AgentOperator(7L, "Alice", "tenant-a", List.of("SUPER_ADMIN"), List.of(permissions));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AgentPlanResolver> provider(AgentPlanResolver resolver) {
        ObjectProvider<AgentPlanResolver> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(resolver);
        return provider;
    }

    private AgentToolDefinition readonlyDefinition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.operationLog.list");
        definition.setTitle("查询操作日志");
        definition.setDescription("分页查询系统操作日志");
        definition.setIntent("system.operationLog.list");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of("pageSize", Map.of("type", "integer", "minimum", 1)));
        definition.setRequiredPermissions(List.of("agent:system:operation-log:read"));
        return definition;
    }

    private AgentToolDefinition writeDefinition() {
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

    private static class TrackingAgentTool implements AgentTool {
        private final AgentToolDefinition definition;
        private boolean previewCalled;
        private boolean executeCalled;

        private TrackingAgentTool(AgentToolDefinition definition) {
            this.definition = definition;
        }

        @Override
        public AgentToolDefinition definition() {
            return definition;
        }

        @Override
        public Optional<AgentToolCall> resolve(String message) {
            return Optional.empty();
        }

        @Override
        public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
            previewCalled = true;
            AgentToolPreview preview = new AgentToolPreview();
            preview.setSummary("将解除用户 88 的聊天禁言。");
            preview.getPlan().add(new AgentChatPlanStep("preview", "预演解除禁言", "done"));
            preview.getDiff().add(new AgentChatDiffItem("chat.userBan", "before", "muted", "active"));
            return preview;
        }

        @Override
        public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
            executeCalled = true;
            throw new AssertionError("request dry-run must not execute target tool");
        }
    }
}
