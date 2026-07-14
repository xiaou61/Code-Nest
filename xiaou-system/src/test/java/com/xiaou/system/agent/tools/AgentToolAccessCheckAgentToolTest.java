package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
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

class AgentToolAccessCheckAgentToolTest {

    @Test
    void shouldResolveCurrentOperatorToolAccessQuestion() {
        AgentToolAccessCheckAgentTool tool = new AgentToolAccessCheckAgentTool(catalogService(
                definition("chat.userBan.unban", List.of("agent:chat:user-ban:write"), List.of("SUPER_ADMIN"))
        ));

        AgentToolCall call = tool.resolve("我能不能用 chat.userBan.unban 这个工具").orElseThrow();

        assertEquals("system.agent.tools.access_check", call.getToolName());
        assertEquals("chat.userBan.unban", call.getInput().get("toolName"));
        assertTrue(tool.resolve("chat.userBan.unban 这个工具需要什么权限").isEmpty());
    }

    @Test
    void shouldReportAllowedAccessForCurrentOperator() {
        AgentToolAccessCheckAgentTool tool = new AgentToolAccessCheckAgentTool(catalogService(
                definition("chat.userBan.unban", List.of("agent:chat:user-ban:write"), List.of("SUPER_ADMIN"))
        ));
        AgentToolCall call = call(tool, "chat.userBan.unban");
        AgentOperator operator = new AgentOperator(
                7L,
                "Alice",
                "tenant-a",
                List.of("SUPER_ADMIN"),
                List.of("agent:chat:user-ban:write", "agent:runtime:tool-access:read")
        );

        AgentToolResult result = tool.execute(call, new AgentExecutionContext("session-1", "我能不能用", operator));

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("可以访问"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentToolAccessCheck", artifact.getType());
        assertEquals("chat.userBan.unban", artifact.getData().get("toolName"));
        assertEquals(true, artifact.getData().get("accessAllowed"));
        assertEquals(List.of(), artifact.getData().get("missingPermissions"));
        assertEquals(List.of(), artifact.getData().get("missingRoles"));
        assertEquals(false, artifact.getData().get("tenantRuntimeCheckRequired"));
        assertEquals(true, artifact.getData().get("requiresConfirmation"));
    }

    @Test
    void shouldReportMissingPermissionsAndRolesForCurrentOperator() {
        AgentToolAccessCheckAgentTool tool = new AgentToolAccessCheckAgentTool(catalogService(
                definition("chat.userBan.unban", List.of("agent:chat:user-ban:write"), List.of("SUPER_ADMIN", "OPS"))
        ));
        AgentToolCall call = call(tool, "chat.userBan.unban");
        AgentOperator operator = new AgentOperator(7L, "Alice", "tenant-a", List.of("AUDITOR"), List.of("agent:runtime:tool-access:read"));

        AgentToolResult result = tool.execute(call, new AgentExecutionContext("session-1", "我能不能用", operator));

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("暂时不能访问"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals(false, artifact.getData().get("accessAllowed"));
        assertEquals(List.of("agent:chat:user-ban:write"), artifact.getData().get("missingPermissions"));
        assertEquals(List.of("SUPER_ADMIN", "OPS"), artifact.getData().get("missingRoles"));
    }

    @Test
    void shouldReportTenantRuntimeCheckForSameTenantTool() {
        AgentToolDefinition definition = definition("tenant.tool", List.of("agent:tenant:read"), List.of());
        definition.setTenantScope("SAME_TENANT");
        AgentToolAccessCheckAgentTool tool = new AgentToolAccessCheckAgentTool(catalogService(definition));
        AgentOperator operator = new AgentOperator(7L, "Alice", "tenant-a", List.of(), List.of("agent:tenant:read"));

        AgentToolResult result = tool.execute(call(tool, "tenant.tool"), new AgentExecutionContext("session-1", "能不能用", operator));

        assertTrue(result.isSuccess());
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals(true, artifact.getData().get("tenantRuntimeCheckRequired"));
        assertEquals(true, artifact.getData().get("accessAllowed"));
        assertTrue(((List<?>) artifact.getData().get("notes")).contains("该工具要求 SAME_TENANT，真实执行时还会校验输入 tenantId 与当前操作者租户一致。"));
    }

    @Test
    void shouldReturnErrorWhenTargetToolMissing() {
        AgentToolAccessCheckAgentTool tool = new AgentToolAccessCheckAgentTool(catalogService());

        AgentToolResult result = tool.execute(call(tool, "system.unknown"), null);

        assertFalse(result.isSuccess());
        assertTrue(result.getSummary().contains("没有找到"));
        assertEquals("后端智能体工具不存在: system.unknown", result.getErrorMessage());
    }

    private AgentToolCall call(AgentToolAccessCheckAgentTool tool, String toolName) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of("toolName", toolName));
        return call;
    }

    private AgentToolCatalogService catalogService(AgentToolDefinition... definitions) {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(definitions));
        return catalogService;
    }

    private AgentToolDefinition definition(String name, List<String> permissions, List<String> roles) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle(name);
        definition.setDescription(name);
        definition.setIntent(name);
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("medium");
        definition.setRiskCategory("WRITE");
        definition.setConfirmationRequired(true);
        definition.setConfirmationText("确认执行");
        definition.setInputSchema(Map.of("userId", Map.of("type", "integer", "minimum", 1)));
        definition.setRequiredInputKeys(List.of("userId"));
        definition.setRequiredPermissions(permissions);
        definition.setRequiredRoles(roles);
        return definition;
    }
}
