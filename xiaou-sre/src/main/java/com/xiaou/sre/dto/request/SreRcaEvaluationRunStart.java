package com.xiaou.sre.dto.request;

import java.math.BigDecimal;

/**
 * Auditable start snapshot for an ad-hoc or versioned-suite replay.
 *
 * @author xiaou
 */
public record SreRcaEvaluationRunStart(
        Long requestedCaseId,
        Long suiteVersionId,
        String suiteKey,
        Integer suiteVersion,
        String suiteManifestSha256,
        String triggerSource,
        String scoringPolicyId,
        String gateEvaluatorId,
        BigDecimal gateMinimumPassRate,
        BigDecimal gateMinimumAverageScore,
        Boolean gateRequireAllSafety,
        Boolean gateRequireNoDegraded,
        Long requestedBy,
        int caseCount,
        String promptId,
        String schemaId
) {
}
