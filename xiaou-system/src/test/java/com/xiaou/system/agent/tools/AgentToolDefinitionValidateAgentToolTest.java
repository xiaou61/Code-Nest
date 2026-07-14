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

class AgentToolDefinitionValidateAgentToolTest {

    @Test
    void shouldResolveToolDefinitionValidationQuestion() {
        AgentToolDefinitionValidateAgentTool tool = new AgentToolDefinitionValidateAgentTool(mock(AgentToolCatalogService.class));

        AgentToolCall call = tool.resolve("检查一下智能体工具定义是否合规").orElseThrow();

        assertEquals("system.agent.tools.validate", call.getToolName());
        assertTrue(call.getInput().isEmpty());
        assertTrue(tool.resolve("你能做什么，有哪些工具").isEmpty());
    }

    @Test
    void shouldReportPassWhenDefinitionsAreValid() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(readonlyTool("system.operationLog.list")));
        AgentToolDefinitionValidateAgentTool tool = new AgentToolDefinitionValidateAgentTool(catalogService);

        AgentToolResult result = tool.execute(call(tool, Map.of()), null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("全部通过"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentToolDefinitionValidation", artifact.getType());
        assertEquals("PASS", artifact.getData().get("status"));
        assertEquals(1, artifact.getData().get("totalCount"));
        assertEquals(0, artifact.getData().get("issueCount"));
    }

    @Test
    void shouldReportContractIssuesForInvalidDefinitions() {
        AgentToolDefinition invalid = readonlyTool("system.invalid");
        invalid.setRoute("admin/agent/chat");
        invalid.setRequiredInputKeys(List.of("missingField"));
        invalid.setRequiredPermissions(List.of("system:legacy:read"));
        invalid.setConfirmationRequired(true);

        AgentToolDefinition writeWithoutConfirmation = readonlyTool("system.write");
        writeWithoutConfirmation.setRiskLevel("medium");
        writeWithoutConfirmation.setRiskCategory("WRITE");
        writeWithoutConfirmation.setConfirmationRequired(false);
        writeWithoutConfirmation.setConfirmationText("");

        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(invalid, writeWithoutConfirmation));
        AgentToolDefinitionValidateAgentTool tool = new AgentToolDefinitionValidateAgentTool(catalogService);

        AgentToolResult result = tool.execute(call(tool, Map.of()), null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("发现"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("FAILED", artifact.getData().get("status"));
        assertEquals(2, artifact.getData().get("totalCount"));
        assertTrue(((Number) artifact.getData().get("issueCount")).intValue() >= 4);
        String issues = String.valueOf(artifact.getData().get("issues"));
        assertTrue(issues.contains("route"));
        assertTrue(issues.contains("missingField"));
        assertTrue(issues.contains("agent:*"));
        assertTrue(issues.contains("confirmation"));
    }

    @Test
    void shouldDetectDuplicateToolNamesAndFilterByToolName() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        AgentToolDefinition first = readonlyTool("system.agent.tools.list");
        AgentToolDefinition duplicate = readonlyTool("system.agent.tools.list");
        AgentToolDefinition other = readonlyTool("system.operationLog.list");
        when(catalogService.definitions()).thenReturn(List.of(first, duplicate, other));
        AgentToolDefinitionValidateAgentTool tool = new AgentToolDefinitionValidateAgentTool(catalogService);

        AgentToolResult duplicateResult = tool.execute(call(tool, Map.of()), null);
        AgentToolResult filteredResult = tool.execute(call(tool, Map.of("toolName", "system.operationLog.list")), null);

        assertEquals("FAILED", duplicateResult.getArtifacts().get(0).getData().get("status"));
        assertTrue(String.valueOf(duplicateResult.getArtifacts().get(0).getData().get("issues")).contains("duplicate"));
        assertEquals("PASS", filteredResult.getArtifacts().get(0).getData().get("status"));
        assertEquals(1, filteredResult.getArtifacts().get(0).getData().get("totalCount"));
    }

    @Test
    void shouldReturnErrorWhenFilteredToolDoesNotExist() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(readonlyTool("system.operationLog.list")));
        AgentToolDefinitionValidateAgentTool tool = new AgentToolDefinitionValidateAgentTool(catalogService);

        AgentToolResult result = tool.execute(call(tool, Map.of("toolName", "system.missing")), null);

        assertFalse(result.isSuccess());
        assertTrue(result.getSummary().contains("没有找到"));
    }

    private AgentToolCall call(AgentToolDefinitionValidateAgentTool tool, Map<String, Object> input) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(input);
        return call;
    }

    private AgentToolDefinition readonlyTool(String name) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle("查询操作日志");
        definition.setDescription("分页查询系统操作日志");
        definition.setIntent(name);
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of("pageSize", Map.of("type", "integer", "minimum", 1)));
        definition.setRequiredInputKeys(List.of("pageSize"));
        definition.setRequiredPermissions(List.of("agent:system:operation-log:read"));
        return definition;
    }
}
