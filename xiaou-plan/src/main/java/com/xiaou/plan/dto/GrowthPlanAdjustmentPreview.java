package com.xiaou.plan.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 可确认的计划调整预览。
 */
@Data
public class GrowthPlanAdjustmentPreview {

    private Long goalId;

    private LocalDate weekStart;

    private Integer basePlanVersion;

    private Integer budgetMinutes;

    private Integer plannedMinutes;

    private String targetRole;

    private boolean completedTasksPreserved;

    private boolean allTasksResourceBacked;

    private List<GrowthPlanTaskChange> changes = new ArrayList<>();
}
