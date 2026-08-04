package com.xiaou.web.growthcoach.controller;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.web.growthcoach.dto.GrowthAnalyticsOverviewResponse;
import com.xiaou.web.growthcoach.service.GrowthAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端成长业务漏斗接口。
 */
@Validated
@RestController
@RequestMapping("/admin/growth-analytics")
@RequiredArgsConstructor
@Tag(name = "成长业务分析", description = "Growth Coach 主行动与求职闭环业务指标")
public class AdminGrowthAnalyticsController {

    private final GrowthAnalyticsService growthAnalyticsService;

    @Operation(summary = "获取成长业务漏斗总览")
    @RequireAdmin(message = "获取成长业务分析需要管理员权限")
    @GetMapping("/overview")
    public Result<GrowthAnalyticsOverviewResponse> getOverview(
            @Min(value = 1, message = "统计天数不能小于1")
            @Max(value = 90, message = "统计天数不能超过90")
            @RequestParam(defaultValue = "7") Integer days
    ) {
        return Result.success(growthAnalyticsService.getOverview(days));
    }
}
