package com.xiaou.ai.metrics;

/**
 * AI 运行观测持久化接口。
 *
 * @author xiaou
 */
public interface AiRuntimeMetricsPersistence {

    /**
     * 持久化模式标识。
     */
    String mode();

    /**
     * 读取已持久化的运行观测状态。
     */
    AiRuntimeMetricsStoreState load();

    /**
     * 保存运行观测状态。
     */
    void save(AiRuntimeMetricsStoreState state);

    /**
     * 清空已持久化的运行观测状态。
     */
    void clear();
}
