package com.xiaou.plan.service;

import com.xiaou.plan.dto.GrowthAutopilotDashboardResponse;
import com.xiaou.plan.dto.GrowthAutopilotGenerateRequest;
import com.xiaou.plan.dto.GrowthAutopilotReplanRequest;

import java.time.LocalDate;

/**
 * 成长闭环自动驾驶服务
 *
 * @author xiaou
 */
public interface GrowthAutopilotService {

    GrowthAutopilotDashboardResponse getDashboard(Long userId, LocalDate weekStart);

    GrowthAutopilotDashboardResponse generateWeeklyPlan(Long userId, GrowthAutopilotGenerateRequest request);

    GrowthAutopilotDashboardResponse completeTask(Long userId, Long taskId);

    GrowthAutopilotDashboardResponse completeTodayTasks(Long userId, LocalDate weekStart);

    GrowthAutopilotDashboardResponse postponeTask(Long userId, Long taskId);

    GrowthAutopilotDashboardResponse replan(Long userId, GrowthAutopilotReplanRequest request);
}
