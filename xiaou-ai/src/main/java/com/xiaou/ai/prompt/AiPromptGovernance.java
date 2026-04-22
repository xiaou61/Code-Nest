package com.xiaou.ai.prompt;

import org.springframework.util.Assert;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * AI Prompt 与检索查询治理规则。
 *
 * @author xiaou
 */
final class AiPromptGovernance {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v\\d+$");
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9_]*$");

    private AiPromptGovernance() {
    }

    static void validatePromptSpec(String key, String version, String systemPrompt, String userTemplate) {
        assertKey(key, "prompt key");
        assertVersion(version, "prompt version");
        Assert.isTrue(
                !AiTemplateSupport.hasTemplateVariables(systemPrompt),
                "system prompt 不能混入模板变量，请把动态内容放到 user template 中"
        );

        Set<String> variables = AiTemplateSupport.extractVariables(userTemplate);
        Assert.isTrue(!variables.isEmpty(), "user template 至少需要一个命名变量");
        AiTemplateSupport.assertVariableNames(variables, "user template");
    }

    static void validateRagQuerySpec(String key, String version, String template) {
        assertKey(key, "rag query key");
        assertVersion(version, "rag query version");

        Set<String> variables = AiTemplateSupport.extractVariables(template);
        Assert.isTrue(!variables.isEmpty(), "rag query template 至少需要一个命名变量");
        AiTemplateSupport.assertVariableNames(variables, "rag query template");
    }

    static boolean isValidVariableName(String variableName) {
        return variableName != null && VARIABLE_NAME_PATTERN.matcher(variableName).matches();
    }

    private static void assertKey(String value, String fieldName) {
        Assert.isTrue(
                value != null && KEY_PATTERN.matcher(value).matches(),
                () -> fieldName + " 格式非法，要求使用小写业务域命名，例如 sql_optimize.rewrite"
        );
    }

    private static void assertVersion(String value, String fieldName) {
        Assert.isTrue(
                value != null && VERSION_PATTERN.matcher(value).matches(),
                () -> fieldName + " 格式非法，要求使用 v1/v2 这类显式版本号"
        );
    }
}
