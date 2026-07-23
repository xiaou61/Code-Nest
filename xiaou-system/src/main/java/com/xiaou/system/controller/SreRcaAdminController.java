package com.xiaou.system.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.system.dto.SreRcaReport;
import com.xiaou.system.service.SreIncidentRcaService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return rcaService.investigate(id)
                .map(Result::success)
                .orElseGet(() -> Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "事故不存在"));
    }
}
