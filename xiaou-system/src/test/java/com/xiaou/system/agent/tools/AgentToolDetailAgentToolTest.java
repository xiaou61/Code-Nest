package com.xiaou.system.agent.tools;

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

class AgentToolDetailAgentToolTest {

    @Test
    void shouldResolveToolDetailQuestionWithRegisteredToolName() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(definition("chat.userBan.unban", "medium", "WRITE", true)));
        AgentToolDetailAgentTool tool = new AgentToolDetailAgentTool(catalogService);

        AgentToolCall call = tool.resolve("chat.userBan.unban 这个工具需要什么权限").orElseThrow();

        assertEquals("system.agent.tools.detail", call.getToolName());
        assertEquals("chat.userBan.unban", call.getInput().get("toolName"));
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldReturnToolDetailArtifact() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(definition("chat.userBan.unban", "medium", "WRITE", true)));
        AgentToolDetailAgentTool tool = new AgentToolDetailAgentTool(catalogService);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of("toolName", "chat.userBan.unban"));
        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("chat.userBan.unban"));
        assertEquals(1, result.getArtifacts().size());
        assertFalse(result.getNextActions().isEmpty());

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentToolDetail", artifact.getType());
        assertEquals("chat.userBan.unban", artifact.getData().get("name"));
        assertEquals("WRITE", artifact.getData().get("riskCategory"));
        assertEquals(true, artifact.getData().get("requiresConfirmation"));
        assertEquals(List.of("agent:test:write"), artifact.getData().get("requiredPermissions"));
        assertEquals(Map.of("userId", Map.of("type", "integer", "minimum", 1)), artifact.getData().get("inputSchema"));
    }

    @Test
    void shouldReturnErrorWhenToolMissing() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of());
        AgentToolDetailAgentTool tool = new AgentToolDetailAgentTool(catalogService);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of("toolName", "system.unknown"));
        AgentToolResult result = tool.execute(call, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getSummary().contains("没有找到"));
        assertEquals("后端智能体工具不存在: system.unknown", result.getErrorMessage());
    }

    private AgentToolDefinition definition(String name, String riskLevel, String riskCategory, boolean confirmationRequired) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle("解除聊天禁言");
        definition.setDescription("解除指定用户当前生效的聊天禁言");
        definition.setIntent(name);
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel(riskLevel);
        definition.setRiskCategory(riskCategory);
        definition.setConfirmationRequired(confirmationRequired);
        definition.setConfirmationText("确认解除禁言");
        definition.setInputSchema(Map.of("userId", Map.of("type", "integer", "minimum", 1)));
        definition.setRequiredInputKeys(List.of("userId"));
        definition.setRequiredPermissions(List.of("agent:test:write"));
        return definition;
    }
}
