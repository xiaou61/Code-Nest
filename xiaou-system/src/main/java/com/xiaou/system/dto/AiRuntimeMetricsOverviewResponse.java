package com.xiaou.system.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * AI 运行观测总览。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeMetricsOverviewResponse {

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
    private String currency;
    private boolean pricingConfigured;
}
