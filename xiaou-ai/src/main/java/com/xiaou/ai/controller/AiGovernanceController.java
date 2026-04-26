package com.xiaou.ai.controller;

import com.xiaou.ai.dto.governance.AiGovernanceOverviewResponse;
import com.xiaou.ai.service.AiGovernanceService;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI运行治理控制器
 *
 * @author xiaou
 */
@RestController
@RequestMapping("/admin/ai/governance")
@RequiredArgsConstructor
@Tag(name = "AI运行治理", description = "AI工作流治理总览")
public class AiGovernanceController {

    private final AiGovernanceService aiGovernanceService;

    @Operation(summary = "获取AI治理总览")
    @RequireAdmin(message = "查看AI治理总览需要管理员权限")
    @GetMapping("/overview")
    public Result<AiGovernanceOverviewResponse> overview() {
        return Result.success(aiGovernanceService.getOverview());
    }
}
