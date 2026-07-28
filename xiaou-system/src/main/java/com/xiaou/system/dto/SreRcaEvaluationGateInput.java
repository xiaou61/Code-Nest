package com.xiaou.system.dto;

import java.math.BigDecimal;

/**
 * Aggregate facts and frozen policy used by the deterministic quality gate.
 *
 * @author xiaou
 */
public record SreRcaEvaluationGateInput(
        boolean applicable,
        int expectedCaseCount,
        int completedCount,
        int passedCount,
        BigDecimal averageScore,
        int unsafeCount,
        int degradedCount,
        BigDecimal minimumPassRate,
        BigDecimal minimumAverageScore,
        boolean requireAllSafety,
        boolean requireNoDegraded
) {
}
