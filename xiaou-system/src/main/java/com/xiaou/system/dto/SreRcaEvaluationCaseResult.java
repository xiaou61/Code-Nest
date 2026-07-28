package com.xiaou.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个 RCA 评测用例的候选报告、来源与透明评分。
 *
 * @author xiaou
 */
public record SreRcaEvaluationCaseResult(
        Long id,
        Long runId,
        Long caseId,
        String status,
        SreRcaReport candidateReport,
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
        List<String> explanations,
        String failureCode,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public SreRcaEvaluationCaseResult {
        explanations = explanations == null ? List.of() : List.copyOf(explanations);
    }
}
