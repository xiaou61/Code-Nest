package com.xiaou.system.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.cache.TextStateStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 会话上下文实现。通过配置显式启用，默认仍使用内存实现。
 *
 * @author xiaou
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "xiaou.admin-agent.session", name = "repository", havingValue = "redis")
public class RedisAgentSessionContextRepository implements AgentSessionContextRepository {

    private static final TypeReference<List<AgentSessionTurn>> TURN_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final AgentSessionProperties properties;
    private final TextStateStore stateStore;

    public RedisAgentSessionContextRepository(ObjectMapper objectMapper,
                                              AgentSessionProperties properties,
                                              ObjectProvider<TextStateStore> stateStoreProvider) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.stateStore = stateStoreProvider.getIfAvailable();
    }

    @Override
    public synchronized AgentSessionSnapshot snapshot(String sessionId) {
        AgentSessionSnapshot snapshot = new AgentSessionSnapshot();
        String normalizedSessionId = normalize(sessionId);
        snapshot.setSessionId(normalizedSessionId);
        if (!StringUtils.hasText(normalizedSessionId) || stateStore == null) {
            return snapshot;
        }

        String json = stateStore.find(redisKey(normalizedSessionId)).orElse(null);
        if (!StringUtils.hasText(json)) {
            return snapshot;
        }

        try {
            snapshot.setRecentTurns(objectMapper.readValue(json, TURN_LIST_TYPE));
            return snapshot;
        } catch (JsonProcessingException e) {
            log.warn("读取管理员智能体 Redis 会话上下文失败 sessionId={}, message={}", normalizedSessionId, e.getMessage());
            return snapshot;
        }
    }

    @Override
    public synchronized void appendTurn(String sessionId, AgentSessionTurn turn, int maxRecentTurns) {
        String normalizedSessionId = normalize(sessionId);
        if (!StringUtils.hasText(normalizedSessionId) || turn == null || stateStore == null) {
            return;
        }

        List<AgentSessionTurn> turns = new ArrayList<>(snapshot(normalizedSessionId).getRecentTurns());
        turns.add(turn);
        turns = limitRecentTurns(turns, maxRecentTurns);

        String key = redisKey(normalizedSessionId);
        try {
            String json = objectMapper.writeValueAsString(turns);
            long ttlSeconds = properties == null ? 0L : properties.getTtlSeconds();
            if (ttlSeconds > 0L) {
                stateStore.put(key, json, Duration.ofSeconds(ttlSeconds));
            } else {
                stateStore.put(key, json);
            }
        } catch (JsonProcessingException e) {
            log.warn("保存管理员智能体 Redis 会话上下文失败 sessionId={}, message={}", normalizedSessionId, e.getMessage());
        }
    }

    private List<AgentSessionTurn> limitRecentTurns(List<AgentSessionTurn> turns, int maxRecentTurns) {
        int maxTurns = Math.max(maxRecentTurns, 0);
        if (turns.size() <= maxTurns) {
            return turns;
        }
        return new ArrayList<>(turns.subList(turns.size() - maxTurns, turns.size()));
    }

    private String redisKey(String sessionId) {
        String prefix = properties == null ? "" : normalize(properties.getRedisKeyPrefix());
        if (!StringUtils.hasText(prefix)) {
            prefix = "xiaou:admin:agent:session";
        }
        return prefix + ":" + sessionId;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
