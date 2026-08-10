package com.xiaou.system.agent.task;

import com.xiaou.system.agent.AgentChatErrorCode;
import com.xiaou.system.agent.AgentChatOrchestrator;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentOperatorResolver;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolRegistry;
import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskStep;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.dto.AgentTaskResponse;
import com.xiaou.system.service.SysAgentAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Executes the business behavior behind one graph cycle and task confirmation.
 */
@Slf4j
@Component
public class AgentTaskCycleRuntime {

    private final AgentTaskStateService stateService;
    private final AgentTaskPlanner planner;
    private final AgentToolRegistry toolRegistry;
    private final AgentChatOrchestrator orchestrator;
    private final AgentOperatorResolver operatorResolver;
    private final SysAgentAuditService auditService;
    private final AgentTaskLeaseManager leaseManager;

    public AgentTaskCycleRuntime(
            AgentTaskStateService stateService,
            AgentTaskPlanner planner,
            AgentToolRegistry toolRegistry,
            AgentChatOrchestrator orchestrator,
            AgentOperatorResolver operatorResolver,
            SysAgentAuditService auditService,
            AgentTaskLeaseManager leaseManager
    ) {
        this.stateService = stateService;
        this.planner = planner;
        this.toolRegistry = toolRegistry;
        this.orchestrator = orchestrator;
        this.operatorResolver = operatorResolver;
        this.auditService = auditService;
        this.leaseManager = leaseManager;
    }

    public AgentTaskPlanDecision planNext(String taskId, String leaseOwner) {
        return planNext(loadContext(taskId, leaseOwner));
    }

    public AgentTaskCycleContext loadContext(String taskId, String leaseOwner) {
        AgentTaskSnapshot snapshot = stateService.loadExecutionSnapshot(taskId, leaseOwner);
        return snapshot == null
                ? new AgentTaskCycleContext(taskId, leaseOwner, false)
                : new AgentTaskCycleContext(snapshot, leaseOwner);
    }

    public AgentTaskPlanDecision planNext(AgentTaskCycleContext context) {
        if (context == null || !context.active()) {
            return AgentTaskPlanDecision.blocked("LEASE_LOST", "任务租约已失效。", false);
        }
        AgentTaskSnapshot snapshot = context.snapshot();
        if (snapshot == null) {
            snapshot = stateService.loadExecutionSnapshot(context.taskId(), context.leaseOwner());
        }
        if (snapshot == null) {
            return AgentTaskPlanDecision.blocked("LEASE_LOST", "任务租约已失效。", false);
        }
        SysAgentTask task = snapshot.task();
        SysAgentTaskStep latest = snapshot.latestStep();
        if (latest != null && AgentTaskStepStatus.PENDING.name().equals(latest.getStatus())) {
            try {
                AgentToolCall storedCall = stateService.readStoredCall(latest);
                return AgentTaskPlanDecision.execute(
                        storedCall,
                        "恢复未完成的持久化只读步骤",
                        Boolean.TRUE.equals(latest.getPlannerFallback())
                );
            } catch (IllegalArgumentException e) {
                return AgentTaskPlanDecision.blocked("INVALID_PERSISTED_STEP", e.getMessage(), false);
            }
        }

        int completedSteps = value(task.getCompletedSteps());
        int maxSteps = Math.max(1, value(task.getMaxSteps()));
        if (completedSteps >= maxSteps) {
            return AgentTaskPlanDecision.blocked(
                    "STEP_LIMIT_EXHAUSTED",
                    "任务达到最大步骤数 " + maxSteps + "，已停止继续执行。",
                    false
            );
        }
        List<SysAgentTaskStep> completed = snapshot.steps().stream()
                .filter(step -> AgentTaskStepStatus.COMPLETED.name().equals(step.getStatus()))
                .toList();
        return planner.plan(new AgentTaskPlanningContext(
                task.getGoal(),
                completed,
                maxSteps - completedSteps,
                stateService.readWorkflowContext(task)
        ));
    }

    public AgentTaskPlanDecision policyGuard(
            AgentTaskCycleContext context,
            AgentTaskPlanDecision decision
    ) {
        if (context == null || !context.active()) {
            return AgentTaskPlanDecision.blocked("LEASE_LOST", "任务租约已失效。", false);
        }
        if (decision == null || !decision.execute()) {
            return decision;
        }
        AgentToolCall call = decision.call();
        AgentTool tool = call == null ? null : toolRegistry.find(call.getToolName()).orElse(null);
        if (tool == null) {
            return AgentTaskPlanDecision.blocked("TASK_TOOL_NOT_FOUND",
                    "任务 planner 指定的后端工具未注册，已拒绝执行。", false);
        }
        if (stateService.hasFingerprint(context.taskId(), call)) {
            return AgentTaskPlanDecision.blocked("DUPLICATE_TASK_STEP",
                    "任务 planner 重复了已经开始或完成的同一工具调用。", false);
        }
        return decision;
    }

    public AgentTaskCycleOutcome waitForInput(
            AgentTaskCycleContext context,
            AgentTaskPlanDecision decision
    ) {
        if (context == null || !context.active()) {
            return AgentTaskCycleOutcome.lostLease("任务租约已失效。");
        }
        boolean updated = stateService.waitForInput(
                context.taskId(), context.leaseOwner(),
                decision == null ? "planner 需要补充输入" : decision.reason());
        return updated
                ? AgentTaskCycleOutcome.waitingInput(decision == null ? "等待补充输入" : decision.reason())
                : outcomeAfterLostTransition(context.taskId(), "任务等待输入状态未能持久化。");
    }

    public AgentTaskCycleOutcome executeOrFinish(
            AgentTaskCycleContext context,
            AgentTaskPlanDecision decision
    ) {
        if (context == null || !context.active()) {
            return AgentTaskCycleOutcome.lostLease("任务租约已失效。");
        }
        return executeOrFinish(context.taskId(), context.leaseOwner(), decision);
    }

    public AgentTaskCycleOutcome observe(AgentTaskCycleOutcome outcome) {
        return outcome == null ? AgentTaskCycleOutcome.failed("任务图没有返回执行结果") : outcome;
    }

    public String route(AgentTaskCycleOutcome outcome) {
        return "end";
    }

    public AgentTaskCycleOutcome executeOrFinish(
            String taskId,
            String leaseOwner,
            AgentTaskPlanDecision decision
    ) {
        if (decision == null) {
            stateService.failTask(taskId, leaseOwner, "PLANNER_EMPTY", "任务 planner 没有返回决定。");
            return outcomeAfterLostTransition(taskId, "任务 planner 没有返回决定。");
        }
        if (decision.waitingInput()) {
            boolean updated = stateService.waitForInput(taskId, leaseOwner, decision.reason());
            return updated
                    ? AgentTaskCycleOutcome.waitingInput(decision.reason())
                    : outcomeAfterLostTransition(taskId, "任务等待输入状态未能持久化。");
        }
        if (decision.complete()) {
            return stateService.completeTask(taskId, leaseOwner, decision.reason())
                    ? AgentTaskCycleOutcome.completed(decision.reason())
                    : outcomeAfterLostTransition(taskId, decision.reason());
        }
        if (decision.blocked()) {
            if ("LEASE_LOST".equals(decision.code())) {
                return AgentTaskCycleOutcome.lostLease(decision.reason());
            }
            return stateService.failTask(taskId, leaseOwner, safeCode(decision.code()), decision.reason())
                    ? AgentTaskCycleOutcome.failed(decision.reason())
                    : outcomeAfterLostTransition(taskId, decision.reason());
        }

        AgentToolCall call = decision.call();
        AgentTool tool = call == null ? null : toolRegistry.find(call.getToolName()).orElse(null);
        if (tool == null) {
            String reason = "任务 planner 指定的后端工具未注册，已拒绝执行。";
            stateService.failTask(taskId, leaseOwner, "TASK_TOOL_NOT_FOUND", reason);
            return AgentTaskCycleOutcome.failed(reason);
        }
        if (stateService.hasFingerprint(taskId, call)) {
            String reason = "任务 planner 重复了已经开始或完成的同一工具调用。";
            stateService.failTask(taskId, leaseOwner, "DUPLICATE_TASK_STEP", reason);
            return AgentTaskCycleOutcome.failed(reason);
        }

        SysAgentTask task = stateService.load(taskId);
        if (task == null) {
            return AgentTaskCycleOutcome.lostLease("任务不存在或已被清理。");
        }

        AgentTaskStepExecution execution;
        try {
            execution = stateService.startStep(
                    taskId,
                    leaseOwner,
                    call,
                    tool.definition(),
                    decision.fallback()
            );
        } catch (IllegalStateException e) {
            if ("DUPLICATE_TASK_STEP".equals(e.getMessage())) {
                stateService.failTask(taskId, leaseOwner, "DUPLICATE_TASK_STEP",
                        "任务步骤指纹重复，已停止执行。");
                return AgentTaskCycleOutcome.failed("任务步骤指纹重复，已停止执行。");
            }
            throw e;
        }
        if (execution == null) {
            return outcomeAfterLostTransition(taskId, "任务步骤没有取得执行权。");
        }

        AgentChatResponse response;
        try {
            AgentOperator operator = operatorResolver.resolve(task.getOperatorId());
            response = orchestrator.executeRegisteredCall(
                    requestFor(task),
                    operator,
                    execution.call()
            );
        } catch (RuntimeException e) {
            AgentChatResponse failure = failureResponse("任务工具执行链路异常：" + safeMessage(e));
            stateService.failStep(
                    taskId,
                    leaseOwner,
                    execution.step().getStepOrder(),
                    AgentTaskStepStatus.RUNNING,
                    failure,
                    "TASK_RUNTIME_ERROR",
                    failure.getErrorMessage()
            );
            return outcomeAfterLostTransition(taskId, failure.getErrorMessage());
        }

        String status = normalize(response == null ? null : response.getStatus());
        if ("answered".equals(status) || "executed".equals(status)) {
            boolean fallback = decision.fallback() || Boolean.TRUE.equals(execution.step().getPlannerFallback());
            AgentTaskStatus next = fallback ? AgentTaskStatus.COMPLETED : AgentTaskStatus.RUNNING;
            boolean updated = stateService.completeStep(
                    taskId,
                    leaseOwner,
                    execution.step().getStepOrder(),
                    AgentTaskStepStatus.RUNNING,
                    response,
                    next
            );
            if (!updated) {
                return outcomeAfterLostTransition(taskId, response == null ? "" : response.getAnswer());
            }
            return fallback
                    ? AgentTaskCycleOutcome.completed(response.getAnswer())
                    : AgentTaskCycleOutcome.continueRunning(response.getAnswer());
        }
        if ("confirm_required".equals(status)) {
            boolean updated = stateService.pauseForConfirmation(
                    taskId,
                    leaseOwner,
                    execution.step().getStepOrder(),
                    response
            );
            return updated
                    ? AgentTaskCycleOutcome.waiting(response.getAnswer())
                    : outcomeAfterLostTransition(taskId, response == null ? "" : response.getAnswer());
        }

        String reason = response == null || !StringUtils.hasText(response.getErrorMessage())
                ? (response == null ? "任务工具没有返回结果。" : response.getAnswer())
                : response.getErrorMessage();
        boolean updated = stateService.failStep(
                taskId,
                leaseOwner,
                execution.step().getStepOrder(),
                AgentTaskStepStatus.RUNNING,
                response,
                response == null ? "TASK_STEP_FAILED" : safeCode(response.getErrorCode()),
                reason
        );
        return updated
                ? AgentTaskCycleOutcome.failed(reason)
                : outcomeAfterLostTransition(taskId, reason);
    }

    public AgentTaskResponse confirm(String taskId, AgentOperator requester, String confirmationText) {
        if (requester == null || requester.id() == null) {
            throw new com.xiaou.common.exception.BusinessException(
                    com.xiaou.common.core.domain.ResultCode.UNAUTHORIZED,
                    "当前管理员身份不可用"
            );
        }
        AgentTaskConfirmationClaim claim = stateService.beginConfirmation(taskId, requester.id());
        AgentAuditResponse audit = auditService.getByAuditId(claim.step().getAuditId());
        if (audit == null) {
            stateService.markRequiresReview(taskId, claim.leaseOwner(), claim.step().getStepOrder(),
                    AgentTaskStepStatus.CONFIRMING, "MISSING_CONFIRMATION_AUDIT",
                    "待确认步骤缺少审计记录，需要人工复核。");
            return stateService.detail(taskId, requester.id());
        }
        if ("CONFIRMED".equals(audit.getStatus())) {
            stateService.markRequiresReview(taskId, claim.leaseOwner(), claim.step().getStepOrder(),
                    AgentTaskStepStatus.CONFIRMING, "AMBIGUOUS_CONFIRMED_WRITE",
                    "写步骤已经确认但没有持久化终态，禁止自动重放。");
            return stateService.detail(taskId, requester.id());
        }

        AgentChatRequest request = requestFor(claim.task());
        request.setAuditId(claim.step().getAuditId());
        request.setConfirmationText(confirmationText);
        AgentChatResponse response;
        try {
            response = leaseManager.callWithHeartbeat(
                    taskId,
                    claim.leaseOwner(),
                    () -> orchestrator.chat(
                            request,
                            operatorResolver.resolve(claim.task().getOperatorId())
                    )
            );
        } catch (RuntimeException e) {
            reconcileConfirmationException(claim, e);
            return stateService.detail(taskId, requester.id());
        }

        String status = normalize(response == null ? null : response.getStatus());
        if ("executed".equals(status)) {
            stateService.completeStep(
                    taskId,
                    claim.leaseOwner(),
                    claim.step().getStepOrder(),
                    AgentTaskStepStatus.CONFIRMING,
                    response,
                    AgentTaskStatus.QUEUED
            );
        } else if (isRetryableConfirmationRejection(response)) {
            stateService.restoreConfirmationWait(claim);
        } else if ("error".equals(status)) {
            stateService.failStep(
                    taskId,
                    claim.leaseOwner(),
                    claim.step().getStepOrder(),
                    AgentTaskStepStatus.CONFIRMING,
                    response,
                    safeCode(response.getErrorCode()),
                    response.getErrorMessage()
            );
        } else {
            AgentAuditResponse latest = auditService.getByAuditId(claim.step().getAuditId());
            if (latest != null && "PREVIEW".equals(latest.getStatus())) {
                stateService.restoreConfirmationWait(claim);
            } else {
                stateService.markRequiresReview(taskId, claim.leaseOwner(), claim.step().getStepOrder(),
                        AgentTaskStepStatus.CONFIRMING, "AMBIGUOUS_CONFIRMATION_RESULT",
                        "确认请求没有得到可证明的审计终态，需要人工复核。");
            }
        }
        return stateService.detail(taskId, requester.id());
    }

    private void reconcileConfirmationException(AgentTaskConfirmationClaim claim, RuntimeException exception) {
        AgentAuditResponse latest = auditService.getByAuditId(claim.step().getAuditId());
        if (latest != null && "PREVIEW".equals(latest.getStatus())) {
            stateService.restoreConfirmationWait(claim);
            return;
        }
        stateService.markRequiresReview(
                claim.task().getTaskId(),
                claim.leaseOwner(),
                claim.step().getStepOrder(),
                AgentTaskStepStatus.CONFIRMING,
                "AMBIGUOUS_CONFIRMATION_FAILURE",
                "确认执行中断且审计没有可安全重放的状态：" + safeMessage(exception)
        );
    }

    private boolean isRetryableConfirmationRejection(AgentChatResponse response) {
        if (response == null || !"rejected".equals(normalize(response.getStatus()))) {
            return false;
        }
        String errorCode = response.getErrorCode();
        return AgentChatErrorCode.CONFIRMATION_MISMATCH.code().equalsIgnoreCase(errorCode)
                || AgentChatErrorCode.POLICY_REJECTED.code().equalsIgnoreCase(errorCode)
                || AgentChatErrorCode.SCHEMA_VALIDATION_FAILED.code().equalsIgnoreCase(errorCode);
    }

    private AgentChatRequest requestFor(SysAgentTask task) {
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId(task == null ? null : task.getSessionId());
        request.setMessage(task == null ? null : task.getGoal());
        return request;
    }

    private AgentChatResponse failureResponse(String message) {
        AgentChatResponse response = new AgentChatResponse();
        response.setStatus("error");
        response.setErrorCode("TASK_RUNTIME_ERROR");
        response.setErrorMessage(message);
        response.setAnswer(message);
        return response;
    }

    private AgentTaskCycleOutcome outcomeAfterLostTransition(String taskId, String detail) {
        SysAgentTask current = stateService.load(taskId);
        AgentTaskStatus status = current == null ? null : AgentTaskStatus.from(current.getStatus());
        if (status == AgentTaskStatus.CANCELLED) {
            return AgentTaskCycleOutcome.cancelled(detail);
        }
        if (status == AgentTaskStatus.REQUIRES_REVIEW) {
            return AgentTaskCycleOutcome.requiresReview(detail);
        }
        if (status == AgentTaskStatus.WAITING_INPUT) {
            return AgentTaskCycleOutcome.waitingInput(detail);
        }
        if (status == AgentTaskStatus.PAUSED) {
            return AgentTaskCycleOutcome.paused(detail);
        }
        if (status == AgentTaskStatus.FAILED) {
            return AgentTaskCycleOutcome.failed(detail);
        }
        if (status == AgentTaskStatus.COMPLETED) {
            return AgentTaskCycleOutcome.completed(detail);
        }
        return AgentTaskCycleOutcome.lostLease(detail);
    }

    private String safeCode(String value) {
        return StringUtils.hasText(value) ? value.trim() : "TASK_STEP_FAILED";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private int value(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String safeMessage(Exception exception) {
        return exception == null || !StringUtils.hasText(exception.getMessage())
                ? (exception == null ? "unknown" : exception.getClass().getSimpleName())
                : exception.getMessage();
    }
}
