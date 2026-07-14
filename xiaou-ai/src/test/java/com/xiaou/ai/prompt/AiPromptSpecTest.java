package com.xiaou.ai.prompt;

import com.xiaou.ai.prompt.admin.AdminAgentPromptSpecs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPromptSpecTest {

    @Test
    void shouldRegisterUniquePromptIds() {
        List<AiPromptSpec> specs = AiPromptCatalog.all();
        Set<String> ids = specs.stream()
                .map(AiPromptSpec::promptId)
                .collect(Collectors.toSet());

        assertEquals(specs.size(), ids.size(), "PromptSpec 不允许重复，避免治理、观测和结构化契约串线");
    }

    @Test
    void shouldRenderAllPromptFixturesWithoutTemplateMarkers() {
        for (AiPromptSpec spec : AiPromptCatalog.all()) {
            String rendered = spec.renderUser(AiPromptFixtures.variables(spec));

            assertFalse(rendered.contains("{{"), () -> "Prompt 样例缺少变量: " + spec.promptId());
            assertFalse(rendered.contains("}}"), () -> "Prompt 样例缺少变量: " + spec.promptId());
        }
    }

    @Test
    void shouldExposeAdminAgentPlannerPromptContract() {
        AiPromptSpec spec = AdminAgentPromptSpecs.PLAN;

        assertEquals("admin_agent.plan", spec.key());
        assertEquals("v1", spec.version());
        assertEquals(512, spec.maxCompletionTokens());
        assertEquals(Set.of("message", "sessionContextJson", "toolsJson"), spec.templateVariables());
        assertTrue(spec.systemPrompt().contains("不能执行工具"));
        assertTrue(spec.systemPrompt().contains("missingFields"));
        assertTrue(spec.systemPrompt().contains("外层主动作"));
        assertTrue(spec.systemPrompt().contains("不要直接选择内层目标工具"));
    }

    @Test
    void shouldRejectTemplateVariablesInsideSystemPrompt() {
        assertThrows(IllegalArgumentException.class, () -> AiPromptSpec.of(
                "test.invalid_system",
                "v1",
                "不要在 system prompt 使用 {{name}}",
                "用户输入：{{input}}"
        ));
    }

    @Test
    void shouldRejectPromptWithoutUserTemplateVariables() {
        assertThrows(IllegalArgumentException.class, () -> AiPromptSpec.of(
                "test.no_variable",
                "v1",
                "合法 system prompt",
                "这里没有变量"
        ));
    }

    @Test
    void shouldRejectNonPositiveCompletionBudget() {
        assertThrows(IllegalArgumentException.class, () -> AiPromptSpec.of(
                "test.invalid_budget",
                "v1",
                "合法 system prompt",
                "用户输入：{{input}}",
                0
        ));
    }
}
