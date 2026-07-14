package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentPolicyEngine;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolCatalogService;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentPolicyDryRunAgentToolTest {

    @Test
    void shouldResolvePolicyDryRunQuestionWithRegisteredToolName() {
        AgentPolicyDryRunAgentTool tool = new AgentPolicyDryRunAgentTool(catalogService(writeTool()), new AgentPolicyEngine());

        AgentToolCall call = tool.resolve("对 chat.userBan.unban 做 policy dry run").orElseThrow();

        assertEquals("system.agent.policy.dry_run", call.getToolName());
        assertEquals("chat.userBan.unban", call.getInput().get("toolName"));
        assertTrue(tool.resolve("chat.userBan.unban 这个工具需要什么权限").isEmpty());
    }

    @Test
    void shouldReportConfirmationRequiredWhenPolicyAllowsWriteTool() {
        AgentPolicyDryRunAgentTool tool = new AgentPolicyDryRunAgentTool(catalogService(writeTool()), new AgentPolicyEngine());
        AgentToolCall call = call(tool, "chat.userBan.unban", Map.of("userId", 88));
        AgentOperator operator = new AgentOperator(7L, "Alice", "tenant-a", List.of("SUPER_ADMIN"),
                List.of("agent:chat:user-ban:write", "agent:runtime:policy:read"));

        AgentToolResult result = tool.execute(call, new AgentExecutionContext("session-1", "策略预检", operator));

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("需要强确认"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentPolicyDryRun", artifact.getType());
        assertEquals("chat.userBan.unban", artifact.getData().get("toolName"));
        assertEquals(Map.of("userId", 88), artifact.getData().get("input"));
        assertEquals(true, artifact.getData().get("allowed"));
        assertEquals(true, artifact.getData().get("confirmationRequired"));
        assertEquals("确认解除禁言", artifact.getData().get("confirmationText"));
        assertEquals("", artifact.getData().get("errorCode"));
        assertEquals("", artifact.getData().get("rejectionReason"));
    }

    @Test
    void shouldReportSchemaRejectionForMissingRequiredInput() {
        AgentPolicyDryRunAgentTool tool = new AgentPolicyDryRunAgentTool(catalogService(writeTool()), new AgentPolicyEngine());
        AgentOperator operator = new AgentOperator(7L, "Alice", "tenant-a", List.of("SUPER_ADMIN"),
                List.of("agent:chat:user-ban:write", "agent:runtime:policy:read"));

        AgentToolResult result = tool.execute(call(tool, "chat.userBan.unban", Map.of()),
                new AgentExecutionContext("session-1", "策略预检", operator));

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("拒绝"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals(false, artifact.getData().get("allowed"));
        assertEquals("SCHEMA_VALIDATION_FAILED", artifact.getData().get("errorCode"));
        assertTrue(String.valueOf(artifact.getData().get("rejectionReason")).contains("userId"));
    }

    @Test
    void shouldReportPermissionRejectionForCurrentOperator() {
        AgentPolicyDryRunAgentTool tool = new AgentPolicyDryRunAgentTool(catalogService(writeTool()), new AgentPolicyEngine());
        AgentOperator operator = new AgentOperator(7L, "Alice", "tenant-a", List.of("SUPER_ADMIN"),
                List.of("agent:runtime:policy:read"));

        AgentToolResult result = tool.execute(call(tool, "chat.userBan.unban", Map.of("userId", 88)),
                new AgentExecutionContext("session-1", "策略预检", operator));

        assertTrue(result.isSuccess());
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals(false, artifact.getData().get("allowed"));
        assertEquals("POLICY_REJECTED", artifact.getData().get("errorCode"));
        assertTrue(String.valueOf(artifact.getData().get("rejectionReason")).contains("agent:chat:user-ban:write"));
    }

    @Test
    void shouldReturnErrorWhenTargetToolMissing() {
        AgentPolicyDryRunAgentTool tool = new AgentPolicyDryRunAgentTool(catalogService(), new AgentPolicyEngine());

        AgentToolResult result = tool.execute(call(tool, "system.unknown", Map.of()), null);

        assertFalse(result.isSuccess());
        assertTrue(result.getSummary().contains("没有找到"));
        assertEquals("后端智能体工具不存在: system.unknown", result.getErrorMessage());
    }

    private AgentToolCall call(AgentPolicyDryRunAgentTool tool, String toolName, Map<String, Object> input) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of("toolName", toolName, "input", input));
        return call;
    }

    private AgentToolCatalogService catalogService(AgentToolDefinition... definitions) {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(definitions));
        return catalogService;
    }

    private AgentToolDefinition writeTool() {
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
}
