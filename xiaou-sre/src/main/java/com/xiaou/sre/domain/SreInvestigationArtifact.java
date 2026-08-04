package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一次 RCA 运行对应的不可变脱敏回放输入与来源信息。
 *
 * @author xiaou
 */
@Data
public class SreInvestigationArtifact {

    private Long id;
    private Long incidentId;
    private Long runId;
    private String contextJson;
    private String contextSha256;
    private Integer contextLength;
    private Boolean contextTruncated;
    private String promptId;
    private String schemaId;
    private String provider;
    private String configuredModel;
    private String actualModel;
    private String invocationOutcome;
    private LocalDateTime createdAt;
}
