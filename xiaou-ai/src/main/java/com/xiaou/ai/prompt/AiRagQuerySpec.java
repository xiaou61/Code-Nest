package com.xiaou.ai.prompt;

import org.springframework.util.Assert;

import java.util.Map;
import java.util.Set;

/**
 * LlamaIndex 检索查询模板定义。
 *
 * @author xiaou
 */
public record AiRagQuerySpec(String key,
                             String version,
                             String template) {

    public AiRagQuerySpec {
        Assert.hasText(key, "rag query key 不能为空");
        Assert.hasText(version, "rag query version 不能为空");
        Assert.hasText(template, "rag query template 不能为空");

        key = key.trim();
        version = version.trim();
        template = template.trim();

        AiPromptGovernance.validateRagQuerySpec(key, version, template);
    }

    public static AiRagQuerySpec of(String key, String version, String template) {
        return new AiRagQuerySpec(key, version, template);
    }

    public String queryId() {
        return key + ":" + version;
    }

    public Set<String> templateVariables() {
        return AiTemplateSupport.extractVariables(template);
    }

    public String render(Map<String, ?> variables) {
        return AiTemplateSupport.render(template, variables);
    }
}
