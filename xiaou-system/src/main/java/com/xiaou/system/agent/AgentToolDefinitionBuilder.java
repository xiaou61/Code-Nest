package com.xiaou.system.agent;

import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AgentTool 定义构造器，集中维护通用风险、权限和 schema 默认值。
 *
 * @author xiaou
 */
public final class AgentToolDefinitionBuilder {

    private final AgentToolDefinition definition;

    private AgentToolDefinitionBuilder(String name, String title) {
        definition = new AgentToolDefinition();
        definition.setName(trim(name));
        definition.setTitle(trim(title));
        definition.setIntent(trim(name));
        definition.setInputSchema(new LinkedHashMap<>());
    }

    public static AgentToolDefinitionBuilder readonly(String name, String title) {
        return new AgentToolDefinitionBuilder(name, title)
                .risk("readonly", "READONLY", false, false, null);
    }

    public static AgentToolDefinitionBuilder write(String name, String title) {
        return new AgentToolDefinitionBuilder(name, title)
                .risk("medium", "WRITE", true, true, null);
    }

    public AgentToolDefinitionBuilder description(String description) {
        definition.setDescription(trim(description));
        return this;
    }

    public AgentToolDefinitionBuilder intent(String intent) {
        definition.setIntent(trim(intent));
        return this;
    }

    public AgentToolDefinitionBuilder route(String route) {
        definition.setRoute(trim(route));
        return this;
    }

    public AgentToolDefinitionBuilder input(String key, Map<String, Object> schema, boolean required) {
        String normalizedKey = trim(key);
        definition.getInputSchema().put(normalizedKey, schema == null ? Map.of() : new LinkedHashMap<>(schema));
        if (required && !definition.getRequiredInputKeys().contains(normalizedKey)) {
            definition.getRequiredInputKeys().add(normalizedKey);
        }
        return this;
    }

    public AgentToolDefinitionBuilder permission(String permission) {
        String normalizedPermission = trim(permission);
        if (!definition.getRequiredPermissions().contains(normalizedPermission)) {
            definition.getRequiredPermissions().add(normalizedPermission);
        }
        return this;
    }

    public AgentToolDefinitionBuilder permissions(Collection<String> permissions) {
        if (permissions == null) {
            return this;
        }
        permissions.forEach(this::permission);
        return this;
    }

    public AgentToolDefinitionBuilder role(String role) {
        String normalizedRole = trim(role);
        if (!definition.getRequiredRoles().contains(normalizedRole)) {
            definition.getRequiredRoles().add(normalizedRole);
        }
        return this;
    }

    public AgentToolDefinitionBuilder tenantScope(String tenantScope) {
        definition.setTenantScope(trim(tenantScope));
        return this;
    }

    public AgentToolDefinitionBuilder confirmationText(String confirmationText) {
        definition.setConfirmationText(trim(confirmationText));
        return this;
    }

    public AgentToolDefinition build() {
        validateText(definition.getName(), "AgentTool definition.name must not be blank");
        validateText(definition.getTitle(), definition.getName() + " title must not be blank");
        validateText(definition.getDescription(), definition.getName() + " description must not be blank");
        validateText(definition.getIntent(), definition.getName() + " intent must not be blank");
        validateText(definition.getRoute(), definition.getName() + " route must not be blank");
        if (definition.getRequiredPermissions().isEmpty()) {
            throw new IllegalStateException(definition.getName() + " must declare at least one permission");
        }
        if (definition.isConfirmationRequired()) {
            validateText(definition.getConfirmationText(), definition.getName() + " confirmationText must not be blank");
        }
        return definition;
    }

    private AgentToolDefinitionBuilder risk(String riskLevel,
                                            String riskCategory,
                                            boolean destructive,
                                            boolean confirmationRequired,
                                            String confirmationText) {
        definition.setRiskLevel(riskLevel);
        definition.setRiskCategory(riskCategory);
        definition.setDestructive(destructive);
        definition.setConfirmationRequired(confirmationRequired);
        definition.setConfirmationText(confirmationText);
        return this;
    }

    private void validateText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
