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
        Provenance provenance,
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

    /**
     * 可安全展示的回放来源摘要。模型输入正文只保留在服务端，不通过详情接口返回。
     */
    public record Provenance(
            Long artifactId,
            String promptId,
            String schemaId,
            String provider,
            String configuredModel,
            String actualModel,
            String invocationOutcome,
            String contextSha256,
            int contextLength,
            boolean contextTruncated,
            LocalDateTime createdAt
    ) {
    }
}
