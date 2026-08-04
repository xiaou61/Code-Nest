package com.xiaou.plan.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 一个已有任务在计划调整中的确定性变化。
 */
@Data
public class GrowthPlanTaskChange {

    private Long taskId;

    private String title;

    private String moduleName;

    /** KEEP / MOVE / SUPERSEDE */
    private String operation;

    private LocalDate fromDate;

    private LocalDate toDate;

    private Integer plannedMinutes;

    private String reason;

    private String resourceType;

    private String resourceId;

    private String resourceVersion;

    private String routePath;
}
