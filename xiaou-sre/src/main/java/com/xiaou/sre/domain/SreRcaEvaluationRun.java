package com.xiaou.sre.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 一次管理员手动触发的 RCA 离线评测运行。
 *
 * @author xiaou
 */
@Data
public class SreRcaEvaluationRun {

    private Long id;
    private String status;
    private Long requestedCaseId;
    private Long requestedBy;
    private Integer caseCount;
    private Integer completedCount;
    private Integer passedCount;
    private Integer failedCount;
    private BigDecimal averageScore;
    private String promptId;
    private String schemaId;
    private String provider;
    private String configuredModel;
    private String failureCode;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
