package com.xiaou.system.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisAgentSessionContextRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAppendTurnToRedisAndKeepRecentWindow() throws Exception {
        RedisFixture fixture = redisFixture();
        AgentSessionProperties properties = properties();
        RedisAgentSessionContextRepository repository = repository(fixture, properties);
        when(fixture.valueOperations.get("xiaou:admin:agent:session:session-1"))
                .thenReturn(objectMapper.writeValueAsString(List.of(
                        turn("message-1", "tool-1"),
                        turn("message-2", "tool-2")
                )));

        repository.appendTurn("session-1", turn("message-3", "tool-3"), 2);

        verify(fixture.valueOperations).set(eq("xiaou:admin:agent:session:session-1"),
                org.mockito.ArgumentMatchers.argThat(json ->
                        json.contains("message-2")
                                && json.contains("message-3")
                                && !json.contains("message-1")));
        verify(fixture.redisTemplate).expire("xiaou:admin:agent:session:session-1", Duration.ofSeconds(3600));
    }

    @Test
    void shouldReadSnapshotFromRedis() throws Exception {
        RedisFixture fixture = redisFixture();
        RedisAgentSessionContextRepository repository = repository(fixture, properties());
        when(fixture.valueOperations.get("xiaou:admin:agent:session:session-1"))
                .thenReturn(objectMapper.writeValueAsString(List.of(turn("message-1", "tool-1"))));

        AgentSessionSnapshot snapshot = repository.snapshot("session-1");

        assertEquals("session-1", snapshot.getSessionId());
        assertEquals(1, snapshot.getRecentTurns().size());
        assertEquals("message-1", snapshot.getRecentTurns().get(0).getMessage());
        assertEquals("tool-1", snapshot.getRecentTurns().get(0).getToolName());
    }

    @Test
    void shouldReturnEmptySnapshotWhenRedisIsUnavailable() {
        RedisAgentSessionContextRepository repository = new RedisAgentSessionContextRepository(
                objectMapper,
                properties(),
                new StaticListableBeanFactory().getBeanProvider(StringRedisTemplate.class)
        );

        AgentSessionSnapshot snapshot = repository.snapshot("session-1");

        assertEquals("session-1", snapshot.getSessionId());
        assertTrue(snapshot.getRecentTurns().isEmpty());
    }

    @Test
    void shouldSkipExpireWhenTtlIsDisabled() {
        RedisFixture fixture = redisFixture();
        AgentSessionProperties properties = properties();
        properties.setTtlSeconds(0);
        RedisAgentSessionContextRepository repository = repository(fixture, properties);

        repository.appendTurn("session-1", turn("message-1", "tool-1"), 6);

        verify(fixture.valueOperations).set(eq("xiaou:admin:agent:session:session-1"), anyString());
        verify(fixture.redisTemplate, never()).expire(eq("xiaou:admin:agent:session:session-1"), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    private RedisAgentSessionContextRepository repository(RedisFixture fixture, AgentSessionProperties properties) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("stringRedisTemplate", fixture.redisTemplate);
        return new RedisAgentSessionContextRepository(
                objectMapper,
                properties,
                beanFactory.getBeanProvider(StringRedisTemplate.class)
        );
    }

    @SuppressWarnings("unchecked")
    private RedisFixture redisFixture() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return new RedisFixture(redisTemplate, valueOperations);
    }

    private AgentSessionProperties properties() {
        AgentSessionProperties properties = new AgentSessionProperties();
        properties.setRedisKeyPrefix("xiaou:admin:agent:session");
        properties.setTtlSeconds(3600);
        return properties;
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

    private record RedisFixture(StringRedisTemplate redisTemplate, ValueOperations<String, String> valueOperations) {
    }
}
