package com.xiaou.system.agent.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatArtifact;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 失败恢复上下文的通用解析与 legacy fallback。
 *
 * @author xiaou
 */
final class AgentFailureRecoverySupport {

    static final String RECOVERY_QUERY_STATE = "查询目标对象当前状态，确认是否发生部分写入。";
    static final String RECOVERY_RETRY_WITH_NEW_PREVIEW = "修复外部依赖、权限或输入数据后，重新发起同一自然语言请求生成新的预览。";
    static final String RETRY_POLICY_NEW_PREVIEW_REQUIRED = "NEW_PREVIEW_REQUIRED";

    private static final Pattern AUDIT_ID_PATTERN = Pattern.compile("(agent-audit-[A-Za-z0-9_-]+)");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<AgentChatArtifact>> ARTIFACT_LIST_TYPE = new TypeReference<>() {
    };

    private AgentFailureRecoverySupport() {
    }

    static AgentChatArtifact resolveRecoveryArtifact(AgentAuditResponse audit, ObjectMapper objectMapper) {
        return parseRecoveryArtifact(audit == null ? null : audit.getResultJson(), objectMapper)
                .orElseGet(() -> fallbackRecoveryArtifact(audit));
    }

    static Optional<AgentChatArtifact> parseRecoveryArtifact(String resultJson, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(resultJson)) {
            return Optional.empty();
        }
        try {
            ObjectMapper mapper = objectMapper == null ? new ObjectMapper() : objectMapper;
            Map<String, Object> result = mapper.readValue(resultJson, MAP_TYPE);
            Object artifactsValue = result.get("artifacts");
            if (artifactsValue == null) {
                return Optional.empty();
            }
            List<AgentChatArtifact> artifacts = mapper.convertValue(artifactsValue, ARTIFACT_LIST_TYPE);
            return artifacts.stream()
                    .filter(artifact -> "failureRecovery".equals(artifact.getType()))
                    .findFirst();
        } catch (IllegalArgumentException | JsonProcessingException ignored) {
            return Optional.empty();
        }
    }

    static AgentChatArtifact fallbackRecoveryArtifact(AgentAuditResponse audit) {
        Map<String, Object> data = new LinkedHashMap<>();
        putIfPresent(data, "auditId", audit == null ? null : audit.getAuditId());
        putIfPresent(data, "idempotencyKey", audit == null ? null : audit.getIdempotencyKey());
        putIfPresent(data, "toolName", audit == null ? null : audit.getActionId());
        putIfPresent(data, "riskLevel", audit == null ? null : audit.getRiskLevel());
        putIfPresent(data, "riskCategory", audit == null ? null : audit.getRiskCategory());
        putIfPresent(data, "status", audit == null ? null : audit.getStatus());
        putIfPresent(data, "errorMessage", audit == null ? null : audit.getErrorMessage());
        data.put("retryPolicy", RETRY_POLICY_NEW_PREVIEW_REQUIRED);
        data.put("sameAuditRetryAllowed", false);
        data.put("requiresNewPreview", true);
        data.put("recommendedActions", List.of(
                RECOVERY_QUERY_STATE,
                RECOVERY_RETRY_WITH_NEW_PREVIEW
        ));
        return new AgentChatArtifact("failureRecovery", "失败恢复上下文", data);
    }

    static void appendRecommendedActions(AgentToolResult result, AgentChatArtifact recovery) {
        if (result == null) {
            return;
        }
        for (String action : recommendedActions(recovery)) {
            addIfMissing(result.getNextActions(), action);
        }
    }

    static List<String> recommendedActions(AgentChatArtifact recovery) {
        Object recommendedActions = recovery == null || recovery.getData() == null
                ? null
                : recovery.getData().get("recommendedActions");
        List<String> actions = new ArrayList<>();
        if (recommendedActions instanceof List<?> list) {
            for (Object action : list) {
                String value = normalize(action);
                if (StringUtils.hasText(value) && !actions.contains(value)) {
                    actions.add(value);
                }
            }
        }
        if (actions.isEmpty()) {
            actions.add(RECOVERY_QUERY_STATE);
            actions.add(RECOVERY_RETRY_WITH_NEW_PREVIEW);
        }
        return actions;
    }

    static String extractAuditId(String value) {
        Matcher matcher = AUDIT_ID_PATTERN.matcher(normalize(value));
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
    }

    static boolean containsAny(String value, List<String> keywords) {
        String normalized = normalize(value);
        return keywords.stream().anyMatch(normalized::contains);
    }

    static String stringValue(Map<String, Object> data, String key) {
        if (data == null || !data.containsKey(key)) {
            return "";
        }
        return normalize(data.get(key));
    }

    static boolean booleanValue(Map<String, Object> data, String key, boolean defaultValue) {
        if (data == null || !data.containsKey(key)) {
            return defaultValue;
        }
        Object value = data.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = normalize(value);
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        return defaultValue;
    }

    static String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static void addIfMissing(List<String> nextActions, String action) {
        if (nextActions != null && StringUtils.hasText(action) && !nextActions.contains(action)) {
            nextActions.add(action);
        }
    }

    private static void putIfPresent(Map<String, Object> data, String key, String value) {
        if (StringUtils.hasText(value)) {
            data.put(key, value);
        }
    }
}
