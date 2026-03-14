package com.xiaou.aigrowth.controller.admin;

import com.xiaou.aigrowth.dto.request.AiGrowthCoachConfigUpdateRequest;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachConfigResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachFailureResponse;
import com.xiaou.aigrowth.dto.response.AiGrowthCoachStatisticsResponse;
import com.xiaou.aigrowth.service.AiGrowthCoachService;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端AI成长教练控制器
 */
@Validated
@RestController
@RequestMapping("/admin/ai-growth-coach")
@RequiredArgsConstructor
public class AdminAiGrowthCoachController {

    private final AiGrowthCoachService aiGrowthCoachService;

    @GetMapping("/statistics")
    @RequireAdmin
    public Result<AiGrowthCoachStatisticsResponse> statistics() {
        return Result.success(aiGrowthCoachService.getStatistics());
    }

    @GetMapping("/failures")
    @RequireAdmin
    public Result<List<AiGrowthCoachFailureResponse>> failures() {
        return Result.success(aiGrowthCoachService.getFailures());
    }

    @GetMapping("/config")
    @RequireAdmin
    public Result<List<AiGrowthCoachConfigResponse>> config() {
        return Result.success(aiGrowthCoachService.getConfigs());
    }

    @PutMapping("/config")
    @RequireAdmin
    public Result<Void> updateConfig(@Valid @RequestBody List<AiGrowthCoachConfigUpdateRequest> requests) {
        aiGrowthCoachService.updateConfigs(requests);
        return Result.success();
    }
}
