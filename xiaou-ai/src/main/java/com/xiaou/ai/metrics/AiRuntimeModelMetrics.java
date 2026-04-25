package com.xiaou.ai.metrics;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * AI 按模型聚合的运行观测。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeModelMetrics {

    private String modelName;
    private long invocations;
    private long successCount;
    private long errorCount;
    private long totalInputTokens;
    private long totalOutputTokens;
    private long totalTokens;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private long averageLatencyMs;
    private Long lastInvocationAt;
}
