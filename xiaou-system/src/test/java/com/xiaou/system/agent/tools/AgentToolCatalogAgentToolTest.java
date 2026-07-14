package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolCatalogService;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentToolCatalogAgentToolTest {

    @Test
    void shouldResolveCapabilityQuestion() {
        AgentToolCatalogAgentTool tool = new AgentToolCatalogAgentTool(mock(AgentToolCatalogService.class));

        assertTrue(tool.resolve("你能做什么").isPresent());
        assertTrue(tool.resolve("有哪些工具可以用").isPresent());
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldReturnRegisteredToolDescriptors() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(definition("system.operationLog.list")));
        AgentToolCatalogAgentTool tool = new AgentToolCatalogAgentTool(catalogService);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());
        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("已注册 1 个工具"));
        assertEquals(1, result.getArtifacts().size());
        assertFalse(result.getNextActions().isEmpty());
    }

    private AgentToolDefinition definition(String name) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle("查询操作日志");
        definition.setDescription("分页查询最近操作日志");
        definition.setIntent(name);
        definition.setRoute("/logs/operation");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of("pageSize", Map.of("type", "integer")));
        definition.setRequiredInputKeys(List.of("pageSize"));
        definition.setRequiredPermissions(List.of("agent:system:operation-log:read"));
        return definition;
    }
}
