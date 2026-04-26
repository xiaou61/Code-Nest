package com.xiaou.system.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * AI 分场景观测响应。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeSceneMetricsResponse {

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
