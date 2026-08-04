package com.xiaou.plan.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 已规范化的计划调整约束。
 */
@Data
public class GrowthPlanAdjustmentCommand {

    private LocalDate weekStart;

    private Integer availableMinutes;

    private String targetRole;

    private boolean prioritizeInterview;

    /**
     * 由已验证成长证据推导出的模块偏好，只影响已有任务的排序。
     */
    private List<String> preferredModuleKeys = new ArrayList<>();
}
