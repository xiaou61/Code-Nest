package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SRE 调查运行中的单个阶段记录。
 *
 * @author xiaou
 */
@Data
public class SreInvestigationStep {

    private Long id;
    private Long runId;
    private Integer stepOrder;
    private String stepCode;
    private String status;
    private String detail;
    private LocalDateTime recordedAt;
    private LocalDateTime createTime;
}
