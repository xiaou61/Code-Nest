package com.xiaou.ai.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisAiRuntimeMetricsPersistenceTest {

    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AiProperties aiProperties;
    private RedisAiRuntimeMetricsPersistence persistence;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        aiProperties = new AiProperties();
        aiProperties.getMetrics().getPersistence().setRedisKey("test:ai:metrics");
        persistence = new RedisAiRuntimeMetricsPersistence(stringRedisTemplate, new ObjectMapper(), aiProperties);
    }

    @Test
    void shouldSaveMetricsStateToRedis() {
        AiRuntimeMetricsStoreState state = new AiRuntimeMetricsStoreState();
        state.getRecentCalls().add(new AiRuntimeRecentCall()
                .setScene("community_summary")
                .setModelName("gpt-5.4")
                .setOutcome("success")
                .setEstimatedCost(new BigDecimal("0.00020000"))
                .setTimestamp(System.currentTimeMillis()));

        persistence.save(state);

        verify(valueOperations).set(eq("test:ai:metrics"), anyString());
    }

    @Test
    void shouldLoadMetricsStateFromRedis() throws Exception {
        AiRuntimeMetricsStoreState state = new AiRuntimeMetricsStoreState();
        state.getSceneStates().put("community_summary|community.summary|v1", new AiRuntimeSceneStoreState()
                .setScene("community_summary")
                .setPromptKey("community.summary")
                .setPromptVersion("v1"));
        state.getRecentCalls().add(new AiRuntimeRecentCall()
                .setScene("community_summary")
                .setModelName("gpt-5.4")
                .setOutcome("success")
                .setTimestamp(System.currentTimeMillis()));

        String json = new ObjectMapper().writeValueAsString(state);
        when(valueOperations.get("test:ai:metrics")).thenReturn(json);

        AiRuntimeMetricsStoreState loaded = persistence.load();

        assertNotNull(loaded);
        assertEquals(1, loaded.getSceneStates().size());
        assertEquals(1, loaded.getRecentCalls().size());
        assertEquals("gpt-5.4", loaded.getRecentCalls().get(0).getModelName());
    }

    @Test
    void shouldReturnNullWhenRedisStateMissing() {
        when(valueOperations.get("test:ai:metrics")).thenReturn(null);

        AiRuntimeMetricsStoreState loaded = persistence.load();

        assertNull(loaded);
    }

    @Test
    void shouldClearMetricsStateFromRedis() {
        persistence.clear();

        verify(stringRedisTemplate).delete("test:ai:metrics");
    }
}
