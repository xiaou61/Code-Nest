package com.xiaou.ai.prompt;

import com.xiaou.ai.prompt.community.CommunityPromptSpecs;
import com.xiaou.ai.prompt.interview.InterviewPromptSpecs;
import com.xiaou.ai.prompt.jobbattle.JobBattlePromptSpecs;
import com.xiaou.ai.prompt.sql.SqlOptimizePromptSpecs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPromptSpecTest {

    @Test
    void shouldRenderInterviewPromptWithNamedVariablesAndRagSection() {
        String prompt = InterviewPromptSpecs.EVALUATE_ANSWER.renderUser(Map.of(
                "direction", "Java后端",
                "level", "高级",
                "style", "压力型",
                "followUpCount", 1,
                "question", "你如何设计高并发库存扣减？",
                "answer", "我会结合 Redis 预扣和数据库最终一致性。",
                "ragSection", AiPromptSections.ragSection("库存扣减要关注幂等与超卖控制")
        ));

        assertTrue(prompt.contains("Java后端"));
        assertTrue(prompt.contains("高并发库存扣减"));
        assertTrue(prompt.contains("超卖控制"));
    }

    @Test
    void shouldNotRenderNullLiteralWhenOptionalVariablesAreMissing() {
        String prompt = CommunityPromptSpecs.POST_SUMMARY.renderUser(Map.of(
                "content", "这是一篇关于 SQL 优化的帖子"
        ));

        assertFalse(prompt.contains("null"));
        assertFalse(prompt.contains("{{title}}"));
        assertTrue(prompt.contains("SQL 优化"));
    }

    @Test
    void shouldKeepPromptVersionedAcrossDifferentScenes() {
        String jobBattlePromptId = JobBattlePromptSpecs.PLAN_GENERATE.promptId();
        String sqlPromptId = SqlOptimizePromptSpecs.REWRITE.promptId();

        assertTrue(jobBattlePromptId.startsWith("job_battle.plan_generate:"));
        assertTrue(sqlPromptId.startsWith("sql_optimize.rewrite:"));
    }

    @Test
    void shouldRegisterUniquePromptIdsAcrossCatalog() {
        List<AiPromptSpec> specs = AiPromptCatalog.all();
        Set<String> promptIds = specs.stream()
                .map(AiPromptSpec::promptId)
                .collect(Collectors.toSet());

        assertEquals(specs.size(), promptIds.size(), "PromptId 不允许重复，避免灰度与观测混乱");
    }

    @Test
    void shouldKeepPromptGovernanceRulesAcrossCatalog() {
        for (AiPromptSpec spec : AiPromptCatalog.all()) {
            assertTrue(spec.systemPrompt().contains("只输出 JSON"),
                    () -> "system prompt 缺少 JSON-only 约束: " + spec.promptId());
            assertTrue(spec.systemPrompt().contains("不要输出 Markdown"),
                    () -> "system prompt 缺少 Markdown 禁止约束: " + spec.promptId());
            assertFalse(spec.systemPrompt().contains("{{"),
                    () -> "system prompt 不应混入模板变量: " + spec.promptId());
            assertTrue(!spec.templateVariables().isEmpty(),
                    () -> "user template 应至少包含一个命名变量: " + spec.promptId());
        }
    }

    @Test
    void shouldRenderAllPromptSpecsWithFixtures() {
        for (AiPromptSpec spec : AiPromptCatalog.all()) {
            Map<String, Object> variables = AiPromptFixtures.promptVariables(spec);
            assertNotNull(variables, () -> "缺少 Prompt 渲染样例: " + spec.promptId());

            String prompt = spec.renderUser(variables);
            assertFalse(prompt.contains("null"), () -> "Prompt 渲染不应出现 null: " + spec.promptId());
            assertFalse(prompt.contains("{{"), () -> "Prompt 渲染后仍有未替换占位符: " + spec.promptId());
        }
    }

    @Test
    void shouldExposeTemplateVariablesForCatalogPrompts() {
        assertEquals(Set.of("title", "content"), CommunityPromptSpecs.POST_SUMMARY.templateVariables());
        assertEquals(
                Set.of("direction", "level", "style", "followUpCount", "question", "answer", "ragSection"),
                InterviewPromptSpecs.EVALUATE_ANSWER.templateVariables()
        );
        assertEquals(
                Set.of("gapsJson", "targetDays", "weeklyHours", "preferredLearningMode", "nextInterviewDate", "ragSection"),
                JobBattlePromptSpecs.PLAN_GENERATE.templateVariables()
        );
        assertEquals(
                Set.of("beforeSql", "beforeExplain", "afterSql", "afterExplain", "explainFormat"),
                SqlOptimizePromptSpecs.COMPARE.templateVariables()
        );
    }

    @Test
    void shouldAllowMissingOptionalPromptVariablesToFallbackAsBlank() {
        String prompt = InterviewPromptSpecs.GENERATE_QUESTIONS.renderUser(Map.of(
                "direction", "Java后端",
                "level", "高级",
                "count", 3
        ));

        assertFalse(prompt.contains("null"));
        assertFalse(prompt.contains("{{ragSection}}"));
        assertTrue(prompt.contains("Java后端"));
        assertTrue(prompt.contains("3 道"));
    }
}
