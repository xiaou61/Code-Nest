package com.xiaou.sre.dto.rca;

import java.time.LocalDateTime;

/**
 * RCA 调查的当前人工反馈。
 *
 * @author xiaou
 */
public record SreRcaFeedback(
        Long id,
        Long runId,
        String accuracy,
        String gapType,
        String note,
        String expectedConclusion,
        Long reviewedBy,
        LocalDateTime reviewedAt
) {
}
