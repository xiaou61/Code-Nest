package com.xiaou.system.agent;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 智能体策略引擎，集中处理风险、确认和执行准入。
 *
 * @author xiaou
 */
@Component
public class AgentPolicyEngine {

    public AgentPolicyDecision evaluate(AgentToolDefinition definition, Map<String, Object> input) {
        return evaluate(definition, input, null);
    }

    public AgentPolicyDecision evaluate(AgentToolDefinition definition, Map<String, Object> input, AgentOperator operator) {
        if (definition == null) {
            return AgentPolicyDecision.reject("工具不存在");
        }

        AgentPolicyDecision schemaDecision = validateInput(definition, input);
        if (!schemaDecision.isAllowed()) {
            return schemaDecision;
        }

        AgentPolicyDecision accessDecision = validateAccess(definition, input, operator);
        if (!accessDecision.isAllowed()) {
            return accessDecision;
        }

        if (!requiresConfirmation(definition)) {
            return AgentPolicyDecision.allowReadonly();
        }

        if (!StringUtils.hasText(definition.getConfirmationText())) {
            return AgentPolicyDecision.reject("写入工具缺少强确认文本配置");
        }

        return AgentPolicyDecision.requireConfirmation(definition.getConfirmationText());
    }

    public boolean confirmationMatches(String expected, String actual) {
        return StringUtils.hasText(expected)
                && StringUtils.hasText(actual)
                && expected.trim().equals(actual.trim());
    }

    private AgentPolicyDecision validateInput(AgentToolDefinition definition, Map<String, Object> input) {
        Map<String, Object> safeInput = input == null ? Map.of() : input;
        List<String> requiredKeys = definition.getRequiredInputKeys() == null ? List.of() : definition.getRequiredInputKeys();
        for (String key : requiredKeys) {
            if (!safeInput.containsKey(key) || safeInput.get(key) == null) {
                return AgentPolicyDecision.reject(
                        AgentChatErrorCode.SCHEMA_VALIDATION_FAILED,
                        "工具输入缺少必填字段: " + key
                );
            }
        }

        Map<String, Object> schema = definition.getInputSchema() == null ? Map.of() : definition.getInputSchema();
        for (String key : safeInput.keySet()) {
            if (!schema.containsKey(key)) {
                return AgentPolicyDecision.reject(
                        AgentChatErrorCode.SCHEMA_VALIDATION_FAILED,
                        "工具输入包含未声明字段: " + key
                );
            }
        }

        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            String key = entry.getKey();
            if (!safeInput.containsKey(key) || safeInput.get(key) == null) {
                continue;
            }
            if (!(entry.getValue() instanceof Map<?, ?> fieldSchema)) {
                continue;
            }
            AgentPolicyDecision decision = validateField(key, safeInput.get(key), fieldSchema);
            if (!decision.isAllowed()) {
                return decision;
            }
        }
        return AgentPolicyDecision.allowReadonly();
    }

    private AgentPolicyDecision validateAccess(AgentToolDefinition definition, Map<String, Object> input, AgentOperator operator) {
        List<String> missingPermissions = requiredPermissions(definition).stream()
                .filter(permission -> operator == null || !operator.hasPermission(permission))
                .toList();
        if (!missingPermissions.isEmpty()) {
            return AgentPolicyDecision.reject("当前管理员缺少权限: " + String.join("、", missingPermissions));
        }

        List<String> requiredRoles = requiredRoles(definition);
        if (!requiredRoles.isEmpty() && requiredRoles.stream().noneMatch(role -> operator != null && operator.hasRole(role))) {
            return AgentPolicyDecision.reject("当前管理员缺少角色，至少需要其一: " + String.join("、", requiredRoles));
        }

        if (requiresSameTenant(definition)) {
            String operatorTenantId = operator == null ? "" : normalize(operator.tenantId());
            String inputTenantId = input == null ? "" : normalize(String.valueOf(input.getOrDefault("tenantId", "")));
            if (!StringUtils.hasText(operatorTenantId) || !StringUtils.hasText(inputTenantId) || !operatorTenantId.equals(inputTenantId)) {
                return AgentPolicyDecision.reject("当前管理员不能操作其它租户范围内的数据");
            }
        }

        return AgentPolicyDecision.allowReadonly();
    }

    private List<String> requiredPermissions(AgentToolDefinition definition) {
        return definition.getRequiredPermissions() == null ? List.of() : definition.getRequiredPermissions().stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> requiredRoles(AgentToolDefinition definition) {
        return definition.getRequiredRoles() == null ? List.of() : definition.getRequiredRoles().stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .toList();
    }

    private boolean requiresSameTenant(AgentToolDefinition definition) {
        return "SAME_TENANT".equalsIgnoreCase(normalize(definition.getTenantScope()));
    }

    private boolean requiresConfirmation(AgentToolDefinition definition) {
        return definition.isConfirmationRequired()
                || definition.isDestructive()
                || isWriteRisk(definition);
    }

    private boolean isWriteRisk(AgentToolDefinition definition) {
        String riskLevel = normalize(definition.getRiskLevel()).toLowerCase(Locale.ROOT);
        String riskCategory = normalize(definition.getRiskCategory()).toUpperCase(Locale.ROOT);
        return (StringUtils.hasText(riskLevel) && !"readonly".equals(riskLevel))
                || (StringUtils.hasText(riskCategory) && !"READONLY".equals(riskCategory));
    }

    private AgentPolicyDecision validateField(String key, Object value, Map<?, ?> fieldSchema) {
        Object typeValue = fieldSchema.get("type");
        String type = typeValue == null ? "" : String.valueOf(typeValue);
        if (StringUtils.hasText(type) && !matchesType(value, type)) {
            return AgentPolicyDecision.reject(
                    AgentChatErrorCode.SCHEMA_VALIDATION_FAILED,
                    "工具输入字段 " + key + " 类型不符合 schema: " + type
            );
        }

        if (value instanceof Number number) {
            Object minimum = fieldSchema.get("minimum");
            if (minimum instanceof Number min && number.doubleValue() < min.doubleValue()) {
                return AgentPolicyDecision.reject(
                        AgentChatErrorCode.SCHEMA_VALIDATION_FAILED,
                        "工具输入字段 " + key + " 必须大于等于 " + min
                );
            }
            Object maximum = fieldSchema.get("maximum");
            if (maximum instanceof Number max && number.doubleValue() > max.doubleValue()) {
                return AgentPolicyDecision.reject(
                        AgentChatErrorCode.SCHEMA_VALIDATION_FAILED,
                        "工具输入字段 " + key + " 必须小于等于 " + max
                );
            }
        }

        if (value instanceof CharSequence text) {
            Object minLength = fieldSchema.get("minLength");
            if (minLength instanceof Number min && text.length() < min.intValue()) {
                return AgentPolicyDecision.reject(
                        AgentChatErrorCode.SCHEMA_VALIDATION_FAILED,
                        "工具输入字段 " + key + " 长度必须大于等于 " + min
                );
            }
            Object maxLength = fieldSchema.get("maxLength");
            if (maxLength instanceof Number max && text.length() > max.intValue()) {
                return AgentPolicyDecision.reject(
                        AgentChatErrorCode.SCHEMA_VALIDATION_FAILED,
                        "工具输入字段 " + key + " 长度必须小于等于 " + max
                );
            }
        }

        Object enumValues = fieldSchema.get("enum");
        if (enumValues instanceof Collection<?> allowedValues
                && !allowedValues.isEmpty()
                && !allowedValues.contains(value)) {
            return AgentPolicyDecision.reject(
                    AgentChatErrorCode.SCHEMA_VALIDATION_FAILED,
                    "工具输入字段 " + key + " 不在允许范围: " + allowedValues
            );
        }

        return AgentPolicyDecision.allowReadonly();
    }

    private boolean matchesType(Object value, String type) {
        return switch (type) {
            case "integer" -> value instanceof Number number && Math.rint(number.doubleValue()) == number.doubleValue();
            case "number" -> value instanceof Number;
            case "string" -> value instanceof CharSequence;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof Collection<?>;
            case "object" -> value instanceof Map<?, ?>;
            default -> true;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
