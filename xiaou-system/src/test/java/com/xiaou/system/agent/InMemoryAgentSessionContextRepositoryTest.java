package com.xiaou.system.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAgentSessionContextRepositoryTest {

    @Test
    void shouldAppendTurnsAndReturnSnapshotBySessionId() {
        InMemoryAgentSessionContextRepository repository = new InMemoryAgentSessionContextRepository();

        repository.appendTurn("session-1", turn("message-1", "tool-1"), 6);

        AgentSessionSnapshot snapshot = repository.snapshot("session-1");

        assertEquals("session-1", snapshot.getSessionId());
        assertEquals(1, snapshot.getRecentTurns().size());
        assertEquals("message-1", snapshot.getRecentTurns().get(0).getMessage());
        assertEquals("tool-1", snapshot.getRecentTurns().get(0).getToolName());
    }

    @Test
    void shouldKeepOnlyConfiguredRecentTurns() {
        InMemoryAgentSessionContextRepository repository = new InMemoryAgentSessionContextRepository();

        for (int i = 1; i <= 5; i++) {
            repository.appendTurn("session-1", turn("message-" + i, "tool-" + i), 3);
        }

        AgentSessionSnapshot snapshot = repository.snapshot("session-1");

        assertEquals(3, snapshot.getRecentTurns().size());
        assertEquals("message-3", snapshot.getRecentTurns().get(0).getMessage());
        assertEquals("message-5", snapshot.getRecentTurns().get(2).getMessage());
    }

    @Test
    void shouldReturnDefensiveSnapshotCopy() {
        InMemoryAgentSessionContextRepository repository = new InMemoryAgentSessionContextRepository();
        repository.appendTurn("session-1", turn("message-1", "tool-1"), 6);

        AgentSessionSnapshot snapshot = repository.snapshot("session-1");
        snapshot.getRecentTurns().clear();

        assertEquals(1, repository.snapshot("session-1").getRecentTurns().size());
    }

    @Test
    void shouldIgnoreBlankSessionId() {
        InMemoryAgentSessionContextRepository repository = new InMemoryAgentSessionContextRepository();

        repository.appendTurn(" ", turn("message-1", "tool-1"), 6);

        assertTrue(repository.snapshot(" ").getRecentTurns().isEmpty());
    }

    private AgentSessionTurn turn(String message, String toolName) {
        AgentSessionTurn turn = new AgentSessionTurn();
        turn.setMessage(message);
        turn.setStatus("answered");
        turn.setToolName(toolName);
        turn.setAnswer("answer");
        turn.setTraceId("agent-trace-test");
        return turn;
    }
}
