package com.xiaou.ai.metrics;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 运行观测持久化状态。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeMetricsStoreState {

    /**
     * 分场景累计状态。
     */
    private Map<String, AiRuntimeSceneStoreState> sceneStates = new LinkedHashMap<>();

    /**
     * 最近调用记录。
     */
    private List<AiRuntimeRecentCall> recentCalls = new ArrayList<>();
}
