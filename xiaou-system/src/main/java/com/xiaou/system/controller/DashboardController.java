package com.xiaou.system.controller;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.system.dto.DashboardOverviewResponse;
import com.xiaou.system.service.SysDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端仪表板控制器
 *
 * @author xiaou
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "管理端仪表板", description = "管理端仪表板总览接口")
public class DashboardController {

    private final SysDashboardService dashboardService;

    /**
     * 获取仪表板总览数据
     */
    @Operation(summary = "获取仪表板总览数据")
    @RequireAdmin(message = "获取仪表板数据需要管理员权限")
    @GetMapping("/overview")
    public Result<DashboardOverviewResponse> getOverview() {
        return Result.success("获取仪表板数据成功", dashboardService.getOverview());
    }
}
