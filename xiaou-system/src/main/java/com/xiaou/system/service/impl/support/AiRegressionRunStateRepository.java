package com.xiaou.system.service.impl.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.config.AiProperties;
import com.xiaou.system.dto.AiRegressionRunResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 最近一次 AI 黄金样例回归结果状态仓储。
 *
 * <p>默认优先保存在当前进程内存中；若 Redis 可用且运行观测持久化已启用，
 * 则同时写入 Redis，便于后台页面刷新或服务重启后恢复最近一次回归结果。</p>
 *
 * @author xiaou
 */
@Slf4j
@Component
public class AiRegressionRunStateRepository {

    private static final String DEFAULT_REDIS_KEY = "xiaou:ai:runtime:regression:last";
    private static final int MAX_HISTORY_SIZE = 20;

    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final AtomicReference<AiRegressionRunResponse> inMemoryState = new AtomicReference<>();
    private final AtomicReference<List<AiRegressionRunResponse>> inMemoryHistory = new AtomicReference<>(List.of());

    public AiRegressionRunStateRepository(ObjectMapper objectMapper,
                                          AiProperties aiProperties,
                                          ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
    }

    /**
     * 读取最近一次回归结果。
     */
    public synchronized AiRegressionRunResponse loadLatest() {
        AiRegressionRunResponse memoryState = copyOf(inMemoryState.get());
        if (memoryState != null) {
            return memoryState;
        }
        if (!isRedisPersistenceAvailable()) {
            return null;
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(resolveRedisKey());
            if (!StringUtils.hasText(json)) {
                return null;
            }
            AiRegressionRunResponse stored = objectMapper.readValue(json, AiRegressionRunResponse.class);
            inMemoryState.set(copyOf(stored));
            return copyOf(stored);
        } catch (Exception e) {
            log.warn("读取最近一次 AI 回归结果失败，将继续使用内存状态: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 保存最近一次回归结果。
     */
    public synchronized void saveLatest(AiRegressionRunResponse response) {
        AiRegressionRunResponse snapshot = copyOf(response);
        inMemoryState.set(snapshot);
        saveHistorySnapshot(snapshot);
        if (snapshot == null || !isRedisPersistenceAvailable()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(resolveRedisKey(), objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            log.warn("保存最近一次 AI 回归结果失败，将继续保留内存状态: {}", e.getMessage());
        }
    }

    /**
     * 读取最近几次回归结果。
     */
    public synchronized List<AiRegressionRunResponse> loadHistory(int limit) {
        int resolvedLimit = resolveLimit(limit);
        List<AiRegressionRunResponse> memoryHistory = copyList(inMemoryHistory.get());
        if (!memoryHistory.isEmpty()) {
            return limitList(memoryHistory, resolvedLimit);
        }
        if (!isRedisPersistenceAvailable()) {
            AiRegressionRunResponse latest = copyOf(inMemoryState.get());
            if (latest != null) {
                return List.of(latest);
            }
            return memoryHistory;
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(resolveHistoryRedisKey());
            if (!StringUtils.hasText(json)) {
                AiRegressionRunResponse latest = loadLatest();
                if (latest != null) {
                    return List.of(copyOf(latest));
                }
                return memoryHistory;
            }
            List<AiRegressionRunResponse> stored = objectMapper.readerForListOf(AiRegressionRunResponse.class).readValue(json);
            List<AiRegressionRunResponse> copied = limitList(copyList(stored), MAX_HISTORY_SIZE);
            inMemoryHistory.set(copied);
            return limitList(copied, resolvedLimit);
        } catch (Exception e) {
            log.warn("读取 AI 回归历史失败，将继续使用内存状态: {}", e.getMessage());
            return memoryHistory;
        }
    }

    private boolean isRedisPersistenceAvailable() {
        return stringRedisTemplate != null
                && aiProperties.getMetrics() != null
                && aiProperties.getMetrics().getPersistence() != null
                && aiProperties.getMetrics().getPersistence().isEnabled();
    }

    private String resolveRedisKey() {
        if (aiProperties.getMetrics() == null || aiProperties.getMetrics().getPersistence() == null) {
            return DEFAULT_REDIS_KEY;
        }
        String metricsRedisKey = aiProperties.getMetrics().getPersistence().getRedisKey();
        if (!StringUtils.hasText(metricsRedisKey)) {
            return DEFAULT_REDIS_KEY;
        }
        return metricsRedisKey.trim() + ":regression:last";
    }

    private String resolveHistoryRedisKey() {
        return resolveRedisKey() + ":history";
    }

    private void saveHistorySnapshot(AiRegressionRunResponse snapshot) {
        if (snapshot == null) {
            return;
        }
        ArrayList<AiRegressionRunResponse> history = new ArrayList<>();
        history.add(copyOf(snapshot));
        for (AiRegressionRunResponse item : inMemoryHistory.get()) {
            if (item == null || isSameRun(item, snapshot)) {
                continue;
            }
            history.add(copyOf(item));
            if (history.size() >= MAX_HISTORY_SIZE) {
                break;
            }
        }
        inMemoryHistory.set(history);
        saveHistoryToRedis(history);
    }

    private void saveHistoryToRedis(List<AiRegressionRunResponse> history) {
        if (!isRedisPersistenceAvailable()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(resolveHistoryRedisKey(), objectMapper.writeValueAsString(history));
        } catch (Exception e) {
            log.warn("保存 AI 回归历史失败，将继续保留内存状态: {}", e.getMessage());
        }
    }

    private boolean isSameRun(AiRegressionRunResponse left, AiRegressionRunResponse right) {
        return normalizeKey(left == null ? null : left.getExecutedAt()).equals(normalizeKey(right == null ? null : right.getExecutedAt()))
                && normalizeKey(left == null ? null : left.getScenario()).equals(normalizeKey(right == null ? null : right.getScenario()))
                && normalizeKey(left == null ? null : left.getCaseId()).equals(normalizeKey(right == null ? null : right.getCaseId()))
                && normalizeKey(left == null ? null : left.getDurationMs()).equals(normalizeKey(right == null ? null : right.getDurationMs()));
    }

    private String normalizeKey(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int resolveLimit(int limit) {
        if (limit <= 0) {
            return MAX_HISTORY_SIZE;
        }
        return Math.min(limit, MAX_HISTORY_SIZE);
    }

    private List<AiRegressionRunResponse> copyList(List<AiRegressionRunResponse> source) {
        ArrayList<AiRegressionRunResponse> copied = new ArrayList<>();
        if (source == null) {
            return copied;
        }
        for (AiRegressionRunResponse item : source) {
            if (item == null) {
                continue;
            }
            copied.add(copyOf(item));
        }
        return copied;
    }

    private List<AiRegressionRunResponse> limitList(List<AiRegressionRunResponse> source, int limit) {
        if (source.size() <= limit) {
            return source;
        }
        return new ArrayList<>(source.subList(0, limit));
    }

    private AiRegressionRunResponse copyOf(AiRegressionRunResponse response) {
        if (response == null) {
            return null;
        }
        return objectMapper.convertValue(response, AiRegressionRunResponse.class);
    }
}
