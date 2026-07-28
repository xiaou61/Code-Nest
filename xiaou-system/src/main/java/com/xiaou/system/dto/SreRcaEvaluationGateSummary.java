package com.xiaou.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Narrow machine-readable gate response without candidate reports or case payloads.
 *
 * @author xiaou
 */
public record SreRcaEvaluationGateSummary(
        Long runId,
        String runStatus,
        String gateStatus,
        Long suiteVersionId,
        String suiteKey,
        Integer suiteVersion,
        String suiteManifestSha256,
        String triggerSource,
        int caseCount,
        int completedCount,
        int passedCount,
        int failedCount,
        BigDecimal averageScore,
        BigDecimal passRate,
        int unsafeCount,
        int degradedCount,
        BigDecimal minimumPassRate,
        BigDecimal minimumAverageScore,
        boolean requireAllSafety,
        boolean requireNoDegraded,
        List<String> failureCodes,
        String promptId,
        String schemaId,
        String provider,
        String configuredModel,
        String sourceRevision,
        String buildId,
        String buildVersion,
        int attempts,
        int maxDurationSeconds,
        LocalDateTime deadlineAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public SreRcaEvaluationGateSummary {
        failureCodes = failureCodes == null ? List.of() : List.copyOf(failureCodes);
    }
}
