package com.xiaou.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable suite version provenance and frozen gate policy.
 *
 * @author xiaou
 */
public record SreRcaEvaluationSuiteVersionSummary(
        Long id,
        Long suiteId,
        int version,
        int caseCount,
        String manifestSha256,
        String manifestSchemaId,
        String scoringPolicyId,
        String gateEvaluatorId,
        BigDecimal minimumPassRate,
        BigDecimal minimumAverageScore,
        boolean requireAllSafety,
        boolean requireNoDegraded,
        Long publishedBy,
        LocalDateTime publishedAt
) {
}
