package com.xiaou.sre.controller.admin;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.sre.dto.rca.SreRcaReport;
import com.xiaou.sre.dto.rca.SreRcaEvaluationSample;
import com.xiaou.sre.dto.rca.SreRcaFeedback;
import com.xiaou.sre.dto.rca.SreRcaFeedbackRequest;
import com.xiaou.sre.dto.rca.SreRcaRunDetail;
import com.xiaou.sre.dto.rca.SreRcaRunSummary;
import com.xiaou.sre.service.rca.SreIncidentRcaService;
import com.xiaou.sre.service.rca.SreRcaTriggerSource;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SRE 根因分析管理员接口。
 *
 * @author xiaou
 */
@Validated
@RestController
@RequestMapping("/admin/sre/incidents")
@RequiredArgsConstructor
public class SreRcaAdminController {

    private final SreIncidentRcaService rcaService;

    @PostMapping("/{id}/rca")
    @RequireAdmin(message = "执行 SRE 事故只读分析需要管理员权限")
    @Log(module = "SRE 事故", type = Log.OperationType.OTHER, description = "生成 SRE 事故只读 RCA",
            saveRequestData = false, saveResponseData = false)
    public Result<SreRcaReport> investigate(@PathVariable @Min(1) Long id) {
        return rcaService.investigate(id, SreRcaTriggerSource.ADMIN_API, StpAdminUtil.getLoginIdAsLong())
                .map(Result::success)
                .orElseGet(() -> Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "事故不存在"));
    }

    @GetMapping("/{id}/rca-runs")
    @RequireAdmin(message = "查询 SRE RCA 历史需要管理员权限")
    public Result<List<SreRcaRunSummary>> history(
            @PathVariable @Min(1) Long id,
            @RequestParam(defaultValue = "10") @Min(1) Integer limit) {
        return Result.success(rcaService.listRuns(id, Math.min(limit, 50)));
    }

    @GetMapping("/{id}/rca-runs/{runId}")
    @RequireAdmin(message = "查询 SRE RCA 详情需要管理员权限")
    public Result<SreRcaRunDetail> runDetail(
            @PathVariable @Min(1) Long id,
            @PathVariable @Min(1) Long runId) {
        return rcaService.getRun(id, runId)
                .map(Result::success)
                .orElseGet(() -> Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "RCA 调查记录不存在"));
    }

    @PutMapping("/{id}/rca-runs/{runId}/feedback")
    @RequireAdmin(message = "评价 SRE RCA 需要管理员权限")
    @Log(module = "SRE 事故", type = Log.OperationType.UPDATE, description = "评价 SRE RCA",
            saveRequestData = false, saveResponseData = false)
    public Result<SreRcaFeedback> saveFeedback(
            @PathVariable @Min(1) Long id,
            @PathVariable @Min(1) Long runId,
            @Valid @RequestBody SreRcaFeedbackRequest request) {
        return rcaService.saveFeedback(id, runId, request, StpAdminUtil.getLoginIdAsLong())
                .map(Result::success)
                .orElseGet(() -> Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "RCA 调查记录不存在"));
    }

    @GetMapping("/{id}/rca-runs/{runId}/evaluation-sample")
    @RequireAdmin(message = "导出 SRE RCA 评测样本需要管理员权限")
    public Result<SreRcaEvaluationSample> evaluationSample(
            @PathVariable @Min(1) Long id,
            @PathVariable @Min(1) Long runId) {
        return rcaService.getEvaluationSample(id, runId)
                .map(Result::success)
                .orElseGet(() -> Result.error(
                        ResultCode.DATA_NOT_EXIST.getCode(), "RCA 评测样本不存在，请先完成反馈"));
    }
}
