package com.xiaou.system.agent.tools;

import com.xiaou.system.agent.AgentExecutionContext;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 校验后端智能体工具定义契约的通用只读工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class AgentToolDefinitionValidateAgentTool implements AgentTool {

    private static final Set<String> TENANT_SCOPES = Set.of("ANY", "SAME_TENANT");

    private final AgentToolCatalogService catalogService;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("system.agent.tools.validate");
        definition.setTitle("校验智能体工具定义");
        definition.setDescription("校验当前后端统一智能体工具目录中的 definition 契约、schema、权限和确认策略。");
        definition.setIntent("system.agent.tools.validate");
        definition.setRoute("/admin/agent/chat");
        definition.setRiskLevel("readonly");
        definition.setRiskCategory("READONLY");
        definition.setInputSchema(Map.of(
                "toolName", Map.of("type", "string", "minLength", 1, "maxLength", 160)
        ));
        definition.setRequiredInputKeys(List.of());
        definition.setRequiredPermissions(List.of("agent:runtime:tool-catalog:read"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message).toLowerCase(Locale.ROOT);
        if (!asksValidation(normalized)) {
            return Optional.empty();
        }

        Map<String, Object> input = new LinkedHashMap<>();
        catalogService.definitions().stream()
                .map(AgentToolDefinition::getName)
                .filter(StringUtils::hasText)
                .filter(normalized::contains)
                .findFirst()
                .ifPresent(toolName -> input.put("toolName", toolName));

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("校验后端智能体工具定义契约");
        call.setInput(input);
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        AgentToolPreview preview = new AgentToolPreview();
        preview.setSummary("校验智能体工具定义是只读动作，不需要写入预览。");
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        String toolName = normalize(call == null || call.getInput() == null ? null : call.getInput().get("toolName"));
        List<AgentToolDefinition> definitions = catalogService.definitions();
        List<AgentToolDefinition> selected = selectedDefinitions(definitions, toolName);
        if (StringUtils.hasText(toolName) && selected.isEmpty()) {
            AgentToolResult missing = new AgentToolResult();
            missing.setSuccess(false);
            missing.setSummary("没有找到对应的后端智能体工具。");
            missing.setErrorMessage("后端智能体工具不存在: " + toolName);
            missing.getNextActions().add("可以先问：你能做什么，查看当前已注册的后端工具目录。");
            return missing;
        }

        Map<String, Long> nameCounts = definitions.stream()
                .map(AgentToolDefinition::getName)
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> issues = new ArrayList<>();
        for (AgentToolDefinition definition : selected) {
            validateDefinition(definition, nameCounts, issues);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", issues.isEmpty() ? "PASS" : "FAILED");
        data.put("toolName", toolName);
        data.put("totalCount", selected.size());
        data.put("issueCount", issues.size());
        data.put("validCount", selected.size() - invalidToolCount(issues));
        data.put("issues", issues);
        data.put("checkedRules", List.of(
                "required metadata text",
                "route starts with /",
                "tenantScope is ANY or SAME_TENANT",
                "requiredInputKeys exist in inputSchema",
                "inputSchema fields declare type",
                "requiredPermissions use agent:* namespace",
                "write/destructive tools require confirmation text",
                "readonly tools do not require confirmation",
                "tool names are unique"
        ));

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary(issues.isEmpty()
                ? "智能体工具定义契约全部通过，共校验 " + selected.size() + " 个工具。"
                : "智能体工具定义契约发现 " + issues.size() + " 个问题，请先修复后再继续扩展。");
        result.getArtifacts().add(new AgentChatArtifact(
                "agentToolDefinitionValidation",
                "智能体工具定义契约校验结果",
                data
        ));
        result.getNextActions().add(issues.isEmpty()
                ? "新增工具时继续遵守 AgentToolDefinition 契约，并补 planner fixture。"
                : "优先修复 schema、权限和确认策略问题，再运行后端智能体回归。");
        return result;
    }

    private List<AgentToolDefinition> selectedDefinitions(List<AgentToolDefinition> definitions, String toolName) {
        List<AgentToolDefinition> safeDefinitions = definitions == null ? List.of() : definitions;
        if (!StringUtils.hasText(toolName)) {
            return safeDefinitions;
        }
        return safeDefinitions.stream()
                .filter(definition -> toolName.equals(normalize(definition.getName())))
                .toList();
    }

    private void validateDefinition(AgentToolDefinition definition,
                                    Map<String, Long> nameCounts,
                                    List<Map<String, Object>> issues) {
        String name = normalize(definition == null ? null : definition.getName());
        if (!StringUtils.hasText(name)) {
            addIssue(issues, name, "name", "ERROR", "definition.name must not be blank");
            return;
        }
        if (nameCounts.getOrDefault(name, 0L) > 1) {
            addIssue(issues, name, "name", "ERROR", "duplicate tool name: " + name);
        }

        requireText(issues, name, "title", definition.getTitle());
        requireText(issues, name, "description", definition.getDescription());
        requireText(issues, name, "intent", definition.getIntent());
        requireText(issues, name, "riskLevel", definition.getRiskLevel());
        requireText(issues, name, "riskCategory", definition.getRiskCategory());

        String route = normalize(definition.getRoute());
        if (!StringUtils.hasText(route) || !route.startsWith("/")) {
            addIssue(issues, name, "route", "ERROR", "route must start with /");
        }

        if (!TENANT_SCOPES.contains(normalize(definition.getTenantScope()))) {
            addIssue(issues, name, "tenantScope", "ERROR", "tenantScope must be ANY or SAME_TENANT");
        }

        validateSchema(issues, name, definition);
        validateAccess(issues, name, definition);
        validateConfirmation(issues, name, definition);
    }

    private void validateSchema(List<Map<String, Object>> issues, String name, AgentToolDefinition definition) {
        Map<String, Object> schema = definition.getInputSchema() == null ? Map.of() : definition.getInputSchema();
        for (String key : safeList(definition.getRequiredInputKeys())) {
            if (!schema.containsKey(key)) {
                addIssue(issues, name, "requiredInputKeys", "ERROR",
                        "required input key is missing from inputSchema: " + key);
            }
        }
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            String fieldName = normalize(entry.getKey());
            if (!StringUtils.hasText(fieldName)) {
                addIssue(issues, name, "inputSchema", "ERROR", "inputSchema field name must not be blank");
                continue;
            }
            if (!(entry.getValue() instanceof Map<?, ?> fieldSchema)) {
                addIssue(issues, name, "inputSchema." + fieldName, "ERROR", "inputSchema field must be an object");
                continue;
            }
            Object type = fieldSchema.get("type");
            if (!(type instanceof String typeText) || !StringUtils.hasText(typeText)) {
                addIssue(issues, name, "inputSchema." + fieldName, "ERROR", "inputSchema field must declare type");
            }
        }
    }

    private void validateAccess(List<Map<String, Object>> issues, String name, AgentToolDefinition definition) {
        List<String> permissions = safeList(definition.getRequiredPermissions());
        if (permissions.isEmpty()) {
            addIssue(issues, name, "requiredPermissions", "ERROR", "requiredPermissions must not be empty");
            return;
        }
        for (String permission : permissions) {
            if (!permission.startsWith("agent:")) {
                addIssue(issues, name, "requiredPermissions", "ERROR",
                        "permission must use agent:* namespace: " + permission);
            }
        }
    }

    private void validateConfirmation(List<Map<String, Object>> issues, String name, AgentToolDefinition definition) {
        boolean readonly = "READONLY".equals(normalize(definition.getRiskCategory()))
                && "readonly".equalsIgnoreCase(normalize(definition.getRiskLevel()))
                && !definition.isDestructive();
        if (readonly) {
            if (definition.isConfirmationRequired()) {
                addIssue(issues, name, "confirmation", "ERROR", "readonly tool should not require confirmation");
            }
            return;
        }

        if (!definition.isConfirmationRequired()) {
            addIssue(issues, name, "confirmation", "ERROR", "write/destructive tool must require confirmation");
        }
        if (!StringUtils.hasText(definition.getConfirmationText())) {
            addIssue(issues, name, "confirmationText", "ERROR", "write/destructive tool must declare confirmationText");
        }
    }

    private int invalidToolCount(List<Map<String, Object>> issues) {
        return (int) issues.stream()
                .map(issue -> normalize(issue.get("toolName")))
                .filter(StringUtils::hasText)
                .distinct()
                .count();
    }

    private void requireText(List<Map<String, Object>> issues, String toolName, String field, String value) {
        if (!StringUtils.hasText(value)) {
            addIssue(issues, toolName, field, "ERROR", field + " must not be blank");
        }
    }

    private void addIssue(List<Map<String, Object>> issues, String toolName, String field, String severity, String message) {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("toolName", normalize(toolName));
        issue.put("field", field);
        issue.put("severity", severity);
        issue.put("message", message);
        issues.add(issue);
    }

    private boolean asksValidation(String message) {
        return containsAny(message, List.of("工具定义自检", "工具契约", "工具校验", "工具定义是否合规", "definition validate", "contract check"))
                || (message.contains("工具") && containsAny(message, List.of("自检", "校验", "合规", "检查")));
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(value::contains);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
