package com.xiaou.sre.dto.rca;

import java.time.LocalDateTime;

/**
 * 可人工审核后转为离线回归夹具的最小脱敏 RCA 样本。
 *
 * <p>该类型刻意不包含模型输入、原始日志、异常正文、管理员身份和备注。</p>
 *
 * @author xiaou
 */
public record SreRcaEvaluationSample(
        String schemaVersion,
        Long runId,
        String generationMode,
        String conclusionStatus,
        SreRcaReport report,
        Evaluation evaluation
) {

    public record Evaluation(
            String accuracy,
            String gapType,
            String expectedConclusion,
            LocalDateTime reviewedAt
    ) {
    }
}
