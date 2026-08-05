package com.xiaou.sre.dto.rca;

import java.time.LocalDateTime;

/**
 * 已持久化 RCA 调查运行摘要，不包含报告正文。
 *
 * @author xiaou
 */
public record SreRcaRunSummary(
        Long id,
        Long incidentId,
        String status,
        String triggerSource,
        Long requestedBy,
        String generationMode,
        String conclusionStatus,
        int alertCount,
        int evidenceCount,
        boolean contextTruncated,
        String failureCode,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
