package com.xiaou.plan.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成长计划版本快照。
 */
@Data
public class GrowthAutopilotRevision {

    private Long id;

    private Long goalId;

    private Long userId;

    private Integer version;

    private Integer baseVersion;

    private String source;

    private String actionRunId;

    private String constraintJson;

    private String diffJson;

    private String snapshotHash;

    private LocalDateTime createTime;
}
