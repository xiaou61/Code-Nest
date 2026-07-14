package com.xiaou.system.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.system.domain.SysAgentSessionContext;
import com.xiaou.system.mapper.SysAgentSessionContextMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DbAgentSessionContextRepositoryTest {

    private static final TypeReference<List<AgentSessionTurn>> TURN_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReadSnapshotFromDatabase() throws Exception {
        SysAgentSessionContextMapper mapper = mock(SysAgentSessionContextMapper.class);
        when(mapper.selectBySessionId("session-1")).thenReturn(record(List.of(turn("message-1", "tool-1"))));
        DbAgentSessionContextRepository repository = new DbAgentSessionContextRepository(objectMapper, mapper);

        AgentSessionSnapshot snapshot = repository.snapshot("session-1");

        assertEquals("session-1", snapshot.getSessionId());
        assertEquals(1, snapshot.getRecentTurns().size());
        assertEquals("message-1", snapshot.getRecentTurns().get(0).getMessage());
        assertEquals("tool-1", snapshot.getRecentTurns().get(0).getToolName());
    }

    @Test
    void shouldAppendTurnAndKeepRecentWindow() throws Exception {
        SysAgentSessionContextMapper mapper = mock(SysAgentSessionContextMapper.class);
        when(mapper.selectBySessionId("session-1")).thenReturn(record(List.of(
                turn("message-1", "tool-1"),
                turn("message-2", "tool-2")
        )));
        DbAgentSessionContextRepository repository = new DbAgentSessionContextRepository(objectMapper, mapper);

        repository.appendTurn("session-1", turn("message-3", "tool-3"), 2);

        ArgumentCaptor<SysAgentSessionContext> captor = ArgumentCaptor.forClass(SysAgentSessionContext.class);
        verify(mapper).upsert(captor.capture());
        SysAgentSessionContext saved = captor.getValue();
        List<AgentSessionTurn> turns = objectMapper.readValue(saved.getTurnsJson(), TURN_LIST_TYPE);
        assertEquals("session-1", saved.getSessionId());
        assertEquals(2, turns.size());
        assertEquals("message-2", turns.get(0).getMessage());
        assertEquals("message-3", turns.get(1).getMessage());
        assertNotNull(saved.getCreatedTime());
        assertNotNull(saved.getUpdatedTime());
    }

    @Test
    void shouldReturnEmptySnapshotWhenStoredJsonIsInvalid() {
        SysAgentSessionContextMapper mapper = mock(SysAgentSessionContextMapper.class);
        SysAgentSessionContext record = new SysAgentSessionContext();
        record.setSessionId("session-1");
        record.setTurnsJson("{bad-json");
        when(mapper.selectBySessionId("session-1")).thenReturn(record);
        DbAgentSessionContextRepository repository = new DbAgentSessionContextRepository(objectMapper, mapper);

        AgentSessionSnapshot snapshot = repository.snapshot("session-1");

        assertEquals("session-1", snapshot.getSessionId());
        assertTrue(snapshot.getRecentTurns().isEmpty());
    }

    @Test
    void shouldIgnoreBlankSessionId() {
        SysAgentSessionContextMapper mapper = mock(SysAgentSessionContextMapper.class);
        DbAgentSessionContextRepository repository = new DbAgentSessionContextRepository(objectMapper, mapper);

        repository.appendTurn(" ", turn("message-1", "tool-1"), 6);

        assertTrue(repository.snapshot(" ").getRecentTurns().isEmpty());
        verify(mapper, never()).upsert(org.mockito.ArgumentMatchers.any());
        verify(mapper, never()).selectBySessionId(org.mockito.ArgumentMatchers.any());
    }

    private SysAgentSessionContext record(List<AgentSessionTurn> turns) throws Exception {
        SysAgentSessionContext record = new SysAgentSessionContext();
        record.setSessionId("session-1");
        record.setTurnsJson(objectMapper.writeValueAsString(turns));
        return record;
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
