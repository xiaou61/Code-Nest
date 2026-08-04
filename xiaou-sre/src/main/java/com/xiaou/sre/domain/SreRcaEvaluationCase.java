package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 由管理员显式提升的不可变 RCA 离线评测用例。
 *
 * @author xiaou
 */
@Data
public class SreRcaEvaluationCase {

    private Long id;
    private Long incidentId;
    private Long sourceRunId;
    private Long sourceArtifactId;
    private Long sourceFeedbackId;
    private String contextJson;
    private String contextSha256;
    private Integer contextLength;
    private Boolean contextTruncated;
    private String baselineReportJson;
    private String expectedConclusion;
    private String feedbackAccuracy;
    private String feedbackGapType;
    private String sourcePromptId;
    private String sourceSchemaId;
    private String sourceProvider;
    private String sourceConfiguredModel;
    private String sourceActualModel;
    private String sourceInvocationOutcome;
    private Long promotedBy;
    private LocalDateTime promotedAt;
    private LocalDateTime createTime;
}
