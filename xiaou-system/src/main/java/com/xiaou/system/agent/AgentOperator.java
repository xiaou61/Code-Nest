package com.xiaou.system.agent;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 当前智能体操作者。
 *
 * @author xiaou
 */
public record AgentOperator(
        Long id,
        String name,
        String tenantId,
        List<String> roles,
        List<String> permissions
) {

    public AgentOperator(Long id, String name) {
        this(id, name, "", List.of(), List.of());
    }

    public AgentOperator {
        tenantId = normalize(tenantId);
        roles = normalizeList(roles);
        permissions = normalizeList(permissions);
    }

    public boolean hasRole(String role) {
        return roles.contains(normalize(role));
    }

    public boolean hasPermission(String permission) {
        String normalizedPermission = normalize(permission);
        return permissions.contains(normalizedPermission) || permissions.contains("*");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                normalized.add(value.trim());
            }
        }
        return List.copyOf(new ArrayList<>(normalized));
    }
}
