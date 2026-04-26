package com.xiaou.ai.metrics;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * AI 运行时分场景统计。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeSceneMetrics {

    private String scene;
    private String promptKey;
    private String promptVersion;
    private String lastModelName;
    private long invocations;
    private long successCount;
    private long errorCount;
    private long fallbackCount;
    private long structuredParseFailureCount;
    private long totalInputTokens;
    private long totalOutputTokens;
    private long totalTokens;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private long averageLatencyMs;
    private Long lastInvocationAt;
}
