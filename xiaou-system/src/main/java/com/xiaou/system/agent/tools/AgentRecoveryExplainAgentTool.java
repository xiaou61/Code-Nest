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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 解释失败审计恢复上下文的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentRecoveryExplainAgentTool implements AgentTool {

    private final SysAgentAuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.recovery.explain");
        definition.setTitle("解释失败恢复上下文");
        definition.setDescription("按 auditId 查询失败审计的 failureRecovery 上下文，只解释恢复路径，不执行补偿动作。");
        definition.setIntent("system.agent.recovery.explain");
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
        if (!AgentFailureRecoverySupport.containsAny(normalized, List.of("失败恢复", "恢复上下文", "补偿建议", "怎么处理", "failureRecovery"))) {
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
        call.setSummary("解释失败恢复上下文");
        call.setInput(input);
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("解释失败恢复上下文是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String auditId = AgentFailureRecoverySupport.normalize(call == null || call.getInput() == null ? null : call.getInput().get("auditId"));
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
        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("已解释失败审计 " + audit.getAuditId() + " 的恢复上下文；该工具只读，不会自动重试或补偿。");
        result.getArtifacts().add(recovery);
        AgentFailureRecoverySupport.appendRecommendedActions(result, recovery);
        result.getNextActions().add("如果要重新处理，请重新发起自然语言请求生成新的预览，不要复用旧失败审计。");
        return result;
    }
}
