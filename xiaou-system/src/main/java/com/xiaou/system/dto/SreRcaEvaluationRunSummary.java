package com.xiaou.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RCA 离线评测运行摘要。
 *
 * @author xiaou
 */
public record SreRcaEvaluationRunSummary(
        Long id,
        String status,
        Long requestedCaseId,
        Long requestedBy,
        int caseCount,
        int completedCount,
        int passedCount,
        int failedCount,
        BigDecimal averageScore,
        String promptId,
        String schemaId,
        String provider,
        String configuredModel,
        String failureCode,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
