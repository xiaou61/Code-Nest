package com.xiaou.system.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.system.dto.AgentAuditPreviewRequest;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentAuditResultRequest;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.dto.AgentChatConfirmation;
import com.xiaou.system.dto.AgentChatRequest;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.service.SysAgentAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 管理员智能体后端运行时。前端只需要调用 chat 这一个深接口。
 *
 * @author xiaou
 */
@Slf4j
@Service
public class AgentChatOrchestrator {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<java.util.List<com.xiaou.system.dto.AgentChatArtifact>> ARTIFACT_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<java.util.List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final String RECOVERY_QUERY_STATE = "查询目标对象当前状态，确认是否发生部分写入。";
    private static final String RECOVERY_RETRY_WITH_NEW_PREVIEW = "修复外部依赖、权限或输入数据后，重新发起同一自然语言请求生成新的预览。";

    private final AgentToolRegistry toolRegistry;
    private final AgentPlanResolver planResolver;
    private final AgentPolicyEngine policyEngine;
    private final SysAgentAuditService auditService;
    private final ObjectMapper objectMapper;
    private final AgentSessionContextStore sessionContextStore;
    private final AgentToolMetricsRecorder metricsRecorder;

    @Autowired
    public AgentChatOrchestrator(
            AgentToolRegistry toolRegistry,
            AgentPlanResolver planResolver,
            AgentPolicyEngine policyEngine,
            SysAgentAuditService auditService,
            ObjectMapper objectMapper,
            AgentSessionContextStore sessionContextStore,
            AgentToolMetricsRecorder metricsRecorder
    ) {
        this.toolRegistry = toolRegistry;
        this.planResolver = planResolver;
        this.policyEngine = policyEngine;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.sessionContextStore = sessionContextStore;
        this.metricsRecorder = metricsRecorder;
    }

    public AgentChatOrchestrator(
            AgentToolRegistry toolRegistry,
            AgentPlanResolver planResolver,
            AgentPolicyEngine policyEngine,
            SysAgentAuditService auditService,
            ObjectMapper objectMapper,
            AgentSessionContextStore sessionContextStore
    ) {
        this(
                toolRegistry,
                planResolver,
                policyEngine,
                auditService,
                objectMapper,
                sessionContextStore,
                null
        );
    }

    public AgentChatOrchestrator(
            AgentToolRegistry toolRegistry,
            AgentPlanResolver planResolver,
            AgentPolicyEngine policyEngine,
            SysAgentAuditService auditService,
            ObjectMapper objectMapper
    ) {
        this(toolRegistry, planResolver, policyEngine, auditService, objectMapper, new AgentSessionContextStore());
    }

    public AgentChatResponse chat(AgentChatRequest request, AgentOperator operator) {
        AgentChatRequest safeRequest = request == null ? new AgentChatRequest() : request;
        String sessionId = normalize(safeRequest.getSessionId());
        Long currentOperatorId = operator == null ? null : operator.id();
        AgentRuntimeTrace trace = AgentRuntimeTrace.start();
        AgentSessionSnapshot sessionSnapshot = sessionContextStore.snapshot(sessionId, currentOperatorId);
        AgentExecutionContext context = new AgentExecutionContext(
                sessionId,
                normalize(safeRequest.getMessage()),
                operator,
                trace,
                sessionSnapshot
        );
        context.markTrace("request.received", "done",
                StringUtils.hasText(safeRequest.getAuditId()) ? "continue audited action" : "start new action");
        context.markTrace("session.loaded", "done",
                "recentTurns=" + sessionSnapshot.getRecentTurns().size());

        AgentChatResponse response;
        if (StringUtils.hasText(safeRequest.getAuditId())) {
            response = continueAuditedAction(safeRequest, context);
        } else {
            response = startNewAction(safeRequest, context);
        }
        sessionContextStore.record(safeRequest, response, currentOperatorId);
        log.info("管理员智能体请求完成 traceId={}, status={}, toolName={}, operatorId={}",
                response.getTraceId(), response.getStatus(), response.getToolName(), operatorId(context));
        return response;
    }

    private AgentChatResponse startNewAction(AgentChatRequest request, AgentExecutionContext context) {
        if (!StringUtils.hasText(context.message())) {
            return rejected(context, AgentChatErrorCode.EMPTY_MESSAGE, "你还没有输入要我处理的管理员请求。");
        }

        AgentPlanResolution plan = planResolver.resolvePlan(context);
        if (plan.isResolved()) {
            context.markTrace("plan.resolved", "done", safeToolName(plan.getResolvedCall()));
            return requireRegisteredTool(plan.getResolvedCall())
                    .map(registered -> handleResolvedCall(request, context, registered))
                    .orElseGet(() -> toolNotFound(context, "计划解析器返回了未注册的后端工具，已拒绝执行。"));
        }
        if (plan.getErrorCode() != null) {
            context.markTrace("plan.clarification", "blocked", safe(plan.getMessage()));
            return rejectedByPlan(context, plan);
        }
        return toolNotFound(context, "我还没有找到可安全执行这个请求的后端工具。");
    }

    private AgentChatResponse rejectedByPlan(AgentExecutionContext context, AgentPlanResolution plan) {
        AgentChatResponse response = rejected(context, plan.getErrorCode(), plan.getMessage());
        response.getNextActions().addAll(plan.getNextActions());
        return response;
    }

    private Optional<AgentResolvedToolCall> requireRegisteredTool(AgentResolvedToolCall resolved) {
        if (resolved == null || resolved.call() == null) {
            return Optional.empty();
        }

        String callToolName = normalize(resolved.call().getToolName());
        String definitionToolName = resolved.tool() == null || resolved.tool().definition() == null
                ? ""
                : normalize(resolved.tool().definition().getName());
        String toolName = StringUtils.hasText(callToolName) ? callToolName : definitionToolName;
        if (!StringUtils.hasText(toolName)) {
            return Optional.empty();
        }

        return toolRegistry.find(toolName)
                .map(tool -> {
                    resolved.call().setToolName(tool.definition().getName());
                    return new AgentResolvedToolCall(tool, resolved.call());
                });
    }

    private AgentChatResponse toolNotFound(AgentExecutionContext context, String message) {
        context.markTrace("tool.not_found", "rejected", message);
        AgentChatResponse response = rejected(context, AgentChatErrorCode.TOOL_NOT_FOUND, message);
        response.getArtifacts().add(new com.xiaou.system.dto.AgentChatArtifact(
                "toolCatalog",
                "当前已注册工具",
                Map.of("tools", toolRegistry.definitions())
        ));
        return response;
    }

    private AgentChatResponse handleResolvedCall(
            AgentChatRequest request,
            AgentExecutionContext context,
            AgentResolvedToolCall resolved
    ) {
        AgentTool tool = resolved.tool();
        AgentToolCall call = resolved.call();
        AgentToolDefinition definition = tool.definition();
        context.markTrace("tool.registered", "done", definition.getName());
        AgentPolicyDecision decision = policyEngine.evaluate(definition, call.getInput(), context.operator());
        context.markTrace("policy.evaluated",
                decision.isAllowed() ? (decision.isConfirmationRequired() ? "confirmation_required" : "allowed") : "rejected",
                definition.getName());
        if (!decision.isAllowed()) {
            return rejectedByPolicy(context, definition, decision);
        }

        if (decision.isConfirmationRequired()) {
            return previewAndRequireConfirmation(request, context, tool, call, decision);
        }

        AgentToolResult result = executeToolSafely(tool, call, context);
        if (!result.isSuccess()) {
            appendFailureRecoveryAdvice(result.getNextActions(), null);
            appendFailureRecoveryArtifact(result.getArtifacts(), null, definition, result.getErrorMessage());
        }
        AgentChatResponse response = base(context);
        response.setStatus(result.isSuccess() ? "answered" : "error");
        response.setAnswer(result.getSummary());
        response.setToolName(definition.getName());
        response.setRiskLevel(definition.getRiskLevel());
        response.setRiskCategory(definition.getRiskCategory());
        response.setDiff(result.getDiff());
        response.setArtifacts(result.getArtifacts());
        response.setNextActions(result.getNextActions());
        if (!result.isSuccess()) {
            response.setErrorCode(AgentChatErrorCode.TOOL_EXECUTION_FAILED.code());
            response.setErrorMessage(result.getErrorMessage());
        }
        return response;
    }

    private AgentChatResponse previewAndRequireConfirmation(
            AgentChatRequest request,
            AgentExecutionContext context,
            AgentTool tool,
            AgentToolCall call,
            AgentPolicyDecision decision
    ) {
        AgentToolDefinition definition = tool.definition();
        AgentToolPreview preview;
        long startNanos = System.nanoTime();
        try {
            preview = tool.preview(call, context);
        } catch (Exception e) {
            recordToolMetric(definition, context, "preview", "error", System.nanoTime() - startNanos);
            log.warn("智能体工具预览失败 tool={}, message={}", definition.getName(), e.getMessage());
            context.markTrace("tool.previewed", "error", exceptionMessage(e));
            return error(context, AgentChatErrorCode.TOOL_EXECUTION_FAILED, definition,
                    "工具预览失败：" + exceptionMessage(e), exceptionMessage(e));
        }
        recordToolMetric(definition, context, "preview", preview.isExecutable() ? "success" : "blocked",
                System.nanoTime() - startNanos);
        context.markTrace("tool.previewed", preview.isExecutable() ? "done" : "blocked", definition.getName());

        if (!preview.isExecutable()) {
            context.markTrace("preview.blocked", "rejected",
                    StringUtils.hasText(preview.getBlockedReason()) ? preview.getBlockedReason() : preview.getSummary());
            AgentChatResponse response = rejected(
                    context,
                    AgentChatErrorCode.PREVIEW_BLOCKED,
                    StringUtils.hasText(preview.getBlockedReason()) ? preview.getBlockedReason() : preview.getSummary()
            );
            response.setToolName(definition.getName());
            response.setRiskLevel(definition.getRiskLevel());
            response.setRiskCategory(definition.getRiskCategory());
            response.setPlan(preview.getPlan());
            response.setDiff(preview.getDiff());
            response.setArtifacts(preview.getArtifacts());
            return response;
        }

        String confirmationId = "agent-confirmation-" + UUID.randomUUID();
        String idempotencyKey = "agent-idempotency-" + UUID.randomUUID();

        AgentAuditPreviewRequest auditRequest = new AgentAuditPreviewRequest();
        auditRequest.setConfirmationId(confirmationId);
        auditRequest.setIdempotencyKey(idempotencyKey);
        auditRequest.setUserMessage(limit(request.getMessage(), 1000));
        auditRequest.setIntent(definition.getIntent());
        auditRequest.setActionId(definition.getName());
        auditRequest.setRoute(definition.getRoute());
        auditRequest.setRiskLevel(definition.getRiskLevel());
        auditRequest.setRiskCategory(definition.getRiskCategory());
        auditRequest.setSummary(preview.getSummary());
        auditRequest.setPayloadJson(toJson(call.getInput()));
        auditRequest.setDiffJson(toJson(preview.getDiff()));
        auditRequest.setPlanJson(toJson(preview.getPlan()));

        AgentAuditResponse audit = auditService.createPreview(auditRequest, operatorId(context), operatorName(context));
        context.markTrace("audit.preview_created", "done", audit.getAuditId());

        AgentChatConfirmation confirmation = new AgentChatConfirmation();
        confirmation.setAuditId(audit.getAuditId());
        confirmation.setConfirmationId(confirmationId);
        confirmation.setRequiredText(decision.getConfirmationText());
        confirmation.setPrompt("这是写入/破坏性动作。确认无误后请输入：" + decision.getConfirmationText());

        AgentChatResponse response = base(context);
        response.setStatus("confirm_required");
        response.setAnswer(preview.getSummary() + " 确认无误后请输入：" + decision.getConfirmationText());
        response.setAuditId(audit.getAuditId());
        response.setIdempotencyKey(audit.getIdempotencyKey());
        response.setToolName(definition.getName());
        response.setRiskLevel(definition.getRiskLevel());
        response.setRiskCategory(definition.getRiskCategory());
        response.setPlan(preview.getPlan());
        response.setDiff(preview.getDiff());
        response.setArtifacts(preview.getArtifacts());
        response.setConfirmation(confirmation);
        response.getNextActions().addAll(decision.getNextActions());
        return response;
    }

    private AgentChatResponse continueAuditedAction(AgentChatRequest request, AgentExecutionContext context) {
        AgentAuditResponse audit = auditService.getByAuditId(request.getAuditId());
        if (audit == null) {
            context.markTrace("audit.loaded", "missing", request.getAuditId());
            return rejected(context, AgentChatErrorCode.AUDIT_NOT_FOUND, "没有找到这次待确认的智能体审计记录。");
        }
        context.markTrace("audit.loaded", "done", audit.getAuditId());

        Long currentOperatorId = operatorId(context);
        if (currentOperatorId == null || audit.getOperatorId() == null || !audit.getOperatorId().equals(currentOperatorId)) {
            AgentChatResponse response = rejected(context, AgentChatErrorCode.AUDIT_OPERATOR_MISMATCH, "这次预览不是由当前管理员发起，不能继续执行。");
            response.setAuditId(audit.getAuditId());
            response.setIdempotencyKey(audit.getIdempotencyKey());
            response.setToolName(audit.getActionId());
            response.setRiskLevel(audit.getRiskLevel());
            response.setRiskCategory(audit.getRiskCategory());
            return response;
        }

        if (isCancelRequest(request)) {
            if (!"PREVIEW".equals(audit.getStatus())) {
                return auditStatusRejected(context, audit, "这次审计记录不是待确认状态，不能继续处理。", audit.getStatus());
            }
            AgentAuditResponse cancelled;
            try {
                cancelled = auditService.cancel(audit.getAuditId(), "管理员通过统一聊天接口取消");
            } catch (IllegalStateException e) {
                return auditStatusRejected(context, audit, "这次审计记录状态已变化，取消请求没有生效。", exceptionMessage(e));
            }
            context.markTrace("audit.cancelled", "done", cancelled.getAuditId());
            AgentChatResponse response = base(context);
            response.setStatus("cancelled");
            response.setAnswer("已取消本次智能体预览，没有执行写入动作。");
            response.setAuditId(cancelled.getAuditId());
            response.setIdempotencyKey(cancelled.getIdempotencyKey());
            return response;
        }

        if (!"PREVIEW".equals(audit.getStatus())) {
            if (isTerminalStatus(audit.getStatus())) {
                return replayTerminalAudit(context, audit);
            }
            return auditStatusRejected(context, audit, "这次审计记录不是待确认状态，不能继续处理。", audit.getStatus());
        }

        AgentTool tool = toolRegistry.find(audit.getActionId()).orElse(null);
        if (tool == null) {
            context.markTrace("tool.not_found", "rejected", audit.getActionId());
            AgentChatResponse response = rejected(context, AgentChatErrorCode.TOOL_NOT_FOUND, "审计记录中的工具已不存在，不能继续执行。");
            response.setAuditId(audit.getAuditId());
            response.setIdempotencyKey(audit.getIdempotencyKey());
            response.setToolName(audit.getActionId());
            response.setRiskLevel(audit.getRiskLevel());
            response.setRiskCategory(audit.getRiskCategory());
            return response;
        }

        Map<String, Object> input;
        try {
            input = parsePayload(audit.getPayloadJson());
        } catch (IllegalArgumentException e) {
            context.markTrace("audit.payload_parsed", "error", exceptionMessage(e));
            AgentChatResponse response = rejected(context, AgentChatErrorCode.AUDIT_PAYLOAD_INVALID, "审计记录中的工具输入无法解析，不能继续执行。");
            response.setAuditId(audit.getAuditId());
            response.setIdempotencyKey(audit.getIdempotencyKey());
            response.setToolName(tool.definition().getName());
            response.setRiskLevel(tool.definition().getRiskLevel());
            response.setRiskCategory(tool.definition().getRiskCategory());
            return response;
        }
        AgentPolicyDecision decision = policyEngine.evaluate(tool.definition(), input, context.operator());
        context.markTrace("policy.evaluated",
                decision.isAllowed() ? "confirmation_required" : "rejected",
                tool.definition().getName());
        if (!decision.isAllowed()) {
            AgentChatResponse response = rejectedByPolicy(context, tool.definition(), decision);
            response.setAuditId(audit.getAuditId());
            response.setIdempotencyKey(audit.getIdempotencyKey());
            return response;
        }

        if (!policyEngine.confirmationMatches(decision.getConfirmationText(), request.getConfirmationText())) {
            context.markTrace("confirmation.checked", "rejected", "confirmation text mismatch");
            AgentChatResponse response = rejected(context, AgentChatErrorCode.CONFIRMATION_MISMATCH, "强确认文本不匹配，写入动作没有执行。");
            response.setAuditId(audit.getAuditId());
            response.setIdempotencyKey(audit.getIdempotencyKey());
            response.setToolName(tool.definition().getName());
            response.setRiskLevel(tool.definition().getRiskLevel());
            response.setRiskCategory(tool.definition().getRiskCategory());
            response.getNextActions().add("请输入完全一致的确认文本：" + decision.getConfirmationText());
            return response;
        }

        try {
            auditService.confirm(audit.getAuditId());
        } catch (IllegalStateException e) {
            AgentAuditResponse latest = auditService.getByAuditId(audit.getAuditId());
            if (latest != null && isTerminalStatus(latest.getStatus())) {
                return replayTerminalAudit(context, latest);
            }
            return auditStatusRejected(context, audit, "这次审计记录状态已变化，写入动作没有执行。", exceptionMessage(e));
        }
        context.markTrace("audit.confirmed", "done", audit.getAuditId());

        AgentToolCall call = new AgentToolCall();
        call.setToolName(tool.definition().getName());
        call.setSummary(audit.getSummary());
        call.setInput(input);

        AgentToolResult result = executeToolSafely(tool, call, context);
        if (!result.isSuccess()) {
            appendFailureRecoveryAdvice(result.getNextActions(), audit.getAuditId());
            appendFailureRecoveryArtifact(result.getArtifacts(), audit, tool.definition(), result.getErrorMessage());
        }
        AgentAuditResultRequest resultRequest = new AgentAuditResultRequest();
        resultRequest.setSuccess(result.isSuccess());
        resultRequest.setResultJson(toJson(Map.of(
                "summary", safe(result.getSummary()),
                "artifacts", result.getArtifacts(),
                "nextActions", result.getNextActions()
        )));
        resultRequest.setErrorMessage(result.getErrorMessage());
        AgentAuditResponse updatedAudit = auditService.recordResult(audit.getAuditId(), resultRequest);
        context.markTrace("audit.result_recorded", result.isSuccess() ? "done" : "error", updatedAudit.getAuditId());

        AgentChatResponse response = base(context);
        response.setStatus(result.isSuccess() ? "executed" : "error");
        response.setAnswer(result.getSummary());
        response.setAuditId(updatedAudit.getAuditId());
        response.setIdempotencyKey(updatedAudit.getIdempotencyKey());
        response.setToolName(tool.definition().getName());
        response.setRiskLevel(tool.definition().getRiskLevel());
        response.setRiskCategory(tool.definition().getRiskCategory());
        response.setDiff(result.getDiff());
        response.setArtifacts(result.getArtifacts());
        response.setNextActions(result.getNextActions());
        if (!result.isSuccess()) {
            response.setErrorCode(AgentChatErrorCode.TOOL_EXECUTION_FAILED.code());
            response.setErrorMessage(result.getErrorMessage());
        }
        return response;
    }

    private AgentChatResponse auditStatusRejected(
            AgentExecutionContext context,
            AgentAuditResponse audit,
            String message,
            String detail
    ) {
        context.markTrace("audit.status_checked", "rejected", detail);
        AgentChatResponse response = rejected(context, AgentChatErrorCode.AUDIT_STATUS_NOT_PREVIEW, message);
        response.setAuditId(audit.getAuditId());
        response.setIdempotencyKey(audit.getIdempotencyKey());
        response.setToolName(audit.getActionId());
        response.setRiskLevel(audit.getRiskLevel());
        response.setRiskCategory(audit.getRiskCategory());
        return response;
    }

    private AgentChatResponse replayTerminalAudit(AgentExecutionContext context, AgentAuditResponse audit) {
        context.markTrace("audit.idempotent_replay", "done", audit.getAuditId());
        Map<String, Object> resultData = parseResultJson(audit.getResultJson());
        AgentChatResponse response = base(context);
        boolean success = "EXECUTED".equals(audit.getStatus());
        response.setStatus(success ? "executed" : "error");
        response.setAnswer(resolveReplaySummary(audit, resultData, success));
        response.setAuditId(audit.getAuditId());
        response.setIdempotencyKey(audit.getIdempotencyKey());
        response.setToolName(audit.getActionId());
        response.setRiskLevel(audit.getRiskLevel());
        response.setRiskCategory(audit.getRiskCategory());
        response.setArtifacts(convertArtifacts(resultData.get("artifacts")));
        response.setNextActions(convertStrings(resultData.get("nextActions")));
        if (!success) {
            appendFailureRecoveryAdvice(response.getNextActions(), audit.getAuditId());
            appendFailureRecoveryArtifact(response.getArtifacts(), audit, null, audit.getErrorMessage());
            response.setErrorCode(AgentChatErrorCode.TOOL_EXECUTION_FAILED.code());
            response.setErrorMessage(StringUtils.hasText(audit.getErrorMessage()) ? audit.getErrorMessage() : response.getAnswer());
        }
        response.getNextActions().add("这是一次幂等重放，目标工具没有再次执行。");
        return response;
    }

    private boolean isTerminalStatus(String status) {
        return "EXECUTED".equals(status) || "FAILED".equals(status);
    }

    private boolean isCancelRequest(AgentChatRequest request) {
        String message = normalize(request.getMessage());
        return "取消".equals(message) || "cancel".equalsIgnoreCase(message);
    }

    private AgentChatResponse base(AgentExecutionContext context) {
        AgentChatResponse response = new AgentChatResponse();
        response.setSessionId(context.sessionId());
        response.setTraceId(context.traceId());
        if (context.trace() != null) {
            response.setTrace(context.trace().steps());
        }
        return response;
    }

    private AgentChatResponse rejectedByPolicy(AgentExecutionContext context, AgentToolDefinition definition, AgentPolicyDecision decision) {
        context.markTrace("policy.rejected", "rejected", decision.getRejectionReason());
        AgentChatErrorCode errorCode = decision.getErrorCode() == null
                ? AgentChatErrorCode.POLICY_REJECTED
                : decision.getErrorCode();
        AgentChatResponse response = rejected(context, errorCode, decision.getRejectionReason());
        response.setToolName(definition.getName());
        response.setRiskLevel(definition.getRiskLevel());
        response.setRiskCategory(definition.getRiskCategory());
        response.getNextActions().addAll(decision.getNextActions());
        return response;
    }

    private AgentToolResult executeToolSafely(AgentTool tool, AgentToolCall call, AgentExecutionContext context) {
        String toolName = tool == null || tool.definition() == null ? "" : tool.definition().getName();
        AgentToolDefinition definition = tool == null ? null : tool.definition();
        long startNanos = System.nanoTime();
        try {
            AgentToolResult result = tool.execute(call, context);
            recordToolMetric(definition, context, "execute", result.isSuccess() ? "success" : "error",
                    System.nanoTime() - startNanos);
            context.markTrace("tool.executed", result.isSuccess() ? "done" : "error", toolName);
            return result;
        } catch (Exception e) {
            recordToolMetric(definition, context, "execute", "error", System.nanoTime() - startNanos);
            log.warn("智能体工具执行失败 tool={}, message={}", toolName, e.getMessage());
            context.markTrace("tool.executed", "error", toolName + ": " + exceptionMessage(e));
            AgentToolResult result = new AgentToolResult();
            result.setSuccess(false);
            result.setSummary("工具执行失败：" + exceptionMessage(e));
            result.setErrorMessage(exceptionMessage(e));
            appendFailureRecoveryAdvice(result.getNextActions(), null);
            return result;
        }
    }

    private AgentChatResponse error(
            AgentExecutionContext context,
            AgentChatErrorCode code,
            AgentToolDefinition definition,
            String answer,
            String errorMessage
    ) {
        context.markTrace("response.error", "error", code.code());
        AgentChatResponse response = base(context);
        response.setStatus("error");
        response.setAnswer(answer);
        response.setErrorCode(code.code());
        response.setErrorMessage(errorMessage);
        if (definition != null) {
            response.setToolName(definition.getName());
            response.setRiskLevel(definition.getRiskLevel());
            response.setRiskCategory(definition.getRiskCategory());
        }
        response.getNextActions().add("请先查询当前状态，确认后台数据是否稳定。");
        return response;
    }

    private void recordToolMetric(
            AgentToolDefinition definition,
            AgentExecutionContext context,
            String phase,
            String outcome,
            long durationNanos
    ) {
        if (metricsRecorder == null) {
            return;
        }
        AgentToolMetricSample sample = new AgentToolMetricSample();
        sample.setToolName(definition == null ? "" : definition.getName());
        sample.setPhase(phase);
        sample.setOutcome(outcome);
        sample.setRiskLevel(definition == null ? "" : definition.getRiskLevel());
        sample.setRiskCategory(definition == null ? "" : definition.getRiskCategory());
        sample.setDurationNanos(durationNanos);
        sample.setTraceId(context == null ? "" : context.traceId());
        sample.setSessionId(context == null ? "" : context.sessionId());
        metricsRecorder.recordInvocation(sample);
    }

    private AgentChatResponse rejected(AgentExecutionContext context, AgentChatErrorCode code, String message) {
        context.markTrace("response.rejected", "rejected", code.code());
        AgentChatResponse response = base(context);
        response.setStatus("rejected");
        response.setAnswer(message);
        response.setErrorCode(code.code());
        response.setErrorMessage(message);
        response.getNextActions().add("请尝试更明确地说明要查询或操作的后台对象。");
        return response;
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(payloadJson, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("智能体审计 payload 无法解析", e);
        }
    }

    private Map<String, Object> parseResultJson(String resultJson) {
        if (!StringUtils.hasText(resultJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(resultJson, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    private String resolveReplaySummary(AgentAuditResponse audit, Map<String, Object> resultData, boolean success) {
        Object summaryValue = resultData.get("summary");
        String summary = normalize(summaryValue == null ? "" : String.valueOf(summaryValue));
        if (StringUtils.hasText(summary)) {
            return summary;
        }
        if (StringUtils.hasText(audit.getSummary())) {
            return audit.getSummary();
        }
        return success ? "该写入动作此前已执行成功。" : "该写入动作此前已执行失败。";
    }

    private java.util.List<com.xiaou.system.dto.AgentChatArtifact> convertArtifacts(Object value) {
        if (value == null) {
            return new java.util.ArrayList<>();
        }
        try {
            return objectMapper.convertValue(value, ARTIFACT_LIST_TYPE);
        } catch (IllegalArgumentException e) {
            return new java.util.ArrayList<>();
        }
    }

    private java.util.List<String> convertStrings(Object value) {
        if (value == null) {
            return new java.util.ArrayList<>();
        }
        try {
            return objectMapper.convertValue(value, STRING_LIST_TYPE);
        } catch (IllegalArgumentException e) {
            return new java.util.ArrayList<>();
        }
    }

    private void appendFailureRecoveryAdvice(java.util.List<String> nextActions, String auditId) {
        if (nextActions == null) {
            return;
        }
        addIfMissing(nextActions, RECOVERY_QUERY_STATE);
        if (StringUtils.hasText(auditId)) {
            addIfMissing(nextActions, "查看审计详情 " + auditId + "，核对 payload、result 和 errorMessage。");
        }
        addIfMissing(nextActions, RECOVERY_RETRY_WITH_NEW_PREVIEW);
    }

    private void addIfMissing(java.util.List<String> nextActions, String action) {
        if (StringUtils.hasText(action) && !nextActions.contains(action)) {
            nextActions.add(action);
        }
    }

    private void appendFailureRecoveryArtifact(
            java.util.List<AgentChatArtifact> artifacts,
            AgentAuditResponse audit,
            AgentToolDefinition definition,
            String errorMessage
    ) {
        if (artifacts == null || artifacts.stream().anyMatch(artifact -> "failureRecovery".equals(artifact.getType()))) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        putIfPresent(data, "auditId", audit == null ? null : audit.getAuditId());
        putIfPresent(data, "idempotencyKey", audit == null ? null : audit.getIdempotencyKey());
        putIfPresent(data, "toolName", resolveToolName(audit, definition));
        putIfPresent(data, "riskLevel", resolveRiskLevel(audit, definition));
        putIfPresent(data, "riskCategory", resolveRiskCategory(audit, definition));
        putIfPresent(data, "status", audit == null ? null : audit.getStatus());
        putIfPresent(data, "errorMessage", errorMessage);
        data.put("retryPolicy", "NEW_PREVIEW_REQUIRED");
        data.put("sameAuditRetryAllowed", false);
        data.put("requiresNewPreview", true);
        data.put("recommendedActions", java.util.List.of(
                RECOVERY_QUERY_STATE,
                RECOVERY_RETRY_WITH_NEW_PREVIEW
        ));
        artifacts.add(new AgentChatArtifact("failureRecovery", "失败恢复上下文", data));
    }

    private String resolveToolName(AgentAuditResponse audit, AgentToolDefinition definition) {
        if (audit != null && StringUtils.hasText(audit.getActionId())) {
            return audit.getActionId();
        }
        return definition == null ? "" : safe(definition.getName());
    }

    private String resolveRiskLevel(AgentAuditResponse audit, AgentToolDefinition definition) {
        if (audit != null && StringUtils.hasText(audit.getRiskLevel())) {
            return audit.getRiskLevel();
        }
        return definition == null ? "" : safe(definition.getRiskLevel());
    }

    private String resolveRiskCategory(AgentAuditResponse audit, AgentToolDefinition definition) {
        if (audit != null && StringUtils.hasText(audit.getRiskCategory())) {
            return audit.getRiskCategory();
        }
        return definition == null ? "" : safe(definition.getRiskCategory());
    }

    private void putIfPresent(Map<String, Object> data, String key, String value) {
        if (StringUtils.hasText(value)) {
            data.put(key, value);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("智能体数据无法序列化", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeToolName(AgentResolvedToolCall resolved) {
        if (resolved == null) {
            return "";
        }
        if (resolved.call() != null && StringUtils.hasText(resolved.call().getToolName())) {
            return resolved.call().getToolName();
        }
        if (resolved.tool() != null && resolved.tool().definition() != null) {
            return safe(resolved.tool().definition().getName());
        }
        return "";
    }

    private Long operatorId(AgentExecutionContext context) {
        return context.operator() == null ? null : context.operator().id();
    }

    private String operatorName(AgentExecutionContext context) {
        return context.operator() == null ? "" : context.operator().name();
    }

    private String exceptionMessage(Exception e) {
        return StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
    }
}
