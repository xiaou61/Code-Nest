package com.xiaou.system.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentOperatorResolver;
import com.xiaou.system.agent.task.AgentTaskCycleRuntime;
import com.xiaou.system.agent.task.AgentTaskStateService;
import com.xiaou.system.dto.AgentTaskCancelRequest;
import com.xiaou.system.dto.AgentTaskConfirmRequest;
import com.xiaou.system.dto.AgentTaskCreateRequest;
import com.xiaou.system.dto.AgentTaskEventPageResponse;
import com.xiaou.system.dto.AgentTaskInputRequest;
import com.xiaou.system.dto.AgentTaskPauseRequest;
import com.xiaou.system.dto.AgentTaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * Owner-scoped HTTP surface for durable administrator-agent tasks.
 */
@Validated
@RestController
@RequestMapping("/admin/agent/tasks")
@RequiredArgsConstructor
@Tag(name = "管理员智能体任务", description = "创建、检查、确认和取消持久化多步骤管理员智能体任务")
public class AgentTaskController {

    private final AgentTaskStateService stateService;
    private final AgentTaskCycleRuntime cycleRuntime;
    private final AgentOperatorResolver operatorResolver;

    @Operation(summary = "创建管理员智能体任务")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "创建管理员智能体任务需要管理员权限")
    @PostMapping
    @Log(module = "系统管理", type = Log.OperationType.INSERT, description = "创建管理员智能体任务",
            saveRequestData = false, saveResponseData = false)
    public Result<AgentTaskResponse> create(@Valid @RequestBody AgentTaskCreateRequest request) {
        AgentOperator operator = currentOperator();
        return Result.success("管理员智能体任务已创建", stateService.create(request, operator));
    }

    @Operation(summary = "查询当前管理员的智能体任务")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查询管理员智能体任务需要管理员权限")
    @GetMapping
    public Result<List<AgentTaskResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success("获取管理员智能体任务成功",
                stateService.listOwned(StpAdminUtil.getLoginIdAsLong(), status, limit));
    }

    @Operation(summary = "查询当前管理员的智能体任务详情")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查询管理员智能体任务需要管理员权限")
    @GetMapping("/{taskId}")
    public Result<AgentTaskResponse> detail(@PathVariable String taskId) {
        return Result.success("获取管理员智能体任务详情成功",
                stateService.detail(taskId, StpAdminUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "查询管理员智能体任务事件")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查询管理员智能体任务事件需要管理员权限")
    @GetMapping("/{taskId}/events")
    public Result<AgentTaskEventPageResponse> events(
            @PathVariable String taskId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success("获取管理员智能体任务事件成功",
                stateService.events(taskId, StpAdminUtil.getLoginIdAsLong(), after, limit));
    }

    @Operation(summary = "暂停管理员智能体任务")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "暂停管理员智能体任务需要管理员权限")
    @PostMapping("/{taskId}/pause")
    @Log(module = "系统管理", type = Log.OperationType.UPDATE, description = "暂停管理员智能体任务",
            saveRequestData = false, saveResponseData = false)
    public Result<AgentTaskResponse> pause(
            @PathVariable String taskId,
            @Valid @RequestBody(required = false) AgentTaskPauseRequest request
    ) {
        return Result.success("管理员智能体任务已暂停",
                stateService.pause(taskId, StpAdminUtil.getLoginIdAsLong(), request));
    }

    @Operation(summary = "恢复管理员智能体任务")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "恢复管理员智能体任务需要管理员权限")
    @PostMapping("/{taskId}/resume")
    @Log(module = "系统管理", type = Log.OperationType.UPDATE, description = "恢复管理员智能体任务",
            saveRequestData = false, saveResponseData = false)
    public Result<AgentTaskResponse> resume(@PathVariable String taskId) {
        return Result.success("管理员智能体任务已恢复",
                stateService.resume(taskId, StpAdminUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "提交管理员智能体任务补充输入")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "提交管理员智能体任务输入需要管理员权限")
    @PostMapping("/{taskId}/input")
    @Log(module = "系统管理", type = Log.OperationType.UPDATE, description = "提交管理员智能体任务补充输入",
            saveRequestData = false, saveResponseData = false)
    public Result<AgentTaskResponse> submitInput(
            @PathVariable String taskId,
            @Valid @RequestBody AgentTaskInputRequest request
    ) {
        return Result.success("管理员智能体任务补充输入已提交",
                stateService.submitInput(taskId, StpAdminUtil.getLoginIdAsLong(), request));
    }

    @Operation(summary = "确认管理员智能体任务的待确认写步骤")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "确认管理员智能体任务需要管理员权限")
    @PostMapping("/{taskId}/confirm")
    @Log(module = "系统管理", type = Log.OperationType.UPDATE, description = "确认管理员智能体任务写步骤",
            saveRequestData = false, saveResponseData = false)
    public Result<AgentTaskResponse> confirm(
            @PathVariable String taskId,
            @Valid @RequestBody AgentTaskConfirmRequest request
    ) {
        return Result.success("管理员智能体任务确认已处理",
                cycleRuntime.confirm(taskId, currentOperator(), request.getConfirmationText()));
    }

    @Operation(summary = "取消管理员智能体任务")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "取消管理员智能体任务需要管理员权限")
    @PostMapping("/{taskId}/cancel")
    @Log(module = "系统管理", type = Log.OperationType.UPDATE, description = "取消管理员智能体任务",
            saveRequestData = false, saveResponseData = false)
    public Result<AgentTaskResponse> cancel(
            @PathVariable String taskId,
            @Valid @RequestBody AgentTaskCancelRequest request
    ) {
        return Result.success("管理员智能体任务已取消",
                stateService.cancel(taskId, StpAdminUtil.getLoginIdAsLong(), request));
    }

    private AgentOperator currentOperator() {
        return operatorResolver.resolve(StpAdminUtil.getLoginIdAsLong());
    }
}
