package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一次可审计、可恢复查看的 SRE 调查运行。
 *
 * @author xiaou
 */
@Data
public class SreInvestigationRun {

    private Long id;
    private Long incidentId;
    private String status;
    private String triggerSource;
    private Long requestedBy;
    private String generationMode;
    private String conclusionStatus;
    private Integer alertCount;
    private Integer evidenceCount;
    private Boolean contextTruncated;
    private String reportJson;
    private String failureCode;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
