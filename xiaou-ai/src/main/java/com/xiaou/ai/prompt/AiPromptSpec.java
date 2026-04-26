package com.xiaou.ai.prompt;

import org.springframework.util.Assert;

import java.util.Map;
import java.util.Set;

/**
 * 统一 Prompt 规范定义。
 *
 * <p>约束 system prompt 与 user template 分离，所有变量通过命名占位符渲染，
 * 便于版本治理、测试验证与后续观测埋点。</p>
 *
 * @author xiaou
 */
public record AiPromptSpec(String key,
                           String version,
                           String systemPrompt,
                           String userTemplate) {

    public AiPromptSpec {
        Assert.hasText(key, "prompt key 不能为空");
        Assert.hasText(version, "prompt version 不能为空");
        Assert.hasText(systemPrompt, "system prompt 不能为空");
        Assert.hasText(userTemplate, "user template 不能为空");

        key = key.trim();
        version = version.trim();
        systemPrompt = systemPrompt.trim();
        userTemplate = userTemplate.trim();

        AiPromptGovernance.validatePromptSpec(key, version, systemPrompt, userTemplate);
    }

    public static AiPromptSpec of(String key, String version, String systemPrompt, String userTemplate) {
        return new AiPromptSpec(key, version, systemPrompt, userTemplate);
    }

    public String promptId() {
        return key + ":" + version;
    }

    public Set<String> templateVariables() {
        return AiTemplateSupport.extractVariables(userTemplate);
    }

    public String renderUser(Map<String, ?> variables) {
        return AiTemplateSupport.render(userTemplate, variables);
    }
}
