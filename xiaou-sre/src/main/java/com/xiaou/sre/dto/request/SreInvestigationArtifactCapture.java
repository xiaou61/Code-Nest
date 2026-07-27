package com.xiaou.sre.dto.request;

/**
 * 固化一次调查脱敏模型输入及其运行来源的内部命令。
 *
 * @author xiaou
 */
public record SreInvestigationArtifactCapture(
        Long incidentId,
        Long runId,
        String contextJson,
        boolean contextTruncated,
        String promptId,
        String schemaId,
        String provider,
        String configuredModel,
        String actualModel,
        String invocationOutcome
) {
}
