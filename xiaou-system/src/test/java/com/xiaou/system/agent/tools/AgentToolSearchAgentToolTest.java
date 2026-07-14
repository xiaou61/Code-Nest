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

class AgentToolSearchAgentToolTest {

    @Test
    void shouldResolveToolSearchQuestionWithExtractedQuery() {
        AgentToolSearchAgentTool tool = new AgentToolSearchAgentTool(catalogService(
                definition("chat.userBan.active", "查询聊天禁言状态", "查询指定用户当前是否有生效禁言"),
                definition("system.operationLog.list", "查询操作日志", "查询后台操作日志")
        ));

        AgentToolCall call = tool.resolve("帮我搜索禁言相关工具").orElseThrow();

        assertEquals("system.agent.tools.search", call.getToolName());
        assertEquals("禁言", call.getInput().get("query"));
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldReturnMatchedToolsByQuery() {
        AgentToolSearchAgentTool tool = new AgentToolSearchAgentTool(catalogService(
                definition("chat.userBan.active", "查询聊天禁言状态", "查询指定用户当前是否有生效禁言"),
                definition("chat.userBan.unban", "解除聊天禁言", "解除指定用户当前生效的聊天禁言"),
                definition("system.operationLog.list", "查询操作日志", "查询后台操作日志")
        ));
        AgentToolCall call = call(tool, "禁言", 10);

        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("2 个"));
        assertEquals(1, result.getArtifacts().size());
        assertFalse(result.getNextActions().isEmpty());

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentToolSearch", artifact.getType());
        assertEquals("禁言", artifact.getData().get("query"));
        assertEquals(2, artifact.getData().get("resultCount"));
        List<?> results = (List<?>) artifact.getData().get("results");
        assertEquals("chat.userBan.active", ((Map<?, ?>) results.get(0)).get("name"));
        assertEquals("chat.userBan.unban", ((Map<?, ?>) results.get(1)).get("name"));
    }

    @Test
    void shouldRespectLimit() {
        AgentToolSearchAgentTool tool = new AgentToolSearchAgentTool(catalogService(
                definition("tool.alpha", "Alpha 工具", "通用测试工具"),
                definition("tool.beta", "Beta 工具", "通用测试工具"),
                definition("tool.gamma", "Gamma 工具", "通用测试工具")
        ));

        AgentToolResult result = tool.execute(call(tool, "工具", 2), null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals(3, artifact.getData().get("totalMatchedCount"));
        assertEquals(2, artifact.getData().get("resultCount"));
    }

    @Test
    void shouldReturnEmptyResultWhenNoToolMatches() {
        AgentToolSearchAgentTool tool = new AgentToolSearchAgentTool(catalogService(
                definition("system.operationLog.list", "查询操作日志", "查询后台操作日志")
        ));

        AgentToolResult result = tool.execute(call(tool, "禁言", 10), null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("没有找到"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals(0, artifact.getData().get("resultCount"));
    }

    private AgentToolCall call(AgentToolSearchAgentTool tool, String query, int limit) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of("query", query, "limit", limit));
        return call;
    }

    private AgentToolCatalogService catalogService(AgentToolDefinition... definitions) {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(definitions));
        return catalogService;
    }

    private AgentToolDefinition definition(String name, String title, String description) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle(title);
        definition.setDescription(description);
        definition.setIntent(name);
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of("userId", Map.of("type", "integer", "minimum", 1)));
        definition.setRequiredInputKeys(List.of("userId"));
        definition.setRequiredPermissions(List.of("agent:test:read"));
        return definition;
    }
}
