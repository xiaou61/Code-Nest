package com.xiaou.sre.dto.request;

import java.math.BigDecimal;

/**
 * Persisted progress and heartbeat for a running evaluation.
 *
 * @author xiaou
 */
public record SreRcaEvaluationRunProgress(
        Long runId,
        int completedCount,
        int passedCount,
        int failedCount,
        BigDecimal averageScore,
        int unsafeCount,
        int degradedCount
) {
}
