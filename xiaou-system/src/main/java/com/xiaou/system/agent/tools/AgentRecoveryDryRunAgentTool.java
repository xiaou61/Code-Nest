package com.xiaou.system.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.service.SysAgentAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 对失败审计恢复路径执行通用只读安全预演。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentRecoveryDryRunAgentTool implements AgentTool {

    private final SysAgentAuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.recovery.dry_run");
        definition.setTitle("失败恢复安全预演");
        definition.setDescription("按 auditId 判断失败审计是否允许重试或补偿，只输出安全闸门，不执行目标工具、不写审计。");
        definition.setIntent("system.agent.recovery.dry_run");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of(
                "auditId", Map.of("type", "string", "minLength", 1, "maxLength", 120)
        ));
        definition.setRequiredInputKeys(List.of("auditId"));
        definition.setRequiredPermissions(List.of("agent:runtime:audit:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = AgentFailureRecoverySupport.normalize(message);
        if (!asksRecoveryDryRun(normalized)) {
            return Optional.empty();
        }

        String auditId = AgentFailureRecoverySupport.extractAuditId(normalized);
        if (!StringUtils.hasText(auditId)) {
            return Optional.empty();
        }

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("auditId", auditId);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("预演失败审计恢复安全闸门");
        call.setInput(input);
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("失败恢复安全预演是只读动作，不重试旧审计、不执行补偿、不写审计。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String auditId = AgentFailureRecoverySupport.normalize(
                call == null || call.getInput() == null ? null : call.getInput().get("auditId")
        );
        if (!StringUtils.hasText(auditId)) {
            AgentToolResult result = new AgentToolResult();
            result.setSuccess(false);
            result.setSummary("失败恢复安全预演缺少 auditId。");
            result.setErrorMessage("auditId 不能为空");
            result.getNextActions().add("请提供失败审计 auditId，例如：预演一下 agent-audit-1 能不能重试。");
            return result;
        }

        AgentAuditResponse audit = auditService.getByAuditId(auditId);
        if (audit == null) {
            AgentToolResult missing = new AgentToolResult();
            missing.setSuccess(false);
            missing.setSummary("没有找到对应的智能体审计记录。");
            missing.setErrorMessage("智能体审计记录不存在: " + auditId);
            missing.getNextActions().add("请先查询最近的智能体审计记录，确认 auditId 是否正确。");
            return missing;
        }

        AgentChatArtifact recovery = AgentFailureRecoverySupport.resolveRecoveryArtifact(audit, objectMapper);
        Map<String, Object> data = buildDryRun(audit, recovery);

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary(summary(data));
        result.getArtifacts().add(new AgentChatArtifact(
                "failureRecoveryDryRun",
                "失败恢复安全预演",
                data
        ));
        for (String action : AgentFailureRecoverySupport.recommendedActions(recovery)) {
            AgentFailureRecoverySupport.addIfMissing(result.getNextActions(), action);
        }
        AgentFailureRecoverySupport.addIfMissing(result.getNextActions(), "不要复用旧失败审计直接重试；需要重新发起自然语言请求生成新的预览。");
        AgentFailureRecoverySupport.addIfMissing(result.getNextActions(), "如果未来需要补偿动作，必须先新增专门的后端 AgentTool 并重新经过 policy、audit 和强确认。");
        return result;
    }

    private Map<String, Object> buildDryRun(AgentAuditResponse audit, AgentChatArtifact recovery) {
        Map<String, Object> recoveryData = recovery == null || recovery.getData() == null
                ? Map.of()
                : recovery.getData();
        String retryPolicy = firstNonBlank(
                AgentFailureRecoverySupport.stringValue(recoveryData, "retryPolicy"),
                AgentFailureRecoverySupport.RETRY_POLICY_NEW_PREVIEW_REQUIRED
        );
        boolean sameAuditRetryAllowed = AgentFailureRecoverySupport.booleanValue(recoveryData, "sameAuditRetryAllowed", false);
        boolean requiresNewPreview = AgentFailureRecoverySupport.booleanValue(recoveryData, "requiresNewPreview", true);
        String status = firstNonBlank(
                AgentFailureRecoverySupport.stringValue(recoveryData, "status"),
                audit.getStatus()
        );

        List<String> blockers = blockers(status, sameAuditRetryAllowed, requiresNewPreview, retryPolicy);
        List<String> recommendedActions = AgentFailureRecoverySupport.recommendedActions(recovery);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auditId", firstNonBlank(AgentFailureRecoverySupport.stringValue(recoveryData, "auditId"), audit.getAuditId()));
        data.put("idempotencyKey", firstNonBlank(AgentFailureRecoverySupport.stringValue(recoveryData, "idempotencyKey"), audit.getIdempotencyKey()));
        data.put("toolName", firstNonBlank(AgentFailureRecoverySupport.stringValue(recoveryData, "toolName"), audit.getActionId()));
        data.put("riskLevel", firstNonBlank(AgentFailureRecoverySupport.stringValue(recoveryData, "riskLevel"), audit.getRiskLevel()));
        data.put("riskCategory", firstNonBlank(AgentFailureRecoverySupport.stringValue(recoveryData, "riskCategory"), audit.getRiskCategory()));
        data.put("status", status);
        data.put("errorMessage", firstNonBlank(AgentFailureRecoverySupport.stringValue(recoveryData, "errorMessage"), audit.getErrorMessage()));
        data.put("retryPolicy", retryPolicy);
        data.put("sameAuditRetryAllowed", sameAuditRetryAllowed);
        data.put("requiresNewPreview", requiresNewPreview);
        data.put("safeToExecuteNow", false);
        data.put("compensationAllowed", false);
        data.put("dryRunOnly", true);
        data.put("targetExecuted", false);
        data.put("auditWritten", false);
        data.put("sameAuditRetryDecision", decision(
                sameAuditRetryAllowed,
                sameAuditRetryAllowed
                        ? "恢复上下文声明允许同审计重试，但本工具仍只做预演，不会执行。"
                        : "旧失败审计不可作为重试入口，避免重复写入或绕过新的预览确认。"
        ));
        data.put("newPreviewDecision", decision(
                true,
                requiresNewPreview
                        ? "必须重新发起原始自然语言请求，生成新的 preview/audit/confirm 链路。"
                        : "当前恢复上下文未强制新预览，但统一运行时仍建议重新进入 preview/audit/confirm 链路。"
        ));
        data.put("compensationDecision", decision(
                false,
                "当前版本未开放自动补偿执行；补偿必须先建模为新的后端 AgentTool，再经过 policy、audit 和强确认。"
        ));
        data.put("blockers", blockers);
        data.put("recommendedActions", recommendedActions);
        data.put("failureRecovery", recoveryData);
        return data;
    }

    private List<String> blockers(String status,
                                  boolean sameAuditRetryAllowed,
                                  boolean requiresNewPreview,
                                  String retryPolicy) {
        List<String> blockers = new ArrayList<>();
        if (!"FAILED".equalsIgnoreCase(status)) {
            blockers.add("当前审计不是 FAILED 状态，失败恢复不适用。");
        }
        if (!sameAuditRetryAllowed) {
            blockers.add("旧失败审计不可重试。");
        }
        if (requiresNewPreview || AgentFailureRecoverySupport.RETRY_POLICY_NEW_PREVIEW_REQUIRED.equalsIgnoreCase(retryPolicy)) {
            blockers.add("必须重新生成预览并走新的强确认链路。");
        }
        blockers.add("当前工具是只读 dry-run，不执行目标工具、不写审计。");
        return blockers;
    }

    private Map<String, Object> decision(boolean allowed, String reason) {
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("allowed", allowed);
        decision.put("reason", reason);
        return decision;
    }

    private String summary(Map<String, Object> data) {
        boolean sameAuditRetryAllowed = Boolean.TRUE.equals(data.get("sameAuditRetryAllowed"));
        if (!sameAuditRetryAllowed) {
            return "失败恢复 dry-run 完成：旧失败审计不可重试，必须重新生成预览；本次只读，未执行目标工具、未写审计。";
        }
        return "失败恢复 dry-run 完成：恢复上下文允许同审计重试判断，但本工具只读，未执行目标工具、未写审计。";
    }

    private boolean asksRecoveryDryRun(String message) {
        String normalized = AgentFailureRecoverySupport.normalize(message).toLowerCase(Locale.ROOT);
        if (containsAny(normalized, List.of("解释", "恢复上下文")) && !containsAny(normalized, List.of("预演", "试跑", "dry run"))) {
            return false;
        }
        return containsAny(normalized, List.of(
                "recovery dry run",
                "compensation dry run",
                "恢复预演",
                "恢复试跑",
                "恢复预检",
                "重试预演",
                "重试试跑",
                "重试预检",
                "补偿预演",
                "补偿试跑",
                "补偿预检",
                "能不能重试",
                "能否重试",
                "可以重试",
                "能不能补偿",
                "能否补偿",
                "可以补偿",
                "安全闸门"
        ));
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
    }

    private String firstNonBlank(String first, String fallback) {
        return StringUtils.hasText(first) ? first : AgentFailureRecoverySupport.normalize(fallback);
    }
}
