package com.xiaou.sre.service;

import java.util.regex.Pattern;

/**
 * 冻结模型上下文的统一安全策略。
 *
 * @author xiaou
 */
public final class SreReplayContextPolicy {

    private static final Pattern CONTROL_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+(?!\\[REDACTED])[-A-Za-z0-9._~+/=]{6,}");
    private static final Pattern STANDALONE_CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)\\b(?:sk-[A-Za-z0-9_-]{16,}|gh[pousr]_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16})\\b");
    private static final Pattern JSON_SECRET_VALUE_PATTERN = Pattern.compile(
            "(?i)\\\"[A-Za-z0-9_.-]*(?:authorization|password|passwd|token|secret|apikey|cookie|credential|privatekey)\\\""
                    + "\\s*:\\s*\\\"(?!\\[REDACTED])[^\\\"]{4,}\\\"");

    private SreReplayContextPolicy() {
    }

    public static boolean containsUnsafeContent(String contextJson) {
        return contextJson != null
                && (CONTROL_PATTERN.matcher(contextJson).find()
                || BEARER_PATTERN.matcher(contextJson).find()
                || STANDALONE_CREDENTIAL_PATTERN.matcher(contextJson).find()
                || JSON_SECRET_VALUE_PATTERN.matcher(contextJson).find());
    }
}
