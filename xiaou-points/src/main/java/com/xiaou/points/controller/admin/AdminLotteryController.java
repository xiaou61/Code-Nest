package com.xiaou.points.controller.admin;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.points.cache.LotteryCacheWarmer;
import com.xiaou.points.domain.LotteryPrizeConfig;
import com.xiaou.points.dto.lottery.*;
import com.xiaou.points.dto.lottery.RiskUserQueryRequest;
import com.xiaou.points.dto.lottery.admin.*;
import com.xiaou.points.service.LotteryAnalysisService;
import com.xiaou.points.service.LotteryAdminService;
import com.xiaou.points.service.LotteryEmergencyService;
import com.xiaou.points.service.LotteryNormalizeService;
import com.xiaou.points.service.LotteryRiskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端抽奖控制器
 * 
 * @author xiaou
 */
@Validated
@RestController
@RequestMapping("/admin/lottery")
@RequiredArgsConstructor
public class AdminLotteryController {
    
    private final LotteryAdminService lotteryAdminService;
    private final LotteryNormalizeService normalizeService;
    private final LotteryRiskService riskService;
    private final LotteryAnalysisService analysisService;
    private final LotteryEmergencyService emergencyService;
    private final LotteryCacheWarmer cacheWarmer;
    
    /**
     * 保存奖品配置
     */
    @RequireAdmin(message = "需要管理员权限才能配置奖品")
    @PostMapping("/prize/save")
    public Result<String> savePrizeConfig(@Valid @RequestBody PrizeConfigSaveRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        lotteryAdminService.savePrizeConfig(request, adminId);
        return Result.success("保存成功");
    }
    
    /**
     * 获取奖品配置列表
     */
    @RequireAdmin(message = "需要管理员权限")
    @GetMapping("/prize/list")
    public Result<List<LotteryPrizeConfig>> getPrizeConfigList() {
        List<LotteryPrizeConfig> prizes = lotteryAdminService.getPrizeConfigList();
        return Result.success("获取成功", prizes);
    }
    
    /**
     * 启用/禁用奖品
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/prize/toggle-status")
    public Result<String> togglePrizeStatus(@RequestParam @Positive(message = "奖品ID必须大于0") Long prizeId,
                                            @RequestParam Boolean isActive) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        lotteryAdminService.togglePrizeStatus(prizeId, isActive, adminId);
        return Result.success(isActive ? "启用成功" : "禁用成功");
    }
    
    /**
     * 暂停/恢复奖品
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/prize/suspend")
    public Result<String> suspendPrize(@RequestParam @Positive(message = "奖品ID必须大于0") Long prizeId,
                                       @RequestParam(required = false) @Min(value = 0, message = "暂停分钟数不能小于0") Long suspendMinutes) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        lotteryAdminService.suspendPrize(prizeId, suspendMinutes, adminId);
        return Result.success(suspendMinutes == null || suspendMinutes <= 0 ? "恢复成功" : "暂停成功");
    }
    
    /**
     * 手动调整概率
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/prize/adjust-probability")
    public Result<String> adjustProbability(@Valid @RequestBody AdjustProbabilityRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        lotteryAdminService.adjustProbability(request, adminId);
        return Result.success("调整成功");
    }
    
    /**
     * 获取实时监控数据
     */
    @RequireAdmin(message = "需要管理员权限")
    @GetMapping("/monitor/realtime")
    public Result<RealtimeMonitorResponse> getRealtimeMonitor() {
        RealtimeMonitorResponse response = lotteryAdminService.getRealtimeMonitor();
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取单个奖品监控数据
     */
    @RequireAdmin(message = "需要管理员权限")
    @GetMapping("/monitor/prize/{prizeId}")
    public Result<PrizeMonitorResponse> getPrizeMonitor(@PathVariable @Positive(message = "奖品ID必须大于0") Long prizeId) {
        PrizeMonitorResponse response = lotteryAdminService.getPrizeMonitor(prizeId);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取所有抽奖记录
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/records")
    public Result<PageResult<LotteryDrawResponse>> getAllDrawRecords(@Valid @RequestBody LotteryRecordQueryRequest request) {
        PageResult<LotteryDrawResponse> response = lotteryAdminService.getAllDrawRecords(request);
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取历史统计数据
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/statistics/history")
    public Result<List<LotteryStatisticsResponse>> getHistoryStatistics(@Valid @RequestBody StatisticsQueryRequest request) {
        List<LotteryStatisticsResponse> response = lotteryAdminService.getHistoryStatistics(
            request.getStartDate(),
            request.getEndDate()
        );
        return Result.success("获取成功", response);
    }
    
    /**
     * 获取概率调整历史
     */
    @RequireAdmin(message = "需要管理员权限")
    @GetMapping("/adjust-history")
    public Result<PageResult<AdjustHistoryResponse>> getAdjustHistory(
            @RequestParam(required = false) @Positive(message = "奖品ID必须大于0") Long prizeId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于0") Integer size) {
        PageResult<AdjustHistoryResponse> response = lotteryAdminService.getAdjustHistory(prizeId, page, size);
        return Result.success("获取成功", response);
    }
    
    /**
     * 重置用户抽奖限制
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/user/reset-limit")
    public Result<String> resetUserLimit(@RequestParam @Positive(message = "用户ID必须大于0") Long userId) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        lotteryAdminService.resetUserLimit(userId, adminId);
        return Result.success("重置成功");
    }
    
    /**
     * 设置用户黑名单
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/user/blacklist")
    public Result<String> setUserBlacklist(@RequestParam @Positive(message = "用户ID必须大于0") Long userId,
                                           @RequestParam Boolean isBlacklist) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        lotteryAdminService.setUserBlacklist(userId, isBlacklist, adminId);
        return Result.success(isBlacklist ? "已加入黑名单" : "已移除黑名单");
    }
    
    /**
     * 归一化所有奖品概率
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/prize/normalize")
    public Result<BigDecimal> normalizeAllProbabilities() {
        BigDecimal sum = normalizeService.normalizeAllProbabilities();
        return Result.success("归一化成功，概率总和：" + sum, sum);
    }
    
    /**
     * 验证概率总和
     */
    @RequireAdmin(message = "需要管理员权限")
    @GetMapping("/prize/validate-probability")
    public Result<BigDecimal> validateProbabilitySum() {
        BigDecimal sum = normalizeService.validateProbabilitySum();
        boolean needsNorm = normalizeService.needsNormalization();
        String message = needsNorm ? "概率需要归一化，当前总和：" + sum : "概率正常，总和：" + sum;
        return Result.success(message, sum);
    }
    
    /**
     * 批量调整概率
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/prize/batch-adjust")
    public Result<String> batchAdjustProbability(@Valid @RequestBody BatchAdjustProbabilityRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        lotteryAdminService.batchAdjustProbability(request, adminId);
        return Result.success("批量调整成功");
    }
    
    /**
     * 批量启用/禁用奖品
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/prize/batch-toggle")
    public Result<String> batchToggleStatus(@Valid @RequestBody BatchToggleStatusRequest request) {
        Long adminId = StpAdminUtil.getLoginIdAsLong();
        lotteryAdminService.batchToggleStatus(request, adminId);
        return Result.success(request.getIsActive() ? "批量启用成功" : "批量禁用成功");
    }
    
    /**
     * 获取风险用户列表
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/user/risk-list")
    public Result<PageResult<com.xiaou.points.domain.UserLotteryLimit>> getRiskUserList(@Valid @RequestBody RiskUserQueryRequest request) {
        PageResult<com.xiaou.points.domain.UserLotteryLimit> result = riskService.getRiskUserList(request);
        return Result.success("获取成功", result);
    }
    
    /**
     * 评估用户风险等级
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/user/evaluate-risk")
    public Result<Integer> evaluateRiskLevel(@RequestParam @Positive(message = "用户ID必须大于0") Long userId) {
        Integer riskLevel = riskService.evaluateRiskLevel(userId);
        return Result.success("评估成功，风险等级：" + riskLevel, riskLevel);
    }
    
    /**
     * 检测异常行为
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/user/detect-abnormal")
    public Result<Boolean> detectAbnormalBehavior(@RequestParam @Positive(message = "用户ID必须大于0") Long userId) {
        boolean hasAbnormal = riskService.detectAbnormalBehavior(userId);
        return Result.success(hasAbnormal ? "检测到异常行为" : "行为正常", hasAbnormal);
    }
    
    /**
     * 刷新缓存
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/cache/refresh")
    public Result<String> refreshCache() {
        cacheWarmer.refreshCache();
        return Result.success("缓存刷新成功");
    }
    
    /**
     * 获取预警信息列表
     */
    @RequireAdmin(message = "需要管理员权限")
    @GetMapping("/monitor/alerts")
    public Result<List<AlertInfo>> getAlerts() {
        List<AlertInfo> alerts = lotteryAdminService.getAlerts();
        return Result.success("获取成功", alerts);
    }
    
    /**
     * 获取综合分析数据
     */
    @RequireAdmin(message = "需要管理员权限")
    @GetMapping("/analysis/comprehensive")
    public Result<AnalysisResponse> getComprehensiveAnalysis(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        AnalysisResponse analysis = analysisService.getComprehensiveAnalysis(startDate, endDate);
        return Result.success("获取成功", analysis);
    }
    
    /**
     * 手动熔断
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/emergency/circuit-break")
    public Result<String> manualCircuitBreak(@RequestParam @NotBlank(message = "熔断原因不能为空") String reason) {
        emergencyService.manualCircuitBreak(reason);
        return Result.success("熔断成功");
    }
    
    /**
     * 恢复服务
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/emergency/resume")
    public Result<String> resumeService() {
        emergencyService.resumeService();
        return Result.success("服务已恢复");
    }
    
    /**
     * 启用降级模式
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/emergency/degradation/enable")
    public Result<String> enableDegradation() {
        emergencyService.enableDegradation();
        return Result.success("降级模式已启用");
    }
    
    /**
     * 禁用降级模式
     */
    @RequireAdmin(message = "需要管理员权限")
    @PostMapping("/emergency/degradation/disable")
    public Result<String> disableDegradation() {
        emergencyService.disableDegradation();
        return Result.success("降级模式已禁用");
    }
}

