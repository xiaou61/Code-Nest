package com.xiaou.system.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * AI 分模型观测响应。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeModelMetricsResponse {

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
