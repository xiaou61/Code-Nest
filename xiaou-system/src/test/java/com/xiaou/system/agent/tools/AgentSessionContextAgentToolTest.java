package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentRuntimeTrace;
import com.xiaou.system.agent.AgentSessionProperties;
import com.xiaou.system.agent.AgentSessionSnapshot;
import com.xiaou.system.agent.AgentSessionTurn;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSessionContextAgentToolTest {

    @Test
    void shouldResolveCurrentSessionContextQuestion() {
        AgentSessionContextAgentTool tool = new AgentSessionContextAgentTool(new AgentSessionProperties());

        AgentToolCall call = tool.resolve("你现在记住了什么").orElseThrow();

        assertEquals("system.agent.session.context", call.getToolName());
        assertTrue(call.getInput().isEmpty());
        assertTrue(tool.resolve("查询操作日志").isEmpty());
    }

    @Test
    void shouldReturnCurrentSessionSnapshotArtifact() {
        AgentSessionProperties properties = new AgentSessionProperties();
        properties.setMaxRecentTurns(8);
        AgentSessionContextAgentTool tool = new AgentSessionContextAgentTool(properties);
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());

        AgentToolResult result = tool.execute(call, context(snapshot("session-1", List.of(
                turn("查最近3条操作日志", "answered", "已查询到 3 条", "system.operationLog.list", "", "trace-1"),
                turn("解除用户88禁言", "confirm_required", "请输入强确认", "chat.userBan.unban", "audit-1", "trace-2")
        ))));

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("已保留 2 轮"));
        assertEquals(1, result.getArtifacts().size());
        assertFalse(result.getNextActions().isEmpty());

        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("agentSessionContext", artifact.getType());
        assertEquals("session-1", artifact.getData().get("sessionId"));
        assertEquals(2, artifact.getData().get("recentTurnCount"));
        assertEquals(8, artifact.getData().get("maxRecentTurns"));
        List<?> turns = (List<?>) artifact.getData().get("recentTurns");
        assertEquals(2, turns.size());
        assertEquals("system.operationLog.list", ((Map<?, ?>) turns.get(0)).get("toolName"));
        assertEquals("audit-1", ((Map<?, ?>) turns.get(1)).get("auditId"));
    }

    @Test
    void shouldHandleMissingContextAsEmptySession() {
        AgentSessionContextAgentTool tool = new AgentSessionContextAgentTool(new AgentSessionProperties());
        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setInput(Map.of());

        AgentToolResult result = tool.execute(call, null);

        assertTrue(result.isSuccess());
        AgentChatArtifact artifact = result.getArtifacts().get(0);
        assertEquals("", artifact.getData().get("sessionId"));
        assertEquals(0, artifact.getData().get("recentTurnCount"));
    }

    private AgentExecutionContext context(AgentSessionSnapshot snapshot) {
        return new AgentExecutionContext("session-1", "你现在记住了什么", null, AgentRuntimeTrace.start(), snapshot);
    }

    private AgentSessionSnapshot snapshot(String sessionId, List<AgentSessionTurn> turns) {
        AgentSessionSnapshot snapshot = new AgentSessionSnapshot();
        snapshot.setSessionId(sessionId);
        snapshot.setRecentTurns(turns);
        return snapshot;
    }

    private AgentSessionTurn turn(String message, String status, String answer, String toolName, String auditId, String traceId) {
        AgentSessionTurn turn = new AgentSessionTurn();
        turn.setMessage(message);
        turn.setStatus(status);
        turn.setAnswer(answer);
        turn.setToolName(toolName);
        turn.setAuditId(auditId);
        turn.setTraceId(traceId);
        return turn;
    }
}
