package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentSessionProperties;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolCatalogService;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRuntimeReadinessAgentToolTest {

    @Test
    void shouldResolveRuntimeReadinessQuestion() {
        AgentRuntimeReadinessAgentTool tool = new AgentRuntimeReadinessAgentTool(
                mock(AgentToolCatalogService.class),
                new AgentSessionProperties()
        );

        AgentToolCall call = tool.resolve("帮我做一下智能体运行时上线 readiness 自检").orElseThrow();

        assertEquals("system.agent.runtime.readiness", call.getToolName());
        assertTrue(call.getInput().isEmpty());
        assertTrue(tool.resolve("智能体现在状态怎么样").isEmpty());
    }

    @Test
    void shouldReportReadyWhenCoreRuntimeIsComplete() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(completeCoreDefinitions());
        AgentSessionProperties properties = new AgentSessionProperties();
        properties.setRepository("db");
        AgentRuntimeReadinessAgentTool tool = new AgentRuntimeReadinessAgentTool(catalogService, properties);

        AgentToolResult result = tool.execute(call(tool), null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("READY"));
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentRuntimeReadiness", artifact.getType());
        assertEquals("READY", artifact.getData().get("status"));
        assertEquals(0, artifact.getData().get("blockedCount"));
        assertEquals(0, artifact.getData().get("warnCount"));
        assertTrue(String.valueOf(artifact.getData().get("checks")).contains("coreTools.present"));
    }

    @Test
    void shouldBlockWhenCoreToolsAreMissing() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(readonlyTool("system.agent.tools.list")));
        AgentRuntimeReadinessAgentTool tool = new AgentRuntimeReadinessAgentTool(catalogService, new AgentSessionProperties());

        AgentToolResult result = tool.execute(call(tool), null);

        assertTrue(result.isSuccess());
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("BLOCKED", artifact.getData().get("status"));
        assertTrue(((Number) artifact.getData().get("blockedCount")).intValue() >= 1);
        assertTrue(result.getNextActions().contains("先补齐 BLOCKED 检查项，再继续扩展业务工具。"));
    }

    @Test
    void shouldWarnForInvalidSessionConfigurationWithoutBlockingToolCatalog() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(completeCoreDefinitions());
        AgentSessionProperties properties = new AgentSessionProperties();
        properties.setRepository("filesystem");
        properties.setMaxRecentTurns(0);
        AgentRuntimeReadinessAgentTool tool = new AgentRuntimeReadinessAgentTool(catalogService, properties);

        AgentToolResult result = tool.execute(call(tool), null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("WARN", artifact.getData().get("status"));
        assertEquals(0, artifact.getData().get("blockedCount"));
        assertTrue(((Number) artifact.getData().get("warnCount")).intValue() >= 1);
        assertTrue(String.valueOf(artifact.getData().get("checks")).contains("session.repository"));
        assertTrue(result.getNextActions().contains("修复 WARN 检查项后再做上线验收。"));
    }

    @Test
    void shouldBlockInvalidWriteToolWithoutConfirmation() {
        List<AgentToolDefinition> definitions = new ArrayList<>(completeCoreDefinitions());
        AgentToolDefinition writeTool = readonlyTool("chat.userBan.unban");
        writeTool.setRiskLevel("medium");
        writeTool.setRiskCategory("WRITE");
        writeTool.setRequiredPermissions(List.of("agent:chat:user-ban:write"));
        writeTool.setConfirmationRequired(false);
        definitions.add(writeTool);

        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(definitions);
        AgentRuntimeReadinessAgentTool tool = new AgentRuntimeReadinessAgentTool(catalogService, new AgentSessionProperties());

        AgentToolResult result = tool.execute(call(tool), null);

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("BLOCKED", artifact.getData().get("status"));
        assertTrue(String.valueOf(artifact.getData().get("checks")).contains("toolDefinitions.writeConfirmation"));
    }

    private AgentToolCall call(AgentRuntimeReadinessAgentTool tool) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());
        return call;
    }

    private List<AgentToolDefinition> completeCoreDefinitions() {
        return List.of(
                readonlyTool("system.agent.tools.list"),
                readonlyTool("system.agent.tools.detail"),
                readonlyTool("system.agent.tools.search"),
                readonlyTool("system.agent.tools.validate"),
                readonlyTool("system.agent.tools.access_check"),
                readonlyTool("system.agent.runtime.status"),
                readonlyTool("system.agent.runtime.readiness"),
                readonlyTool("system.agent.runtime.observability"),
                readonlyTool("system.agent.session.context"),
                readonlyTool("system.agent.operator.self"),
                readonlyTool("system.agent.planner.diagnostics"),
                readonlyTool("system.agent.planner.dry_run"),
                readonlyTool("system.agent.request.dry_run"),
                readonlyTool("system.agent.policy.dry_run"),
                readonlyTool("system.agent.metrics.summary"),
                readonlyTool("system.agent.metrics.health"),
                readonlyTool("system.agent.metrics.alerts"),
                readonlyTool("system.agent.audit.list"),
                readonlyTool("system.agent.audit.detail"),
                readonlyTool("system.agent.recovery.explain"),
                readonlyTool("system.agent.recovery.dry_run")
        );
    }

    private AgentToolDefinition readonlyTool(String name) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle(name);
        definition.setDescription("测试工具");
        definition.setIntent(name);
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:status:read"));
        return definition;
    }
}
