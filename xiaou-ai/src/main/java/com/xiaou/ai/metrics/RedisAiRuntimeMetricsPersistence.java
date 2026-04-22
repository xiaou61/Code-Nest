package com.xiaou.ai.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * 基于 Redis 的 AI 运行观测持久化实现。
 *
 * @author xiaou
 */
@Slf4j
public class RedisAiRuntimeMetricsPersistence implements AiRuntimeMetricsPersistence {

    private static final String DEFAULT_REDIS_KEY = "xiaou:ai:runtime:metrics";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    public RedisAiRuntimeMetricsPersistence(StringRedisTemplate stringRedisTemplate,
                                           ObjectMapper objectMapper,
                                           AiProperties aiProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
    }

    @Override
    public String mode() {
        return "redis";
    }

    @Override
    public AiRuntimeMetricsStoreState load() {
        String json = stringRedisTemplate.opsForValue().get(resolveRedisKey());
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AiRuntimeMetricsStoreState.class);
        } catch (Exception e) {
            throw new IllegalStateException("读取 AI 运行观测 Redis 状态失败", e);
        }
    }

    @Override
    public void save(AiRuntimeMetricsStoreState state) {
        if (state == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(resolveRedisKey(), objectMapper.writeValueAsString(state));
        } catch (Exception e) {
            throw new IllegalStateException("保存 AI 运行观测 Redis 状态失败", e);
        }
    }

    @Override
    public void clear() {
        stringRedisTemplate.delete(resolveRedisKey());
    }

    private String resolveRedisKey() {
        if (aiProperties.getMetrics() == null || aiProperties.getMetrics().getPersistence() == null) {
            return DEFAULT_REDIS_KEY;
        }
        String redisKey = aiProperties.getMetrics().getPersistence().getRedisKey();
        return StringUtils.hasText(redisKey) ? redisKey.trim() : DEFAULT_REDIS_KEY;
    }
}
