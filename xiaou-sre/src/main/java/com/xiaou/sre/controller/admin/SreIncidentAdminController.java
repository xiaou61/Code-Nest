package com.xiaou.sre.controller.admin;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreIncidentEvidence;
import com.xiaou.sre.dto.request.SreIncidentQuery;
import com.xiaou.sre.dto.response.SreInvestigationContext;
import com.xiaou.sre.dto.response.SreIncidentSummary;
import com.xiaou.sre.service.SreIncidentEvidenceService;
import com.xiaou.sre.service.SreIncidentService;
import com.xiaou.sre.service.SreInvestigationFacade;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
 * SRE 事故管理接口。
 *
 * @author xiaou
 */
@Validated
@RestController
@RequestMapping("/admin/sre/incidents")
@RequiredArgsConstructor
public class SreIncidentAdminController {

    private final SreIncidentService incidentService;
    private final SreIncidentEvidenceService evidenceService;
    private final SreInvestigationFacade investigationFacade;

    @GetMapping("/summary")
    @RequireAdmin(message = "查询 SRE 事故汇总需要管理员权限")
    public Result<SreIncidentSummary> summary() {
        return Result.success(incidentService.summary());
    }

    @GetMapping
    @RequireAdmin(message = "查询 SRE 事故需要管理员权限")
    public Result<PageResult<SreIncident>> list(
            @RequestParam(required = false) @Size(max = 32) String state,
            @RequestParam(required = false) @Size(max = 32) String severity,
            @RequestParam(required = false) @Size(max = 100) String service,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) Integer pageSize) {
        SreIncidentQuery query = new SreIncidentQuery();
        query.setState(state);
        query.setSeverity(severity);
        query.setService(service);
        query.setPageNum(pageNum);
        query.setPageSize(Math.min(pageSize, 100));
        return Result.success(incidentService.list(query));
    }

    @GetMapping("/{id}")
    @RequireAdmin(message = "查询 SRE 事故需要管理员权限")
    public Result<SreIncident> getById(@PathVariable @Min(1) Long id) {
        SreIncident incident = incidentService.getById(id);
        if (incident == null) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "事故不存在");
        }
        return Result.success(incident);
    }

    @GetMapping("/{id}/evidence")
    @RequireAdmin(message = "查询 SRE 事故证据需要管理员权限")
    public Result<List<SreIncidentEvidence>> evidence(@PathVariable @Min(1) Long id) {
        if (incidentService.getById(id) == null) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "事故不存在");
        }
        return Result.success(evidenceService.listByIncidentId(id));
    }

    @GetMapping("/{id}/investigation-context")
    @RequireAdmin(message = "查询 SRE 事故调查上下文需要管理员权限")
    public Result<SreInvestigationContext> investigationContext(@PathVariable @Min(1) Long id) {
        return investigationFacade.findByIncidentId(id)
                .map(Result::success)
                .orElseGet(() -> Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "事故不存在"));
    }

    @PostMapping("/{id}/ack")
    @RequireAdmin(message = "确认 SRE 事故需要管理员权限")
    @Log(module = "SRE 事故", type = Log.OperationType.UPDATE, description = "确认 SRE 事故",
            saveRequestData = false, saveResponseData = false)
    public Result<Void> acknowledge(@PathVariable @Min(1) Long id) {
        boolean success = incidentService.acknowledge(id, StpAdminUtil.getLoginIdAsLong());
        return success ? Result.success() : Result.error(ResultCode.OPERATION_NOT_ALLOWED.getCode(), "事故不存在或当前状态不可确认");
    }

    @PostMapping("/{id}/resolve")
    @RequireAdmin(message = "关闭 SRE 事故需要管理员权限")
    @Log(module = "SRE 事故", type = Log.OperationType.UPDATE, description = "关闭 SRE 事故",
            saveRequestData = false, saveResponseData = false)
    public Result<Void> resolve(@PathVariable @Min(1) Long id) {
        boolean success = incidentService.resolve(id, StpAdminUtil.getLoginIdAsLong());
        return success ? Result.success() : Result.error(ResultCode.OPERATION_NOT_ALLOWED.getCode(), "事故不存在或当前状态不可关闭");
    }
}
