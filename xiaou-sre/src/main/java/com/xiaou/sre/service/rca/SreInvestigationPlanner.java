package com.xiaou.sre.service.rca;

/**
 * 生成固定只读工具调查计划的边界。
 *
 * @author xiaou
 */
public interface SreInvestigationPlanner {

    SreInvestigationPlan plan(SreInvestigationPlanningInput input);
}
