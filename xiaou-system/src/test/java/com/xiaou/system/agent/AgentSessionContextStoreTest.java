package com.xiaou.system.agent;

import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSessionContextStoreTest {

    @Test
    void shouldRecordRecentTurnsBySessionId() {
        AgentSessionContextStore store = new AgentSessionContextStore();

        store.record(request("session-1", "查最近3条操作日志"),
                response("answered", "system.operationLog.list", "已查询到最近 3 条操作日志。"));

        AgentSessionSnapshot snapshot = store.snapshot("session-1");

        assertEquals("session-1", snapshot.getSessionId());
        assertEquals(1, snapshot.getRecentTurns().size());
        assertEquals("查最近3条操作日志", snapshot.getRecentTurns().get(0).getMessage());
        assertEquals("answered", snapshot.getRecentTurns().get(0).getStatus());
        assertEquals("system.operationLog.list", snapshot.getRecentTurns().get(0).getToolName());
    }

    @Test
    void shouldKeepSessionsIsolated() {
        AgentSessionContextStore store = new AgentSessionContextStore();

        store.record(request("session-a", "查日志"), response("answered", "system.operationLog.list", "日志结果"));
        store.record(request("session-b", "查禁言"), response("answered", "chat.userBan.active", "禁言结果"));

        assertEquals("system.operationLog.list", store.snapshot("session-a").getRecentTurns().get(0).getToolName());
        assertEquals("chat.userBan.active", store.snapshot("session-b").getRecentTurns().get(0).getToolName());
    }

    @Test
    void shouldScopeSameSessionIdByOperator() {
        AgentSessionContextStore store = new AgentSessionContextStore();

        store.record(request("shared-session", "管理员一的请求"),
                response("answered", "system.operationLog.list", "管理员一的结果"), 100L);
        store.record(request("shared-session", "管理员二的请求"),
                response("answered", "chat.userBan.active", "管理员二的结果"), 200L);

        AgentSessionSnapshot firstOperator = store.snapshot("shared-session", 100L);
        AgentSessionSnapshot secondOperator = store.snapshot("shared-session", 200L);

        assertEquals("shared-session", firstOperator.getSessionId());
        assertEquals(1, firstOperator.getRecentTurns().size());
        assertEquals("system.operationLog.list", firstOperator.getRecentTurns().get(0).getToolName());
        assertEquals(1, secondOperator.getRecentTurns().size());
        assertEquals("chat.userBan.active", secondOperator.getRecentTurns().get(0).getToolName());
    }

    @Test
    void shouldIgnoreBlankSessionId() {
        AgentSessionContextStore store = new AgentSessionContextStore();

        store.record(request(" ", "查日志"), response("answered", "system.operationLog.list", "日志结果"));

        assertTrue(store.snapshot(" ").getRecentTurns().isEmpty());
    }

    @Test
    void shouldKeepOnlyRecentTurns() {
        AgentSessionContextStore store = new AgentSessionContextStore();

        for (int i = 1; i <= 8; i++) {
            store.record(request("session-1", "message-" + i), response("answered", "tool-" + i, "answer-" + i));
        }

        AgentSessionSnapshot snapshot = store.snapshot("session-1");

        assertEquals(6, snapshot.getRecentTurns().size());
        assertEquals("message-3", snapshot.getRecentTurns().get(0).getMessage());
        assertEquals("message-8", snapshot.getRecentTurns().get(5).getMessage());
    }

    @Test
    void shouldDelegateTurnsToReplaceableRepository() {
        CapturingSessionContextRepository repository = new CapturingSessionContextRepository();
        AgentSessionContextStore store = new AgentSessionContextStore(repository);

        store.record(request(" session-1 ", "查日志"), response("answered", "system.operationLog.list", "日志结果"));

        assertEquals("session-1", repository.sessionId);
        assertEquals(6, repository.maxRecentTurns);
        assertEquals("查日志", repository.turns.get(0).getMessage());
        assertEquals("system.operationLog.list", repository.turns.get(0).getToolName());
        assertEquals(1, store.snapshot("session-1").getRecentTurns().size());
    }

    private AgentChatRequest request(String sessionId, String message) {
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId(sessionId);
        request.setMessage(message);
        return request;
    }

    private AgentChatResponse response(String status, String toolName, String answer) {
        AgentChatResponse response = new AgentChatResponse();
        response.setStatus(status);
        response.setToolName(toolName);
        response.setAnswer(answer);
        response.setTraceId("agent-trace-test");
        return response;
    }

    private static class CapturingSessionContextRepository implements AgentSessionContextRepository {
        private String sessionId;
        private int maxRecentTurns;
        private final List<AgentSessionTurn> turns = new ArrayList<>();

        @Override
        public AgentSessionSnapshot snapshot(String sessionId) {
            AgentSessionSnapshot snapshot = new AgentSessionSnapshot();
            snapshot.setSessionId(sessionId);
            snapshot.setRecentTurns(new ArrayList<>(turns));
            return snapshot;
        }

        @Override
        public void appendTurn(String sessionId, AgentSessionTurn turn, int maxRecentTurns) {
            this.sessionId = sessionId;
            this.maxRecentTurns = maxRecentTurns;
            this.turns.add(turn);
        }
    }
}
