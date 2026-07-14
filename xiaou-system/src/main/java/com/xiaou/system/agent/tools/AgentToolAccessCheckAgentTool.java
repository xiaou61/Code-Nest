package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
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
 * 查询当前操作者对指定智能体工具的访问条件。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentToolAccessCheckAgentTool implements AgentTool {

    private final AgentToolCatalogService catalogService;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.tools.access_check");
        definition.setTitle("检查当前操作者工具访问");
        definition.setDescription("检查当前请求操作者是否满足某个后端智能体工具声明的权限、角色和租户策略。");
        definition.setIntent("system.agent.tools.access_check");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of(
                "toolName", Map.of("type", "string", "minLength", 1, "maxLength", 160)
        ));
        definition.setRequiredInputKeys(List.of("toolName"));
        definition.setRequiredPermissions(List.of("agent:runtime:tool-access:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!asksAccessCheck(normalized)) {
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
                    call.setSummary("检查当前操作者对后端智能体工具的访问条件");
                    call.setInput(input);
                    return call;
                });
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("检查工具访问条件是只读动作，不需要写入预览。");
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
        AgentOperator operator = context == null ? null : context.operator();
        List<String> requiredPermissions = normalizedList(target.getRequiredPermissions());
        List<String> requiredRoles = normalizedList(target.getRequiredRoles());
        List<String> missingPermissions = requiredPermissions.stream()
                .filter(permission -> operator == null || !operator.hasPermission(permission))
                .toList();
        List<String> missingRoles = missingRoles(operator, requiredRoles);
        boolean tenantRuntimeCheckRequired = "SAME_TENANT".equalsIgnoreCase(normalize(target.getTenantScope()));
        boolean accessAllowed = missingPermissions.isEmpty() && missingRoles.isEmpty();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("toolName", target.getName());
        data.put("toolTitle", target.getTitle());
        data.put("accessAllowed", accessAllowed);
        data.put("operatorId", operator == null ? null : operator.id());
        data.put("operatorName", operator == null ? "" : normalize(operator.name()));
        data.put("operatorTenantId", operator == null ? "" : normalize(operator.tenantId()));
        data.put("requiredPermissions", requiredPermissions);
        data.put("missingPermissions", missingPermissions);
        data.put("requiredRoles", requiredRoles);
        data.put("missingRoles", missingRoles);
        data.put("tenantScope", normalize(target.getTenantScope()));
        data.put("tenantRuntimeCheckRequired", tenantRuntimeCheckRequired);
        data.put("riskLevel", target.getRiskLevel());
        data.put("riskCategory", target.getRiskCategory());
        data.put("destructive", target.isDestructive());
        data.put("requiresConfirmation", requiresConfirmation(target));
        data.put("confirmationText", target.getConfirmationText());
        data.put("requiredInputKeys", target.getRequiredInputKeys());
        data.put("notes", notes(tenantRuntimeCheckRequired));

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary(accessAllowed
                ? "当前操作者可以访问工具 " + target.getName() + "；真实执行时仍会校验输入 schema、租户和确认策略。"
                : "当前操作者暂时不能访问工具 " + target.getName() + "，请查看缺失权限或角色。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentToolAccessCheck",
                "当前操作者工具访问检查",
                data
        ));
        result.getNextActions().add("如果访问被拒绝，可以先补齐权限种子或角色授权，再重新通过统一聊天入口发起请求。");
        return result;
    }

    private boolean asksAccessCheck(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return containsAny(normalized, List.of("我能不能用", "我能否使用", "我可以用", "有没有权限用", "是否可以使用", "访问检查", "权限检查"));
    }

    private List<String> missingRoles(AgentOperator operator, List<String> requiredRoles) {
        if (requiredRoles.isEmpty()) {
            return List.of();
        }
        boolean matched = requiredRoles.stream().anyMatch(role -> operator != null && operator.hasRole(role));
        return matched ? List.of() : requiredRoles;
    }

    private List<String> notes(boolean tenantRuntimeCheckRequired) {
        if (!tenantRuntimeCheckRequired) {
            return List.of("这是静态访问自检；真实执行仍由 AgentPolicyEngine 重新校验。");
        }
        return List.of(
                "这是静态访问自检；真实执行仍由 AgentPolicyEngine 重新校验。",
                "该工具要求 SAME_TENANT，真实执行时还会校验输入 tenantId 与当前操作者租户一致。"
        );
    }

    private boolean requiresConfirmation(AgentToolDefinition definition) {
        return definition.isConfirmationRequired()
                || definition.isDestructive()
                || !"readonly".equalsIgnoreCase(normalize(definition.getRiskLevel()))
                || !"READONLY".equalsIgnoreCase(normalize(definition.getRiskCategory()));
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
    }

    private List<String> normalizedList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
