package com.xiaou.ai.prompt;

import com.xiaou.ai.prompt.interview.InterviewRagQuerySpecs;
import com.xiaou.ai.prompt.jobbattle.JobBattleRagQuerySpecs;
import com.xiaou.ai.prompt.sql.SqlOptimizeRagQuerySpecs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRagQuerySpecTest {

    @Test
    void shouldRegisterUniqueQueryIdsAcrossCatalog() {
        List<AiRagQuerySpec> specs = AiRagQueryCatalog.all();
        Set<String> queryIds = specs.stream()
                .map(AiRagQuerySpec::queryId)
                .collect(Collectors.toSet());

        assertEquals(specs.size(), queryIds.size(), "RAG queryId 不允许重复，避免知识库检索灰度混乱");
    }

    @Test
    void shouldKeepRagQueryGovernanceRulesAcrossCatalog() {
        for (AiRagQuerySpec spec : AiRagQueryCatalog.all()) {
            assertTrue(spec.key().contains(".retrieve."), () -> "RAG query key 应带 retrieve 语义: " + spec.queryId());
            assertTrue(!spec.templateVariables().isEmpty(), () -> "RAG query 必须包含命名变量: " + spec.queryId());
        }
    }

    @Test
    void shouldRenderAllRagQueriesWithFixtures() {
        for (AiRagQuerySpec spec : AiRagQueryCatalog.all()) {
            Map<String, Object> variables = AiPromptFixtures.ragQueryVariables(spec);
            assertNotNull(variables, () -> "缺少 RAG query 渲染样例: " + spec.queryId());

            String query = spec.render(variables);
            assertFalse(query.contains("null"), () -> "RAG query 渲染不应出现 null: " + spec.queryId());
            assertFalse(query.contains("{{"), () -> "RAG query 渲染后仍有未替换占位符: " + spec.queryId());
        }
    }

    @Test
    void shouldExposeTemplateVariablesForRagQueries() {
        assertEquals(Set.of("direction", "level", "count"), InterviewRagQuerySpecs.GENERATE_QUESTIONS.templateVariables());
        assertEquals(
                Set.of("targetRole", "targetLevel", "city", "jdText", "resumeText", "projectHighlights", "targetCompanyType"),
                JobBattleRagQuerySpecs.ANALYZE_TARGET.templateVariables()
        );
        assertEquals(Set.of("originalSql", "diagnoseJson"), SqlOptimizeRagQuerySpecs.REWRITE.templateVariables());
    }

    @Test
    void shouldAllowMissingRagQueryVariablesToFallbackAsBlank() {
        String query = JobBattleRagQuerySpecs.REVIEW_INTERVIEW.render(Map.of(
                "targetRole", "Java开发工程师",
                "interviewResult", "待定"
        ));

        assertFalse(query.contains("null"));
        assertFalse(query.contains("{{interviewNotes}}"));
        assertTrue(query.contains("Java开发工程师"));
        assertTrue(query.contains("待定"));
    }
}
