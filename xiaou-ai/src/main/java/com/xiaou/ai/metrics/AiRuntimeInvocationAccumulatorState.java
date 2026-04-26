package com.xiaou.ai.metrics;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 运行观测累计器持久化状态。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRuntimeInvocationAccumulatorState {

    private long invocations;
    private long totalLatencyMs;
    private long totalInputTokens;
    private long totalOutputTokens;
    private long totalTokens;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private String lastModelName = "-";
    private Long lastInvocationAt;
}
