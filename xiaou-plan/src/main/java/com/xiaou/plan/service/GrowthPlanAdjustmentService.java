package com.xiaou.plan.service;

import com.xiaou.plan.dto.GrowthPlanAdjustmentApplyResult;
import com.xiaou.plan.dto.GrowthPlanAdjustmentCommand;
import com.xiaou.plan.dto.GrowthPlanAdjustmentPreview;

/**
 * 版本化的成长计划调整服务。
 */
public interface GrowthPlanAdjustmentService {

    GrowthPlanAdjustmentPreview preview(Long userId, GrowthPlanAdjustmentCommand command);

    GrowthPlanAdjustmentApplyResult apply(Long userId,
                                           GrowthPlanAdjustmentPreview preview,
                                           String actionRunId);
}
