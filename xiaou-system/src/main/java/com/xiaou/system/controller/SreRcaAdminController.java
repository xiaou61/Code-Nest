package com.xiaou.system.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.system.dto.SreRcaReport;
import com.xiaou.system.dto.SreRcaRunDetail;
import com.xiaou.system.dto.SreRcaRunSummary;
import com.xiaou.system.service.SreIncidentRcaService;
import com.xiaou.system.service.SreRcaTriggerSource;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
