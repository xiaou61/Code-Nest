package com.xiaou.sre.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单个 RCA 评测结果的持久化输入。
 *
 * @author xiaou
 */
public record SreRcaEvaluationResultCapture(
        Long runId,
        Long caseId,
        String status,
        String candidateReportJson,
        String promptId,
        String schemaId,
        String provider,
        String configuredModel,
        String actualModel,
        String invocationOutcome,
        BigDecimal conclusionSimilarity,
        BigDecimal evidenceRecall,
        boolean severityMatched,
        boolean safetyCompliant,
        BigDecimal totalScore,
        boolean passed,
        String scoreDetailJson,
        String failureCode,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
