package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 运行观测响应。
 *
 * @author xiaou
 */
@Data
public class AiRuntimeMetricsResponse {

    /**
     * 总览数据。
     */
    private AiRuntimeMetricsOverviewResponse overview = new AiRuntimeMetricsOverviewResponse();

    /**
     * 分场景聚合。
     */
    private List<AiRuntimeSceneMetricsResponse> sceneStats = new ArrayList<>();

    /**
     * 分模型聚合。
     */
    private List<AiRuntimeModelMetricsResponse> modelStats = new ArrayList<>();

    /**
     * 最近调用记录。
     */
    private List<AiRuntimeRecentCallResponse> recentCalls = new ArrayList<>();
}
