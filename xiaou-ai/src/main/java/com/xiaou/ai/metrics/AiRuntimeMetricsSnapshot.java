package com.xiaou.ai.metrics;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 运行时指标快照。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeMetricsSnapshot {

    /**
     * 总览统计。
     */
    private AiRuntimeOverviewMetrics overview = new AiRuntimeOverviewMetrics();

    /**
     * 分场景统计。
     */
    private List<AiRuntimeSceneMetrics> sceneStats = new ArrayList<>();

    /**
     * 分模型统计。
     */
    private List<AiRuntimeModelMetrics> modelStats = new ArrayList<>();

    /**
     * 最近调用记录。
     */
    private List<AiRuntimeRecentCall> recentCalls = new ArrayList<>();
}
