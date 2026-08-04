package com.xiaou.sre.dto.request;

import java.math.BigDecimal;

/**
 * Aggregated terminal state and quality-gate decision for one replay run.
 *
 * @author xiaou
 */
public record SreRcaEvaluationRunCompletion(
        Long runId,
        String status,
        int completedCount,
        int passedCount,
        int failedCount,
        BigDecimal averageScore,
        BigDecimal passRate,
        int unsafeCount,
        int degradedCount,
        String gateStatus,
        String gateDetailJson,
        String provider,
        String configuredModel
) {
}
