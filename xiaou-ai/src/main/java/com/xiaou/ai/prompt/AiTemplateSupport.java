package com.xiaou.ai.prompt;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 模板渲染与变量提取工具。
 *
 * @author xiaou
 */
final class AiTemplateSupport {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}");
    private static final Set<String> RESERVED_VARIABLES = Set.of(
            "current_date",
            "current_time",
            "current_date_time"
    );

    private AiTemplateSupport() {
    }

    static Set<String> extractVariables(String template) {
        if (!StringUtils.hasText(template)) {
            return Collections.emptySet();
        }

        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return Collections.unmodifiableSet(variables);
    }

    static boolean hasTemplateVariables(String template) {
        return !extractVariables(template).isEmpty();
    }

    static String render(String template, Map<String, ?> variables) {
        Assert.hasText(template, "模板内容不能为空");
        Prompt prompt = PromptTemplate.from(template).apply(prepareVariables(template, variables));
        return prompt.text();
    }

    static void assertVariableNames(Set<String> variableNames, String templateType) {
        for (String variableName : variableNames) {
            Assert.isTrue(
                    AiPromptGovernance.isValidVariableName(variableName),
                    () -> templateType + " 中存在非法变量名: " + variableName
            );
        }
    }

    private static Map<String, Object> prepareVariables(String template, Map<String, ?> variables) {
        Set<String> templateVariables = extractVariables(template);
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (String variableName : templateVariables) {
            if (!shouldBackfill(variableName, variables)) {
                continue;
            }
            Object value = variables == null ? null : variables.get(variableName);
            sanitized.put(variableName, sanitizeValue(value));
        }

        if (variables != null) {
            variables.forEach((name, value) -> sanitized.putIfAbsent(name, sanitizeValue(value)));
        }
        return sanitized;
    }

    private static boolean shouldBackfill(String variableName, Map<String, ?> variables) {
        return !RESERVED_VARIABLES.contains(variableName)
                || (variables != null && variables.containsKey(variableName));
    }

    private static Object sanitizeValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return StringUtils.hasText(text) ? text.trim() : "";
        }
        return value;
    }
}
