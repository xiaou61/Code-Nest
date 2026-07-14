package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentSessionProperties;
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

class AgentRuntimeStatusAgentToolTest {

    @Test
    void shouldResolveRuntimeStatusQuestion() {
        AgentRuntimeStatusAgentTool tool = new AgentRuntimeStatusAgentTool(
                mock(AgentToolCatalogService.class),
                new AgentSessionProperties()
        );

        AgentToolCall call = tool.resolve("智能体现在状态怎么样").orElseThrow();

        assertEquals("system.agent.runtime.status", call.getToolName());
        assertTrue(call.getInput().isEmpty());
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldNotResolveMoreSpecificRuntimeChecks() {
        AgentRuntimeStatusAgentTool tool = new AgentRuntimeStatusAgentTool(
                mock(AgentToolCatalogService.class),
                new AgentSessionProperties()
        );

        assertTrue(tool.resolve("智能体工具调用健康和告警情况怎么样").isEmpty());
        assertTrue(tool.resolve("智能体上线前就绪状态自检").isEmpty());
    }

    @Test
    void shouldReturnRuntimeStatusArtifact() {
        AgentToolCatalogService catalogService = mock(AgentToolCatalogService.class);
        when(catalogService.definitions()).thenReturn(List.of(
                definition("system.agent.tools.list", "readonly", "READONLY", false),
                definition("chat.userBan.unban", "medium", "WRITE", false),
                definition("system.operationLog.cleanExpired", "high", "DESTRUCTIVE", true)
        ));
        AgentSessionProperties properties = new AgentSessionProperties();
        properties.setRepository("redis");
        properties.setMaxRecentTurns(8);
        properties.setRedisKeyPrefix("test:agent");
        properties.setTtlSeconds(120L);
        AgentRuntimeStatusAgentTool tool = new AgentRuntimeStatusAgentTool(catalogService, properties);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());
        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("已注册 3 个后端工具"));
        assertEquals(1, result.getArtifacts().size());
        assertFalse(result.getNextActions().isEmpty());

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentRuntimeStatus", artifact.getType());
        assertEquals("UP", artifact.getData().get("status"));
        assertEquals(3, artifact.getData().get("toolCount"));
        assertEquals(1, artifact.getData().get("readonlyToolCount"));
        assertEquals(2, artifact.getData().get("writeRiskToolCount"));
        assertEquals(1, artifact.getData().get("destructiveToolCount"));
        assertEquals(Map.of(
                "repository", "redis",
                "maxRecentTurns", 8,
                "ttlSeconds", 120L,
                "redisKeyPrefix", "test:agent"
        ), artifact.getData().get("session"));
    }

    private AgentToolDefinition definition(String name, String riskLevel, String riskCategory, boolean destructive) {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName(name);
        definition.setTitle(name);
        definition.setDescription("测试工具");
        definition.setIntent(name);
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel(riskLevel);
        definition.setRiskCategory(riskCategory);
        definition.setDestructive(destructive);
        definition.setRequiredPermissions(List.of("agent:test"));
        return definition;
    }
}
