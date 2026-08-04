package com.xiaou.system.dto;

import java.time.LocalDateTime;

/**
 * 不包含冻结模型上下文和基准报告正文的 RCA 评测用例摘要。
 *
 * @author xiaou
 */
public record SreRcaEvaluationCaseSummary(
        Long id,
        Long incidentId,
        Long sourceRunId,
        Long sourceArtifactId,
        Long sourceFeedbackId,
        String contextSha256,
        int contextLength,
        boolean contextTruncated,
        String expectedConclusion,
        String feedbackAccuracy,
        String feedbackGapType,
        String sourcePromptId,
        String sourceSchemaId,
        String sourceProvider,
        String sourceConfiguredModel,
        String sourceActualModel,
        String sourceInvocationOutcome,
        Long promotedBy,
        LocalDateTime promotedAt
) {
}
