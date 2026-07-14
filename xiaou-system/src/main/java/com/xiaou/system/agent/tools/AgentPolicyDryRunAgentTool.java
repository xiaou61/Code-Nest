package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentChatErrorCode;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentPolicyDecision;
import com.xiaou.system.agent.AgentPolicyEngine;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolCatalogService;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 对指定工具输入执行策略预检的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentPolicyDryRunAgentTool implements AgentTool {

    private final AgentToolCatalogService catalogService;
    private final AgentPolicyEngine policyEngine;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.policy.dry_run");
        definition.setTitle("智能体策略预检");
        definition.setDescription("对指定工具和输入执行 AgentPolicyEngine 只读预检，解释 schema、权限、角色、租户和确认策略判定。");
        definition.setIntent("system.agent.policy.dry_run");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of(
                "toolName", Map.of("type", "string", "minLength", 1, "maxLength", 160),
                "input", Map.of("type", "object")
        ));
        definition.setRequiredInputKeys(List.of("toolName"));
        definition.setRequiredPermissions(List.of("agent:runtime:policy:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!asksPolicyDryRun(normalized)) {
            return Optional.empty();
        }

        return catalogService.definitions().stream()
                .map(AgentToolDefinition::getName)
                .filter(name -> !name.equals(definition().getName()))
                .filter(name -> normalized.contains(name))
                .findFirst()
                .map(toolName -> {
                    Map<String, Object> input = new LinkedHashMap<>();
                    input.put("toolName", toolName);

                    AgentToolCall call = new AgentToolCall();
                    call.setToolName(definition().getName());
                    call.setSummary("对后端智能体工具执行策略预检");
                    call.setInput(input);
                    return call;
                });
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("策略预检是只读动作，不执行目标工具，也不生成写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String toolName = normalize(call.getInput().get("toolName"));
        Optional<AgentToolDefinition> matched = catalogService.definitions().stream()
                .filter(definition -> toolName.equals(definition.getName()))
                .findFirst();
        if (matched.isEmpty()) {
            AgentToolResult missing = new AgentToolResult();
            missing.setSuccess(false);
            missing.setSummary("没有找到对应的后端智能体工具。");
            missing.setErrorMessage("后端智能体工具不存在: " + toolName);
            missing.getNextActions().add("可以先问：你能做什么，查看当前已注册的后端工具目录。");
            return missing;
        }

        AgentToolDefinition target = matched.get();
        Map<String, Object> targetInput = targetInput(call.getInput().get("input"));
        AgentOperator operator = context == null ? null : context.operator();
        AgentPolicyDecision decision = policyEngine.evaluate(target, targetInput, operator);

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary(summary(target, decision));
        result.getArtifacts().add(new AgentChatArtifact(
                "agentPolicyDryRun",
                "智能体策略预检结果",
                descriptor(target, targetInput, operator, decision)
        ));
        result.getNextActions().add("策略预检不会执行目标工具；真实请求仍必须重新经过 orchestrator、policy、audit 和确认链路。");
        return result;
    }

    private Map<String, Object> descriptor(AgentToolDefinition target,
                                           Map<String, Object> input,
                                           AgentOperator operator,
                                           AgentPolicyDecision decision) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("toolName", target.getName());
        data.put("toolTitle", target.getTitle());
        data.put("input", input);
        data.put("operatorId", operator == null ? null : operator.id());
        data.put("operatorName", operator == null ? "" : normalize(operator.name()));
        data.put("operatorTenantId", operator == null ? "" : normalize(operator.tenantId()));
        data.put("allowed", decision.isAllowed());
        data.put("confirmationRequired", decision.isConfirmationRequired());
        data.put("confirmationText", normalize(decision.getConfirmationText()));
        data.put("errorCode", errorCode(decision.getErrorCode()));
        data.put("rejectionReason", normalize(decision.getRejectionReason()));
        data.put("nextActions", decision.getNextActions());
        data.put("riskLevel", target.getRiskLevel());
        data.put("riskCategory", target.getRiskCategory());
        data.put("destructive", target.isDestructive());
        data.put("requiredInputKeys", target.getRequiredInputKeys());
        data.put("requiredPermissions", target.getRequiredPermissions());
        data.put("requiredRoles", target.getRequiredRoles());
        data.put("tenantScope", target.getTenantScope());
        data.put("engine", "AgentPolicyEngine.evaluate");
        data.put("dryRunOnly", true);
        return data;
    }

    private String summary(AgentToolDefinition target, AgentPolicyDecision decision) {
        if (!decision.isAllowed()) {
            return "策略预检拒绝工具 " + target.getName() + "：" + normalize(decision.getRejectionReason());
        }
        if (decision.isConfirmationRequired()) {
            return "策略预检通过工具 " + target.getName() + "，但真实执行需要强确认。";
        }
        return "策略预检通过工具 " + target.getName() + "，真实执行仍会重新校验策略。";
    }

    private Map<String, Object> targetInput(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> input = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                input.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return input;
    }

    private boolean asksPolicyDryRun(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return containsAny(normalized, List.of("策略预检", "策略试跑", "policy dry run", "policy 预检", "policy 试跑", "dry run"))
                || (containsAny(normalized, List.of("如果调用", "如果执行")) && normalized.contains("policy"));
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
    }

    private String errorCode(AgentChatErrorCode errorCode) {
        return errorCode == null ? "" : errorCode.name();
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
