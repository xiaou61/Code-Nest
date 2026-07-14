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

class AgentPlannerDiagnosticsAgentToolTest {

    @Test
    void shouldResolvePlannerDiagnosticsQuestion() {
        AgentPlannerDiagnosticsAgentTool tool = new AgentPlannerDiagnosticsAgentTool(catalogService());

        AgentToolCall call = tool.resolve("planner 结构化输出契约和命中规则是什么").orElseThrow();

        assertEquals("system.agent.planner.diagnostics", call.getToolName());
        assertTrue(call.getInput().isEmpty());
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldReturnPlannerDiagnosticsArtifact() {
        AgentPlannerDiagnosticsAgentTool tool = new AgentPlannerDiagnosticsAgentTool(catalogService());
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());

        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("2 个"));
        assertEquals(1, result.getArtifacts().size());
        assertFalse(result.getNextActions().isEmpty());

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentPlannerDiagnostics", artifact.getType());
        assertEquals(2, artifact.getData().get("registeredToolCount"));
        assertEquals(List.of("chat.userBan.unban", "system.agent.tools.list"), artifact.getData().get("registeredToolNames"));
        assertEquals("admin_agent.plan", ((Map<?, ?>) artifact.getData().get("prompt")).get("key"));
        assertEquals("v1", ((Map<?, ?>) artifact.getData().get("prompt")).get("version"));
        assertEquals(List.of("toolName", "input", "confidence", "missingFields"),
                ((Map<?, ?>) artifact.getData().get("structuredOutput")).get("requiredFields"));
        assertEquals(0.6D, artifact.getData().get("minimumConfidence"));
    }

    private AgentToolCatalogService catalogService() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(
                definition("system.agent.tools.list"),
                definition("chat.userBan.unban")
        ));
        return catalogService;
    }

    private AgentToolDefinition definition(String name) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle(name);
        definition.setDescription(name);
        definition.setIntent(name);
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:test:read"));
        return definition;
    }
}
