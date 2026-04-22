package com.xiaou.ai.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.prompt.AiPromptSpec;
import com.xiaou.common.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AI 运行时内存指标聚合器。
 *
 * <p>用于后台可视化查看最近调用、分场景聚合和成本估算结果。</p>
 *
 * @author xiaou
 */
@Slf4j
@Component
public class AiRuntimeMetricsCollector {

    private static final int MAX_RECENT_CALLS = 50;

    private final Map<String, SceneAccumulator> sceneAccumulators = new LinkedHashMap<>();
    private final Deque<AiRuntimeRecentCall> recentCalls = new ArrayDeque<>();
    private AiRuntimeMetricsPersistence persistence;

    @Autowired(required = false)
    void setPersistence(AiRuntimeMetricsPersistence persistence) {
        this.persistence = persistence;
        restoreFromPersistence();
    }

    @Autowired
    void configureRedisPersistence(ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider,
                                   ObjectMapper objectMapper,
                                   AiProperties aiProperties) {
        if (this.persistence != null) {
            return;
        }
        if (aiProperties.getMetrics() == null || aiProperties.getMetrics().getPersistence() == null) {
            return;
        }
        if (!aiProperties.getMetrics().getPersistence().isEnabled()) {
            return;
        }
        StringRedisTemplate stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        if (stringRedisTemplate == null) {
            return;
        }
        this.persistence = new RedisAiRuntimeMetricsPersistence(stringRedisTemplate, objectMapper, aiProperties);
        restoreFromPersistence();
    }

    public synchronized void recordInvocation(String sceneName,
                                              AiPromptSpec promptSpec,
                                              String outcome,
                                              long durationNanos,
                                              String modelName,
                                              Integer inputTokens,
                                              Integer outputTokens,
                                              Integer totalTokens,
                                              BigDecimal estimatedCost) {
        long latencyMs = Math.max(0L, durationNanos / 1_000_000L);
        long now = System.currentTimeMillis();

        SceneAccumulator accumulator = sceneAccumulators.computeIfAbsent(
                buildSceneKey(sceneName, promptSpec),
                ignored -> new SceneAccumulator(
                        safeTag(sceneName),
                        promptKey(promptSpec),
                        promptVersion(promptSpec)
                )
        );
        accumulator.recordInvocation(outcome, latencyMs, modelName, inputTokens, outputTokens, totalTokens, estimatedCost, now);

        recentCalls.addFirst(new AiRuntimeRecentCall()
                .setScene(safeTag(sceneName))
                .setPromptKey(promptKey(promptSpec))
                .setPromptVersion(promptVersion(promptSpec))
                .setModelName(safeText(modelName))
                .setOutcome(safeTag(outcome))
                .setLatencyMs(latencyMs)
                .setInputTokens(inputTokens)
                .setOutputTokens(outputTokens)
                .setTotalTokens(totalTokens)
                .setEstimatedCost(defaultCost(estimatedCost))
                .setTimestamp(now));
        while (recentCalls.size() > MAX_RECENT_CALLS) {
            recentCalls.removeLast();
        }
        persistState();
    }

    public synchronized void recordFallback(String sceneName, AiPromptSpec promptSpec, String reason) {
        recordFallback(sceneName, promptSpec, reason, null);
    }

    public synchronized void recordFallback(String sceneName, AiPromptSpec promptSpec, String reason, String modelName) {
        sceneAccumulators.computeIfAbsent(
                buildSceneKey(sceneName, promptSpec),
                ignored -> new SceneAccumulator(
                        safeTag(sceneName),
                        promptKey(promptSpec),
                        promptVersion(promptSpec)
                )
        ).recordFallback(modelName);
        persistState();
    }

    public synchronized void recordStructuredParseFailure(String sceneName, AiPromptSpec promptSpec, String reason) {
        recordStructuredParseFailure(sceneName, promptSpec, reason, null);
    }

    public synchronized void recordStructuredParseFailure(String sceneName,
                                                          AiPromptSpec promptSpec,
                                                          String reason,
                                                          String modelName) {
        sceneAccumulators.computeIfAbsent(
                buildSceneKey(sceneName, promptSpec),
                ignored -> new SceneAccumulator(
                        safeTag(sceneName),
                        promptKey(promptSpec),
                        promptVersion(promptSpec)
                )
        ).recordParseFailure(modelName);
        persistState();
    }

    public synchronized AiRuntimeMetricsSnapshot snapshot() {
        return snapshot(null, null, null, MAX_RECENT_CALLS);
    }

    public synchronized AiRuntimeMetricsSnapshot snapshot(String sceneKeyword, String outcome, Integer recentLimit) {
        return snapshot(sceneKeyword, outcome, null, recentLimit);
    }

    public synchronized AiRuntimeMetricsSnapshot snapshot(String sceneKeyword,
                                                          String outcome,
                                                          String modelKeyword,
                                                          Integer recentLimit) {
        AiRuntimeMetricsSnapshot snapshot = new AiRuntimeMetricsSnapshot();
        AiRuntimeOverviewMetrics overview = new AiRuntimeOverviewMetrics();
        List<AiRuntimeSceneMetrics> sceneStats = new ArrayList<>();
        Map<String, ModelMetricsAccumulator> modelAccumulators = new LinkedHashMap<>();
        String normalizedSceneKeyword = normalizeKeyword(sceneKeyword);
        String normalizedOutcome = normalizeOutcome(outcome);
        String normalizedModelKeyword = normalizeKeyword(modelKeyword);
        int resolvedRecentLimit = resolveRecentLimit(recentLimit);
        long totalLatencyMs = 0L;

        for (SceneAccumulator accumulator : sceneAccumulators.values()) {
            if (!accumulator.matchesScene(normalizedSceneKeyword)) {
                continue;
            }

            AiRuntimeSceneMetrics sceneMetrics = accumulator.toSceneMetrics(normalizedOutcome, normalizedModelKeyword);
            if (!hasVisibleMetrics(sceneMetrics)) {
                continue;
            }
            sceneStats.add(sceneMetrics);

            overview.setTotalInvocations(overview.getTotalInvocations() + sceneMetrics.getInvocations());
            overview.setSuccessCount(overview.getSuccessCount() + sceneMetrics.getSuccessCount());
            overview.setErrorCount(overview.getErrorCount() + sceneMetrics.getErrorCount());
            overview.setFallbackCount(overview.getFallbackCount() + sceneMetrics.getFallbackCount());
            overview.setStructuredParseFailureCount(overview.getStructuredParseFailureCount() + sceneMetrics.getStructuredParseFailureCount());
            overview.setTotalInputTokens(overview.getTotalInputTokens() + sceneMetrics.getTotalInputTokens());
            overview.setTotalOutputTokens(overview.getTotalOutputTokens() + sceneMetrics.getTotalOutputTokens());
            overview.setTotalTokens(overview.getTotalTokens() + sceneMetrics.getTotalTokens());
            overview.setEstimatedCost(overview.getEstimatedCost().add(sceneMetrics.getEstimatedCost()));
            totalLatencyMs += accumulator.selectedTotalLatencyMs(normalizedOutcome, normalizedModelKeyword);
            if (sceneMetrics.getLastInvocationAt() != null
                    && (overview.getLastInvocationAt() == null || sceneMetrics.getLastInvocationAt() > overview.getLastInvocationAt())) {
                overview.setLastInvocationAt(sceneMetrics.getLastInvocationAt());
            }
            accumulator.accumulateModelMetrics(modelAccumulators, normalizedOutcome, normalizedModelKeyword);
        }

        if (overview.getTotalInvocations() > 0) {
            overview.setAverageLatencyMs(totalLatencyMs / overview.getTotalInvocations());
        }

        sceneStats.sort(Comparator.comparingLong(AiRuntimeSceneMetrics::getInvocations).reversed()
                .thenComparing(AiRuntimeSceneMetrics::getScene));
        snapshot.setOverview(overview);
        snapshot.setSceneStats(sceneStats);
        snapshot.setModelStats(toModelMetrics(modelAccumulators));
        snapshot.setRecentCalls(filterRecentCalls(normalizedSceneKeyword, normalizedOutcome, normalizedModelKeyword, resolvedRecentLimit));
        return snapshot;
    }

    /**
     * 清空当前进程内聚合的运行观测数据。
     */
    public synchronized void clear() {
        sceneAccumulators.clear();
        recentCalls.clear();
        clearPersistence();
        log.info("AI 运行观测内存聚合数据已清空");
    }

    public synchronized boolean isPersistenceEnabled() {
        return persistence != null;
    }

    public synchronized String persistenceMode() {
        return persistence == null ? "memory" : persistence.mode();
    }

    private void restoreFromPersistence() {
        if (persistence == null) {
            return;
        }
        try {
            AiRuntimeMetricsStoreState state = persistence.load();
            if (state == null) {
                return;
            }
            restoreState(state);
            log.info("AI 运行观测已从 {} 持久化状态恢复", persistence.mode());
        } catch (Exception e) {
            log.warn("AI 运行观测恢复失败，将继续使用内存模式: {}", e.getMessage());
        }
    }

    private void persistState() {
        if (persistence == null) {
            return;
        }
        try {
            persistence.save(exportState());
        } catch (Exception e) {
            log.warn("AI 运行观测持久化失败，将继续保留内存聚合: {}", e.getMessage());
        }
    }

    private void clearPersistence() {
        if (persistence == null) {
            return;
        }
        try {
            persistence.clear();
        } catch (Exception e) {
            log.warn("AI 运行观测持久化清理失败: {}", e.getMessage());
        }
    }

    private AiRuntimeMetricsStoreState exportState() {
        AiRuntimeMetricsStoreState state = new AiRuntimeMetricsStoreState();
        Map<String, AiRuntimeSceneStoreState> sceneStates = new LinkedHashMap<>();
        for (Map.Entry<String, SceneAccumulator> entry : sceneAccumulators.entrySet()) {
            sceneStates.put(entry.getKey(), entry.getValue().toStoreState());
        }
        state.setSceneStates(sceneStates);
        state.setRecentCalls(new ArrayList<>(recentCalls));
        return state;
    }

    private void restoreState(AiRuntimeMetricsStoreState state) {
        sceneAccumulators.clear();
        recentCalls.clear();
        if (state == null) {
            return;
        }
        if (state.getSceneStates() != null) {
            for (Map.Entry<String, AiRuntimeSceneStoreState> entry : state.getSceneStates().entrySet()) {
                sceneAccumulators.put(entry.getKey(), SceneAccumulator.fromStoreState(entry.getValue()));
            }
        }
        if (state.getRecentCalls() != null) {
            for (AiRuntimeRecentCall recentCall : state.getRecentCalls()) {
                if (recentCall == null) {
                    continue;
                }
                recentCalls.addLast(recentCall);
                if (recentCalls.size() >= MAX_RECENT_CALLS) {
                    break;
                }
            }
        }
    }

    private ArrayList<AiRuntimeRecentCall> filterRecentCalls(String sceneKeyword,
                                                            String outcome,
                                                            String modelKeyword,
                                                            int recentLimit) {
        ArrayList<AiRuntimeRecentCall> filtered = new ArrayList<>();
        for (AiRuntimeRecentCall recentCall : recentCalls) {
            if (!matchesScene(recentCall.getScene(), sceneKeyword)
                    && !matchesScene(recentCall.getPromptKey(), sceneKeyword)
                    && !matchesScene(recentCall.getPromptVersion(), sceneKeyword)) {
                continue;
            }
            if (!matchesOutcome(recentCall.getOutcome(), outcome)) {
                continue;
            }
            if (!matchesModel(recentCall.getModelName(), modelKeyword)) {
                continue;
            }
            filtered.add(recentCall);
            if (filtered.size() >= recentLimit) {
                break;
            }
        }
        return filtered;
    }

    private boolean hasVisibleMetrics(AiRuntimeSceneMetrics sceneMetrics) {
        return sceneMetrics.getInvocations() > 0
                || sceneMetrics.getFallbackCount() > 0
                || sceneMetrics.getStructuredParseFailureCount() > 0;
    }

    private List<AiRuntimeModelMetrics> toModelMetrics(Map<String, ModelMetricsAccumulator> modelAccumulators) {
        ArrayList<AiRuntimeModelMetrics> modelStats = new ArrayList<>();
        for (ModelMetricsAccumulator accumulator : modelAccumulators.values()) {
            if (accumulator.invocations == 0) {
                continue;
            }
            modelStats.add(accumulator.toMetrics());
        }
        modelStats.sort(Comparator.comparingLong(AiRuntimeModelMetrics::getInvocations).reversed()
                .thenComparing(AiRuntimeModelMetrics::getModelName));
        return modelStats;
    }

    private boolean matchesScene(String source, String sceneKeyword) {
        if (!StringUtils.hasText(sceneKeyword)) {
            return true;
        }
        if (!StringUtils.hasText(source)) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(sceneKeyword);
    }

    private boolean matchesOutcome(String source, String outcome) {
        if (!StringUtils.hasText(outcome)) {
            return true;
        }
        return outcome.equals(normalizeOutcome(source));
    }

    private boolean matchesModel(String source, String modelKeyword) {
        if (!StringUtils.hasText(modelKeyword)) {
            return true;
        }
        if (!StringUtils.hasText(source)) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(modelKeyword);
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOutcome(String outcome) {
        if (!StringUtils.hasText(outcome)) {
            return null;
        }
        return outcome.trim().toLowerCase(Locale.ROOT);
    }

    private int resolveRecentLimit(Integer recentLimit) {
        if (recentLimit == null || recentLimit <= 0) {
            return MAX_RECENT_CALLS;
        }
        return Math.min(recentLimit, MAX_RECENT_CALLS);
    }

    private String buildSceneKey(String sceneName, AiPromptSpec promptSpec) {
        return safeTag(sceneName) + "|" + promptKey(promptSpec) + "|" + promptVersion(promptSpec);
    }

    private String promptKey(AiPromptSpec promptSpec) {
        return promptSpec == null ? "ad_hoc" : safeTag(promptSpec.key());
    }

    private String promptVersion(AiPromptSpec promptSpec) {
        return promptSpec == null ? "na" : safeTag(promptSpec.version());
    }

    private String safeTag(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private BigDecimal defaultCost(BigDecimal estimatedCost) {
        return estimatedCost == null ? BigDecimal.ZERO : estimatedCost;
    }

    private static final class SceneAccumulator {
        private final String scene;
        private final String promptKey;
        private final String promptVersion;
        private long fallbackCount;
        private long errorFallbackCount;
        private long structuredParseFailureCount;
        private long errorStructuredParseFailureCount;
        private final InvocationAccumulator totalAccumulator = new InvocationAccumulator();
        private final InvocationAccumulator successAccumulator = new InvocationAccumulator();
        private final InvocationAccumulator errorAccumulator = new InvocationAccumulator();
        private final Map<String, InvocationAccumulator> totalModelAccumulators = new LinkedHashMap<>();
        private final Map<String, InvocationAccumulator> successModelAccumulators = new LinkedHashMap<>();
        private final Map<String, InvocationAccumulator> errorModelAccumulators = new LinkedHashMap<>();
        private final Map<String, Long> totalModelFallbackCounts = new LinkedHashMap<>();
        private final Map<String, Long> errorModelFallbackCounts = new LinkedHashMap<>();
        private final Map<String, Long> totalModelParseFailureCounts = new LinkedHashMap<>();
        private final Map<String, Long> errorModelParseFailureCounts = new LinkedHashMap<>();

        private SceneAccumulator(String scene, String promptKey, String promptVersion) {
            this.scene = scene;
            this.promptKey = promptKey;
            this.promptVersion = promptVersion;
        }

        private void recordInvocation(String outcome,
                                      long latencyMs,
                                      String modelName,
                                      Integer inputTokens,
                                      Integer outputTokens,
                                      Integer totalTokens,
                                      BigDecimal cost,
                                      long timestamp) {
            String normalizedModelName = normalizeModelName(modelName);
            totalAccumulator.record(latencyMs, normalizedModelName, inputTokens, outputTokens, totalTokens, cost, timestamp);
            totalModelAccumulators.computeIfAbsent(normalizedModelName, ignored -> new InvocationAccumulator())
                    .record(latencyMs, normalizedModelName, inputTokens, outputTokens, totalTokens, cost, timestamp);
            if (isSuccess(outcome)) {
                successAccumulator.record(latencyMs, normalizedModelName, inputTokens, outputTokens, totalTokens, cost, timestamp);
                successModelAccumulators.computeIfAbsent(normalizedModelName, ignored -> new InvocationAccumulator())
                        .record(latencyMs, normalizedModelName, inputTokens, outputTokens, totalTokens, cost, timestamp);
            } else {
                errorAccumulator.record(latencyMs, normalizedModelName, inputTokens, outputTokens, totalTokens, cost, timestamp);
                errorModelAccumulators.computeIfAbsent(normalizedModelName, ignored -> new InvocationAccumulator())
                        .record(latencyMs, normalizedModelName, inputTokens, outputTokens, totalTokens, cost, timestamp);
            }
        }

        private void recordFallback(String modelName) {
            fallbackCount++;
            errorFallbackCount++;
            String normalizedModelName = normalizeModelName(modelName);
            incrementCounter(totalModelFallbackCounts, normalizedModelName);
            incrementCounter(errorModelFallbackCounts, normalizedModelName);
        }

        private void recordParseFailure(String modelName) {
            structuredParseFailureCount++;
            errorStructuredParseFailureCount++;
            String normalizedModelName = normalizeModelName(modelName);
            incrementCounter(totalModelParseFailureCounts, normalizedModelName);
            incrementCounter(errorModelParseFailureCounts, normalizedModelName);
        }

        private boolean matchesScene(String sceneKeyword) {
            if (!StringUtils.hasText(sceneKeyword)) {
                return true;
            }
            return containsIgnoreCase(scene, sceneKeyword)
                    || containsIgnoreCase(promptKey, sceneKeyword)
                    || containsIgnoreCase(promptVersion, sceneKeyword);
        }

        private long selectedTotalLatencyMs(String outcomeFilter, String modelKeyword) {
            return selectAccumulator(outcomeFilter, modelKeyword).totalLatencyMs;
        }

        private void accumulateModelMetrics(Map<String, ModelMetricsAccumulator> target,
                                            String outcomeFilter,
                                            String modelKeyword) {
            if ("success".equals(outcomeFilter)) {
                mergeModelAccumulators(target, successModelAccumulators, true, modelKeyword);
                return;
            }
            if ("error".equals(outcomeFilter)) {
                mergeModelAccumulators(target, errorModelAccumulators, false, modelKeyword);
                return;
            }

            for (Map.Entry<String, InvocationAccumulator> entry : totalModelAccumulators.entrySet()) {
                String modelName = entry.getKey();
                if (!matchesModel(modelName, modelKeyword)) {
                    continue;
                }
                InvocationAccumulator total = entry.getValue();
                InvocationAccumulator success = successModelAccumulators.get(modelName);
                InvocationAccumulator error = errorModelAccumulators.get(modelName);
                target.computeIfAbsent(modelName, ModelMetricsAccumulator::new)
                        .merge(total, success == null ? 0L : success.invocations, error == null ? 0L : error.invocations);
            }
        }

        private AiRuntimeSceneMetrics toSceneMetrics(String outcomeFilter, String modelKeyword) {
            InvocationAccumulator selectedAccumulator = selectAccumulator(outcomeFilter, modelKeyword);
            long selectedFallbackCount = selectedFallbackCount(outcomeFilter, modelKeyword);
            long selectedParseFailureCount = selectedParseFailureCount(outcomeFilter, modelKeyword);
            return new AiRuntimeSceneMetrics()
                    .setScene(scene)
                    .setPromptKey(promptKey)
                    .setPromptVersion(promptVersion)
                    .setLastModelName(selectedAccumulator.lastModelName)
                    .setInvocations(selectedAccumulator.invocations)
                    .setSuccessCount(successCount(outcomeFilter, modelKeyword))
                    .setErrorCount(errorCount(outcomeFilter, modelKeyword))
                    .setFallbackCount(selectedFallbackCount)
                    .setStructuredParseFailureCount(selectedParseFailureCount)
                    .setTotalInputTokens(selectedAccumulator.totalInputTokens)
                    .setTotalOutputTokens(selectedAccumulator.totalOutputTokens)
                    .setTotalTokens(selectedAccumulator.totalTokens)
                    .setEstimatedCost(selectedAccumulator.estimatedCost)
                    .setAverageLatencyMs(selectedAccumulator.averageLatencyMs())
                    .setLastInvocationAt(selectedAccumulator.lastInvocationAt);
        }

        private InvocationAccumulator selectAccumulator(String outcomeFilter) {
            if ("success".equals(outcomeFilter)) {
                return successAccumulator;
            }
            if ("error".equals(outcomeFilter)) {
                return errorAccumulator;
            }
            return totalAccumulator;
        }

        private InvocationAccumulator selectAccumulator(String outcomeFilter, String modelKeyword) {
            if (!StringUtils.hasText(modelKeyword)) {
                return selectAccumulator(outcomeFilter);
            }

            InvocationAccumulator merged = new InvocationAccumulator();
            for (Map.Entry<String, InvocationAccumulator> entry : selectModelAccumulators(outcomeFilter).entrySet()) {
                if (!matchesModel(entry.getKey(), modelKeyword)) {
                    continue;
                }
                merged.merge(entry.getValue());
            }
            return merged;
        }

        private Map<String, InvocationAccumulator> selectModelAccumulators(String outcomeFilter) {
            if ("success".equals(outcomeFilter)) {
                return successModelAccumulators;
            }
            if ("error".equals(outcomeFilter)) {
                return errorModelAccumulators;
            }
            return totalModelAccumulators;
        }

        private long selectedFallbackCount(String outcomeFilter, String modelKeyword) {
            if (StringUtils.hasText(modelKeyword)) {
                if ("success".equals(outcomeFilter)) {
                    return 0L;
                }
                if ("error".equals(outcomeFilter)) {
                    return sumMatchingCounts(errorModelFallbackCounts, modelKeyword);
                }
                return sumMatchingCounts(totalModelFallbackCounts, modelKeyword);
            }
            if ("success".equals(outcomeFilter)) {
                return 0L;
            }
            if ("error".equals(outcomeFilter)) {
                return errorFallbackCount;
            }
            return fallbackCount;
        }

        private long selectedParseFailureCount(String outcomeFilter, String modelKeyword) {
            if (StringUtils.hasText(modelKeyword)) {
                if ("success".equals(outcomeFilter)) {
                    return 0L;
                }
                if ("error".equals(outcomeFilter)) {
                    return sumMatchingCounts(errorModelParseFailureCounts, modelKeyword);
                }
                return sumMatchingCounts(totalModelParseFailureCounts, modelKeyword);
            }
            if ("success".equals(outcomeFilter)) {
                return 0L;
            }
            if ("error".equals(outcomeFilter)) {
                return errorStructuredParseFailureCount;
            }
            return structuredParseFailureCount;
        }

        private long successCount(String outcomeFilter, String modelKeyword) {
            if ("error".equals(outcomeFilter)) {
                return 0L;
            }
            return selectAccumulator("success", modelKeyword).invocations;
        }

        private long errorCount(String outcomeFilter, String modelKeyword) {
            if ("success".equals(outcomeFilter)) {
                return 0L;
            }
            return selectAccumulator("error", modelKeyword).invocations;
        }

        private boolean containsIgnoreCase(String source, String keyword) {
            return StringUtils.hasText(source)
                    && source.toLowerCase(Locale.ROOT).contains(keyword);
        }

        private boolean matchesModel(String modelName, String modelKeyword) {
            if (!StringUtils.hasText(modelKeyword)) {
                return true;
            }
            return containsIgnoreCase(modelName, modelKeyword);
        }

        private boolean isSuccess(String outcome) {
            return "success".equalsIgnoreCase(outcome);
        }

        private void mergeModelAccumulators(Map<String, ModelMetricsAccumulator> target,
                                            Map<String, InvocationAccumulator> source,
                                            boolean success,
                                            String modelKeyword) {
            for (Map.Entry<String, InvocationAccumulator> entry : source.entrySet()) {
                if (!matchesModel(entry.getKey(), modelKeyword)) {
                    continue;
                }
                long successCount = success ? entry.getValue().invocations : 0L;
                long errorCount = success ? 0L : entry.getValue().invocations;
                target.computeIfAbsent(entry.getKey(), ModelMetricsAccumulator::new)
                        .merge(entry.getValue(), successCount, errorCount);
            }
        }

        private long sumMatchingCounts(Map<String, Long> source, String modelKeyword) {
            long total = 0L;
            for (Map.Entry<String, Long> entry : source.entrySet()) {
                if (!matchesModel(entry.getKey(), modelKeyword)) {
                    continue;
                }
                total += entry.getValue();
            }
            return total;
        }

        private void incrementCounter(Map<String, Long> target, String modelName) {
            target.merge(modelName, 1L, Long::sum);
        }

        private String normalizeModelName(String modelName) {
            return StringUtils.hasText(modelName) ? modelName.trim() : "-";
        }

        private AiRuntimeSceneStoreState toStoreState() {
            return new AiRuntimeSceneStoreState()
                    .setScene(scene)
                    .setPromptKey(promptKey)
                    .setPromptVersion(promptVersion)
                    .setFallbackCount(fallbackCount)
                    .setErrorFallbackCount(errorFallbackCount)
                    .setStructuredParseFailureCount(structuredParseFailureCount)
                    .setErrorStructuredParseFailureCount(errorStructuredParseFailureCount)
                    .setTotalAccumulator(totalAccumulator.toState())
                    .setSuccessAccumulator(successAccumulator.toState())
                    .setErrorAccumulator(errorAccumulator.toState())
                    .setTotalModelAccumulators(toStateMap(totalModelAccumulators))
                    .setSuccessModelAccumulators(toStateMap(successModelAccumulators))
                    .setErrorModelAccumulators(toStateMap(errorModelAccumulators))
                    .setTotalModelFallbackCounts(new LinkedHashMap<>(totalModelFallbackCounts))
                    .setErrorModelFallbackCounts(new LinkedHashMap<>(errorModelFallbackCounts))
                    .setTotalModelParseFailureCounts(new LinkedHashMap<>(totalModelParseFailureCounts))
                    .setErrorModelParseFailureCounts(new LinkedHashMap<>(errorModelParseFailureCounts));
        }

        private static SceneAccumulator fromStoreState(AiRuntimeSceneStoreState state) {
            SceneAccumulator accumulator = new SceneAccumulator(
                    state == null ? null : state.getScene(),
                    state == null ? null : state.getPromptKey(),
                    state == null ? null : state.getPromptVersion()
            );
            if (state == null) {
                return accumulator;
            }
            accumulator.fallbackCount = state.getFallbackCount();
            accumulator.errorFallbackCount = state.getErrorFallbackCount();
            accumulator.structuredParseFailureCount = state.getStructuredParseFailureCount();
            accumulator.errorStructuredParseFailureCount = state.getErrorStructuredParseFailureCount();
            accumulator.totalAccumulator.copyFrom(state.getTotalAccumulator());
            accumulator.successAccumulator.copyFrom(state.getSuccessAccumulator());
            accumulator.errorAccumulator.copyFrom(state.getErrorAccumulator());
            accumulator.totalModelAccumulators.putAll(fromStateMap(state.getTotalModelAccumulators()));
            accumulator.successModelAccumulators.putAll(fromStateMap(state.getSuccessModelAccumulators()));
            accumulator.errorModelAccumulators.putAll(fromStateMap(state.getErrorModelAccumulators()));
            accumulator.totalModelFallbackCounts.putAll(copyLongMap(state.getTotalModelFallbackCounts()));
            accumulator.errorModelFallbackCounts.putAll(copyLongMap(state.getErrorModelFallbackCounts()));
            accumulator.totalModelParseFailureCounts.putAll(copyLongMap(state.getTotalModelParseFailureCounts()));
            accumulator.errorModelParseFailureCounts.putAll(copyLongMap(state.getErrorModelParseFailureCounts()));
            return accumulator;
        }

        private Map<String, AiRuntimeInvocationAccumulatorState> toStateMap(Map<String, InvocationAccumulator> source) {
            Map<String, AiRuntimeInvocationAccumulatorState> stateMap = new LinkedHashMap<>();
            for (Map.Entry<String, InvocationAccumulator> entry : source.entrySet()) {
                stateMap.put(entry.getKey(), entry.getValue().toState());
            }
            return stateMap;
        }

        private static Map<String, InvocationAccumulator> fromStateMap(Map<String, AiRuntimeInvocationAccumulatorState> source) {
            Map<String, InvocationAccumulator> stateMap = new LinkedHashMap<>();
            if (source == null) {
                return stateMap;
            }
            for (Map.Entry<String, AiRuntimeInvocationAccumulatorState> entry : source.entrySet()) {
                InvocationAccumulator accumulator = new InvocationAccumulator();
                accumulator.copyFrom(entry.getValue());
                stateMap.put(entry.getKey(), accumulator);
            }
            return stateMap;
        }

        private static Map<String, Long> copyLongMap(Map<String, Long> source) {
            return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
        }
    }

    private static final class InvocationAccumulator {
        private long invocations;
        private long totalLatencyMs;
        private long totalInputTokens;
        private long totalOutputTokens;
        private long totalTokens;
        private BigDecimal estimatedCost = BigDecimal.ZERO;
        private String lastModelName = "-";
        private Long lastInvocationAt;

        private void record(long latencyMs,
                            String modelName,
                            Integer inputTokens,
                            Integer outputTokens,
                            Integer totalTokens,
                            BigDecimal cost,
                            long timestamp) {
            invocations++;
            totalLatencyMs += latencyMs;
            this.totalInputTokens += safeLong(inputTokens);
            this.totalOutputTokens += safeLong(outputTokens);
            this.totalTokens += safeLong(totalTokens);
            this.estimatedCost = this.estimatedCost.add(cost == null ? BigDecimal.ZERO : cost);
            this.lastModelName = StringUtils.hasText(modelName) ? modelName.trim() : this.lastModelName;
            this.lastInvocationAt = timestamp;
        }

        private long averageLatencyMs() {
            return invocations == 0 ? 0L : totalLatencyMs / invocations;
        }

        private void merge(InvocationAccumulator source) {
            this.invocations += source.invocations;
            this.totalLatencyMs += source.totalLatencyMs;
            this.totalInputTokens += source.totalInputTokens;
            this.totalOutputTokens += source.totalOutputTokens;
            this.totalTokens += source.totalTokens;
            this.estimatedCost = this.estimatedCost.add(source.estimatedCost);
            if (source.lastInvocationAt != null
                    && (this.lastInvocationAt == null || source.lastInvocationAt > this.lastInvocationAt)) {
                this.lastInvocationAt = source.lastInvocationAt;
                this.lastModelName = source.lastModelName;
            }
        }

        private AiRuntimeInvocationAccumulatorState toState() {
            return new AiRuntimeInvocationAccumulatorState()
                    .setInvocations(invocations)
                    .setTotalLatencyMs(totalLatencyMs)
                    .setTotalInputTokens(totalInputTokens)
                    .setTotalOutputTokens(totalOutputTokens)
                    .setTotalTokens(totalTokens)
                    .setEstimatedCost(estimatedCost)
                    .setLastModelName(lastModelName)
                    .setLastInvocationAt(lastInvocationAt);
        }

        private void copyFrom(AiRuntimeInvocationAccumulatorState state) {
            if (state == null) {
                return;
            }
            this.invocations = state.getInvocations();
            this.totalLatencyMs = state.getTotalLatencyMs();
            this.totalInputTokens = state.getTotalInputTokens();
            this.totalOutputTokens = state.getTotalOutputTokens();
            this.totalTokens = state.getTotalTokens();
            this.estimatedCost = state.getEstimatedCost() == null ? BigDecimal.ZERO : state.getEstimatedCost();
            this.lastModelName = StringUtils.hasText(state.getLastModelName()) ? state.getLastModelName().trim() : "-";
            this.lastInvocationAt = state.getLastInvocationAt();
        }

        private long safeLong(Integer value) {
            return value == null ? 0L : Math.max(value, 0);
        }
    }

    private static final class ModelMetricsAccumulator {
        private final String modelName;
        private long invocations;
        private long successCount;
        private long errorCount;
        private long totalLatencyMs;
        private long totalInputTokens;
        private long totalOutputTokens;
        private long totalTokens;
        private BigDecimal estimatedCost = BigDecimal.ZERO;
        private Long lastInvocationAt;

        private ModelMetricsAccumulator(String modelName) {
            this.modelName = modelName;
        }

        private void merge(InvocationAccumulator source, long successCount, long errorCount) {
            this.invocations += source.invocations;
            this.successCount += successCount;
            this.errorCount += errorCount;
            this.totalLatencyMs += source.totalLatencyMs;
            this.totalInputTokens += source.totalInputTokens;
            this.totalOutputTokens += source.totalOutputTokens;
            this.totalTokens += source.totalTokens;
            this.estimatedCost = this.estimatedCost.add(source.estimatedCost);
            if (source.lastInvocationAt != null
                    && (this.lastInvocationAt == null || source.lastInvocationAt > this.lastInvocationAt)) {
                this.lastInvocationAt = source.lastInvocationAt;
            }
        }

        private AiRuntimeModelMetrics toMetrics() {
            return new AiRuntimeModelMetrics()
                    .setModelName(modelName)
                    .setInvocations(invocations)
                    .setSuccessCount(successCount)
                    .setErrorCount(errorCount)
                    .setTotalInputTokens(totalInputTokens)
                    .setTotalOutputTokens(totalOutputTokens)
                    .setTotalTokens(totalTokens)
                    .setEstimatedCost(estimatedCost)
                    .setAverageLatencyMs(invocations == 0 ? 0L : totalLatencyMs / invocations)
                    .setLastInvocationAt(lastInvocationAt);
        }
    }
}
