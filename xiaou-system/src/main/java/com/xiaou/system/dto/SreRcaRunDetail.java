package com.xiaou.system.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 可恢复查看的 RCA 报告及调查步骤。
 *
 * @author xiaou
 */
public record SreRcaRunDetail(
        SreRcaRunSummary run,
        List<Step> steps,
        SreRcaReport report,
        SreRcaFeedback feedback
) {

    public SreRcaRunDetail {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public record Step(
            Long id,
            int order,
            String code,
            String status,
            String detail,
            LocalDateTime recordedAt
    ) {
    }
}
