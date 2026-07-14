package com.xiaou.system.agent.tools;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询单条管理员智能体审计记录的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentAuditDetailAgentTool implements AgentTool {

    private static final Pattern AUDIT_ID_PATTERN = Pattern.compile("(agent-audit-[A-Za-z0-9_-]+)");

    private final SysAgentAuditService auditService;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.audit.detail");
        definition.setTitle("查询智能体审计详情");
        definition.setDescription("按 auditId 查询单条管理员智能体审计记录，返回计划、输入、结果和状态信息。");
        definition.setIntent("system.agent.audit.detail");
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
        String normalized = normalize(message);
        if (!containsAny(normalized, List.of("智能体审计详情", "审计详情", "智能体记录详情", "执行详情", "确认详情"))) {
            return Optional.empty();
        }

        String auditId = extractAuditId(normalized);
        if (!StringUtils.hasText(auditId)) {
            return Optional.empty();
        }

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("auditId", auditId);

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("查询管理员智能体审计详情");
        call.setInput(input);
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("查询智能体审计详情是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String auditId = normalize(call.getInput().get("auditId"));
        AgentAuditResponse audit = auditService.getByAuditId(auditId);
        if (audit == null) {
            AgentToolResult missing = new AgentToolResult();
            missing.setSuccess(false);
            missing.setSummary("没有找到对应的智能体审计记录。");
            missing.setErrorMessage("智能体审计记录不存在: " + auditId);
            missing.getNextActions().add("请先查询最近的智能体审计记录，确认 auditId 是否正确。");
            return missing;
        }

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("已查询到智能体审计记录 " + audit.getAuditId() + "，当前状态：" + audit.getStatusText() + "。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentAuditDetail",
                "智能体审计详情",
                toDetail(audit)
        ));
        result.getNextActions().add("如果这是待确认记录，可以继续输入强确认文本执行，或输入“取消”放弃。");
        return result;
    }

    private Map<String, Object> toDetail(AgentAuditResponse audit) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("auditId", audit.getAuditId());
        detail.put("confirmationId", audit.getConfirmationId());
        detail.put("userMessage", audit.getUserMessage());
        detail.put("intent", audit.getIntent());
        detail.put("actionId", audit.getActionId());
        detail.put("route", audit.getRoute());
        detail.put("riskLevel", audit.getRiskLevel());
        detail.put("riskCategory", audit.getRiskCategory());
        detail.put("status", audit.getStatus());
        detail.put("statusText", audit.getStatusText());
        detail.put("summary", audit.getSummary());
        detail.put("payloadJson", audit.getPayloadJson());
        detail.put("diffJson", audit.getDiffJson());
        detail.put("planJson", audit.getPlanJson());
        detail.put("resultJson", audit.getResultJson());
        detail.put("errorMessage", audit.getErrorMessage());
        detail.put("operatorId", audit.getOperatorId());
        detail.put("operatorName", audit.getOperatorName());
        detail.put("confirmedTime", audit.getConfirmedTime());
        detail.put("executedTime", audit.getExecutedTime());
        detail.put("createdTime", audit.getCreatedTime());
        detail.put("updatedTime", audit.getUpdatedTime());
        return detail;
    }

    private String extractAuditId(String value) {
        Matcher matcher = AUDIT_ID_PATTERN.matcher(value);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream().anyMatch(value::contains);
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
