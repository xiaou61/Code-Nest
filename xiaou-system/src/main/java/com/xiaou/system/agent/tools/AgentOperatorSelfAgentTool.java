package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 查询当前智能体操作者上下文的通用只读工具。
 *
 * @author xiaou
 */
@Component
public class AgentOperatorSelfAgentTool implements AgentTool {

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.operator.self");
        definition.setTitle("查询当前操作者上下文");
        definition.setDescription("查询当前请求在智能体后端运行时中的操作者身份、角色、权限和租户上下文。");
        definition.setIntent("system.agent.operator.self");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of());
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:operator:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message).toLowerCase(Locale.ROOT);
        boolean asksSelfAccess = containsAny(normalized, List.of(
                "我在智能体",
                "当前智能体操作者",
                "当前操作者",
                "当前操作员",
                "我的角色和权限",
                "我的权限",
                "我的角色",
                "operator self"
        ));
        boolean asksRuntimeAccess = containsAny(normalized, List.of("我", "当前"))
                && containsAny(normalized, List.of("智能体", "agent", "运行时"))
                && containsAny(normalized, List.of("角色", "权限", "操作者", "操作员"));
        if (!asksSelfAccess && !asksRuntimeAccess) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("查询当前智能体操作者上下文");
        call.setInput(new LinkedHashMap<>());
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("查询当前操作者上下文是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        AgentOperator operator = context == null ? null : context.operator();
        List<String> roles = sorted(operator == null ? List.of() : operator.roles());
        List<String> permissions = sorted(operator == null ? List.of() : operator.permissions());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operatorId", operator == null ? null : operator.id());
        data.put("operatorName", operator == null ? "" : normalize(operator.name()));
        data.put("tenantId", operator == null ? "" : normalize(operator.tenantId()));
        data.put("roleCount", roles.size());
        data.put("permissionCount", permissions.size());
        data.put("roles", roles);
        data.put("permissions", permissions);
        data.put("hasWildcardPermission", permissions.contains("*"));
        data.put("selfOnly", true);
        data.put("source", "AgentExecutionContext.operator");

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary(summary(operator, roles, permissions));
        result.getArtifacts().add(new AgentChatArtifact(
                "agentOperatorSelf",
                "当前智能体操作者上下文",
                data
        ));
        result.getNextActions().add("如果某个工具被拒绝，可以继续查询该工具详情，比对 requiredPermissions 与当前权限。");
        return result;
    }

    private String summary(AgentOperator operator, List<String> roles, List<String> permissions) {
        String name = operator == null ? "" : normalize(operator.name());
        if (name.isEmpty()) {
            return "当前请求没有加载操作者名称，已返回空的智能体操作者上下文。";
        }
        return "当前智能体操作者是 " + name + "，拥有 " + roles.size() + " 个角色、"
                + permissions.size() + " 个智能体权限。";
    }

    private List<String> sorted(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::normalize)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
