package com.xiaou.system.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * AI 最近调用响应。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeRecentCallResponse {

    private String scene;
    private String promptKey;
    private String promptVersion;
    private String modelName;
    private String outcome;
    private long latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private long timestamp;
}
