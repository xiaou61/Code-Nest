package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentOperatorSelfAgentToolTest {

    @Test
    void shouldResolveCurrentOperatorQuestion() {
        AgentOperatorSelfAgentTool tool = new AgentOperatorSelfAgentTool();

        AgentToolCall call = tool.resolve("我在智能体里的角色和权限是什么").orElseThrow();

        assertEquals("system.agent.operator.self", call.getToolName());
        assertTrue(call.getInput().isEmpty());
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldReturnCurrentOperatorArtifact() {
        AgentOperatorSelfAgentTool tool = new AgentOperatorSelfAgentTool();
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());

        AgentOperator operator = new AgentOperator(
                7L,
                "Alice",
                "tenant-a",
                List.of("SUPER_ADMIN", "OPS", "SUPER_ADMIN"),
                List.of("agent:runtime:operator:read", "agent:chat:user-ban:read", "*")
        );
        AgentToolResult result = tool.execute(call, new AgentExecutionContext("session-1", "我的权限", operator));

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("Alice"));
        assertTrue(result.getSummary().contains("2 个角色"));
        assertEquals(1, result.getArtifacts().size());
        assertFalse(result.getNextActions().isEmpty());

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentOperatorSelf", artifact.getType());
        assertEquals(7L, artifact.getData().get("operatorId"));
        assertEquals("Alice", artifact.getData().get("operatorName"));
        assertEquals("tenant-a", artifact.getData().get("tenantId"));
        assertEquals(2, artifact.getData().get("roleCount"));
        assertEquals(3, artifact.getData().get("permissionCount"));
        assertEquals(List.of("OPS", "SUPER_ADMIN"), artifact.getData().get("roles"));
        assertEquals(List.of("*", "agent:chat:user-ban:read", "agent:runtime:operator:read"),
                artifact.getData().get("permissions"));
        assertEquals(true, artifact.getData().get("hasWildcardPermission"));
        assertEquals(true, artifact.getData().get("selfOnly"));
    }

    @Test
    void shouldHandleMissingOperatorAsEmptyRuntimeContext() {
        AgentOperatorSelfAgentTool tool = new AgentOperatorSelfAgentTool();
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());

        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertNull(artifact.getData().get("operatorId"));
        assertEquals("", artifact.getData().get("operatorName"));
        assertEquals("", artifact.getData().get("tenantId"));
        assertEquals(0, artifact.getData().get("roleCount"));
        assertEquals(0, artifact.getData().get("permissionCount"));
    }
}
