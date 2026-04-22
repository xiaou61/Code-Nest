package com.xiaou.ai.metrics;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个场景的运行观测持久化状态。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeSceneStoreState {

    private String scene;
    private String promptKey;
    private String promptVersion;
    private long fallbackCount;
    private long errorFallbackCount;
    private long structuredParseFailureCount;
    private long errorStructuredParseFailureCount;
    private AiRuntimeInvocationAccumulatorState totalAccumulator = new AiRuntimeInvocationAccumulatorState();
    private AiRuntimeInvocationAccumulatorState successAccumulator = new AiRuntimeInvocationAccumulatorState();
    private AiRuntimeInvocationAccumulatorState errorAccumulator = new AiRuntimeInvocationAccumulatorState();
    private Map<String, AiRuntimeInvocationAccumulatorState> totalModelAccumulators = new LinkedHashMap<>();
    private Map<String, AiRuntimeInvocationAccumulatorState> successModelAccumulators = new LinkedHashMap<>();
    private Map<String, AiRuntimeInvocationAccumulatorState> errorModelAccumulators = new LinkedHashMap<>();
    private Map<String, Long> totalModelFallbackCounts = new LinkedHashMap<>();
    private Map<String, Long> errorModelFallbackCounts = new LinkedHashMap<>();
    private Map<String, Long> totalModelParseFailureCounts = new LinkedHashMap<>();
    private Map<String, Long> errorModelParseFailureCounts = new LinkedHashMap<>();
}
