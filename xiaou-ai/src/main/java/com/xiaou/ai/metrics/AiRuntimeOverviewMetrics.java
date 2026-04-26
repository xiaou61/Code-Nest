package com.xiaou.ai.metrics;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * AI 运行时总览指标。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeOverviewMetrics {

    private long totalInvocations;
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
