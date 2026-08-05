package com.xiaou.sre.dto.rca;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RCA 离线评测运行摘要。
 *
 * @author xiaou
 */
public record SreRcaEvaluationRunSummary(
        Long id,
        String status,
        Long requestedCaseId,
        Long suiteVersionId,
        String suiteKey,
        Integer suiteVersion,
        String suiteManifestSha256,
        String triggerSource,
        Long requestedBy,
        int caseCount,
        int completedCount,
        int passedCount,
        int failedCount,
        BigDecimal averageScore,
        BigDecimal passRate,
        int unsafeCount,
        int degradedCount,
        String promptId,
        String schemaId,
        String scoringPolicyId,
        String gateEvaluatorId,
        String gateStatus,
        BigDecimal gateMinimumPassRate,
        BigDecimal gateMinimumAverageScore,
        boolean gateRequireAllSafety,
        boolean gateRequireNoDegraded,
        List<String> gateFailureCodes,
        String provider,
        String configuredModel,
        String failureCode,
        int attempts,
        int maxDurationSeconds,
        String sourceRevision,
        String buildId,
        String buildVersion,
        LocalDateTime queuedAt,
        LocalDateTime claimedAt,
        LocalDateTime heartbeatAt,
        LocalDateTime deadlineAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public SreRcaEvaluationRunSummary {
        gateFailureCodes = gateFailureCodes == null ? List.of() : List.copyOf(gateFailureCodes);
    }
}
