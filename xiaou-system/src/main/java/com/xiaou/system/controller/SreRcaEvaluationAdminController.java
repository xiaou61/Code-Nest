package com.xiaou.system.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.system.dto.SreRcaEvaluationCaseSummary;
import com.xiaou.system.dto.SreRcaEvaluationPromotionRequest;
import com.xiaou.system.dto.SreRcaEvaluationRunDetail;
import com.xiaou.system.dto.SreRcaEvaluationRunRequest;
import com.xiaou.system.dto.SreRcaEvaluationRunSummary;
import com.xiaou.system.service.SreRcaEvaluationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RCA 离线评测管理员接口。
 *
 * @author xiaou
 */
@Validated
@RestController
@RequestMapping("/admin/sre")
@RequiredArgsConstructor
public class SreRcaEvaluationAdminController {

    private final SreRcaEvaluationService evaluationService;

    @PostMapping("/incidents/{id}/rca-runs/{runId}/evaluation-cases")
    @RequireAdmin(message = "提升 SRE RCA 评测用例需要管理员权限")
    @Log(module = "SRE RCA 评测", type = Log.OperationType.INSERT, description = "提升 RCA 评测用例",
            saveRequestData = false, saveResponseData = false)
    public Result<SreRcaEvaluationCaseSummary> promote(
            @PathVariable @Min(1) Long id,
            @PathVariable @Min(1) Long runId,
            @Valid @RequestBody SreRcaEvaluationPromotionRequest request) {
        return evaluationService.promote(
                        id, runId, request.feedbackId(), StpAdminUtil.getLoginIdAsLong())
                .map(Result::success)
                .orElseGet(() -> Result.error(
                        ResultCode.DATA_NOT_EXIST.getCode(), "RCA 运行或指定反馈修订不存在"));
    }

    @GetMapping("/rca-evaluations/cases")
    @RequireAdmin(message = "查询 SRE RCA 评测用例需要管理员权限")
    public Result<List<SreRcaEvaluationCaseSummary>> cases(
            @RequestParam(defaultValue = "50") @Min(1) Integer limit) {
        return Result.success(evaluationService.listCases(Math.min(limit, 100)));
    }

    @PostMapping("/rca-evaluations/runs")
    @RequireAdmin(message = "执行 SRE RCA 离线评测需要管理员权限")
    @Log(module = "SRE RCA 评测", type = Log.OperationType.OTHER, description = "手动执行 RCA 离线评测",
            saveRequestData = false, saveResponseData = false)
    public Result<SreRcaEvaluationRunDetail> run(
            @Valid @RequestBody(required = false) SreRcaEvaluationRunRequest request) {
        Long caseId = request == null ? null : request.caseId();
        return Result.success(evaluationService.run(caseId, StpAdminUtil.getLoginIdAsLong()));
    }

    @GetMapping("/rca-evaluations/runs")
    @RequireAdmin(message = "查询 SRE RCA 评测历史需要管理员权限")
    public Result<List<SreRcaEvaluationRunSummary>> runs(
            @RequestParam(defaultValue = "20") @Min(1) Integer limit) {
        return Result.success(evaluationService.listRuns(Math.min(limit, 50)));
    }

    @GetMapping("/rca-evaluations/runs/{runId}")
    @RequireAdmin(message = "查询 SRE RCA 评测详情需要管理员权限")
    public Result<SreRcaEvaluationRunDetail> runDetail(@PathVariable @Min(1) Long runId) {
        return evaluationService.getRun(runId)
                .map(Result::success)
                .orElseGet(() -> Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "RCA 评测运行不存在"));
    }
}
