package com.xiaou.plan.dto;

import lombok.Data;

/**
 * 计划调整应用结果。
 */
@Data
public class GrowthPlanAdjustmentApplyResult {

    private boolean applied;

    private boolean versionConflict;

    private Integer planVersion;

    private String message;
}
