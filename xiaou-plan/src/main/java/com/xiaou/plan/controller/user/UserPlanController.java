package com.xiaou.plan.controller.user;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.plan.domain.PlanCheckinRecord;
import com.xiaou.plan.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import com.xiaou.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端计划控制器
 * 
 * @author xiaou
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user/plan")
@RequiredArgsConstructor
public class UserPlanController {
    
    private final PlanService planService;
    
    /**
     * 创建计划
     */
    @PostMapping("/create")
    public Result<PlanResponse> createPlan(@Valid @RequestBody PlanCreateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        PlanResponse response = planService.createPlan(userId, request);
        return Result.success("创建成功", response);
    }
    
    /**
     * 更新计划
     */
    @PutMapping("/update/{planId}")
    public Result<PlanResponse> updatePlan(
            @Positive(message = "计划ID必须为正数") @PathVariable Long planId,
            @Valid @RequestBody PlanCreateRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        PlanResponse response = planService.updatePlan(userId, planId, request);
        return Result.success("更新成功", response);
    }
    
    /**
     * 删除计划
     */
    @DeleteMapping("/{planId}")
    public Result<Boolean> deletePlan(@Positive(message = "计划ID必须为正数") @PathVariable Long planId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = planService.deletePlan(userId, planId);
        return success ? Result.success("删除成功", true) : Result.error("删除失败");
    }
    
    /**
     * 获取计划详情
     */
    @GetMapping("/{planId}")
    public Result<PlanResponse> getPlanDetail(@Positive(message = "计划ID必须为正数") @PathVariable Long planId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        PlanResponse response = planService.getPlanDetail(userId, planId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取计划列表
     */
    @PostMapping("/list")
    public Result<PageResult<PlanResponse>> getPlanList(@Valid @RequestBody PlanListRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        request.setUserId(userId);
        PageResult<PlanResponse> response = planService.getPlanList(request);
        return Result.success("获取成功", response);
    }
    
    /**
     * 暂停计划
     */
    @PutMapping("/{planId}/pause")
    public Result<Boolean> pausePlan(@Positive(message = "计划ID必须为正数") @PathVariable Long planId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = planService.pausePlan(userId, planId);
        return success ? Result.success("暂停成功", true) : Result.error("暂停失败");
    }
    
    /**
     * 恢复计划
     */
    @PutMapping("/{planId}/resume")
    public Result<Boolean> resumePlan(@Positive(message = "计划ID必须为正数") @PathVariable Long planId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean success = planService.resumePlan(userId, planId);
        return success ? Result.success("恢复成功", true) : Result.error("恢复失败");
    }
    
    /**
     * 获取今日任务列表
     */
    @GetMapping("/today-tasks")
    public Result<List<TodayTaskResponse>> getTodayTasks() {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        List<TodayTaskResponse> tasks = planService.getTodayTasks(userId);
        return Result.success("获取成功", tasks);
    }
    
    /**
     * 执行打卡
     */
    @PostMapping("/checkin")
    public Result<PlanCheckinResponse> checkin(@Valid @RequestBody PlanCheckinRequest request) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        PlanCheckinResponse response = planService.checkin(userId, request);
        return Result.success("打卡成功", response);
    }
    
    /**
     * 获取计划打卡记录
     */
    @GetMapping("/{planId}/checkin/list")
    public Result<List<PlanCheckinRecord>> getCheckinRecords(@Positive(message = "计划ID必须为正数") @PathVariable Long planId) {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        List<PlanCheckinRecord> records = planService.getCheckinRecords(userId, planId);
        return Result.success("获取成功", records);
    }
    
    /**
     * 获取统计概览
     */
    @GetMapping("/stats/overview")
    public Result<PlanStatsResponse> getStatsOverview() {
        StpUserUtil.checkLogin();
        Long userId = StpUserUtil.getLoginIdAsLong();
        PlanStatsResponse response = planService.getStatsOverview(userId);
        return Result.success("获取成功", response);
    }
}
