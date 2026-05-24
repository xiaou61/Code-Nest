package com.xiaou.plan.controller.user;

import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.plan.dto.GrowthAutopilotDashboardResponse;
import com.xiaou.plan.dto.GrowthAutopilotGenerateRequest;
import com.xiaou.plan.dto.GrowthAutopilotReplanRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import com.xiaou.plan.service.GrowthAutopilotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 用户端成长闭环自动驾驶控制器
 *
 * @author xiaou
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user/plan/autopilot")
@RequiredArgsConstructor
public class UserGrowthAutopilotController {

    private final GrowthAutopilotService growthAutopilotService;

    /**
     * 获取自动驾驶看板
     */
    @GetMapping("/dashboard")
    public Result<GrowthAutopilotDashboardResponse> getDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart
    ) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success("获取成功", growthAutopilotService.getDashboard(userId, weekStart));
    }

    /**
     * 生成本周自动驾驶计划
     */
    @PostMapping("/generate")
    public Result<GrowthAutopilotDashboardResponse> generate(@Valid @RequestBody(required = false) GrowthAutopilotGenerateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success("生成成功", growthAutopilotService.generateWeeklyPlan(userId, request));
    }

    /**
     * 一键重排任务
     */
    @PostMapping("/replan")
    public Result<GrowthAutopilotDashboardResponse> replan(@Valid @RequestBody(required = false) GrowthAutopilotReplanRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success("重排成功", growthAutopilotService.replan(userId, request));
    }

    /**
     * 完成任务
     */
    @PostMapping("/tasks/{taskId}/complete")
    public Result<GrowthAutopilotDashboardResponse> completeTask(@Positive(message = "任务ID必须为正数") @PathVariable Long taskId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success("任务完成", growthAutopilotService.completeTask(userId, taskId));
    }

    /**
     * 批量完成今日任务
     */
    @PostMapping("/tasks/today/complete")
    public Result<GrowthAutopilotDashboardResponse> completeTodayTasks(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart
    ) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success("今日任务已批量完成", growthAutopilotService.completeTodayTasks(userId, weekStart));
    }

    /**
     * 任务顺延一天
     */
    @PostMapping("/tasks/{taskId}/postpone")
    public Result<GrowthAutopilotDashboardResponse> postponeTask(@Positive(message = "任务ID必须为正数") @PathVariable Long taskId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success("任务已顺延", growthAutopilotService.postponeTask(userId, taskId));
    }
}
