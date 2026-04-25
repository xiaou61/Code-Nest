package com.xiaou.ai.regression;

import com.xiaou.common.config.AiProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRegressionServiceImplTest {

    private final AiRegressionService service = new AiRegressionServiceImpl(new AiProperties());

    @Test
    void shouldListRegressionCases() {
        var cases = service.listCases();

        assertFalse(cases.isEmpty());
        assertTrue(cases.stream().anyMatch(item -> "community_summary".equals(item.getScenario())));
        assertTrue(cases.stream().anyMatch(item -> "sql_optimize_analyze".equals(item.getScenario())));
    }

    @Test
    void shouldRunScenarioRegressionCases() {
        AiRegressionRunSummary summary = service.run("community_summary", null);

        assertEquals(2, summary.getTotalCount());
        assertEquals(2, summary.getPassedCount());
        assertEquals(0, summary.getFailedCount());
        assertEquals(2, summary.getCaseResults().size());
        assertNotNull(summary.getExecutedAt());
        assertTrue(summary.getCaseResults().stream().allMatch(AiRegressionCaseResult::isPassed));
        assertTrue(summary.getCaseResults().stream().allMatch(item -> "gpt-4o-mini".equals(item.getModelName())));
        assertTrue(summary.getCaseResults().stream().allMatch(item -> "community_summary".equals(item.getGraphName())));
        assertTrue(summary.getCaseResults().stream().allMatch(item ->
                item.getPromptIds().contains("community.post_summary:v1")
        ));
    }

    @Test
    void shouldExposeCompositePromptMetadataForTargetAnalyzeScenario() {
        AiRegressionRunSummary summary = service.run("job_battle_target_analyze", "job-battle-target-analyze-success");

        assertEquals(1, summary.getTotalCount());
        assertEquals(1, summary.getCaseResults().size());
        AiRegressionCaseResult result = summary.getCaseResults().get(0);
        assertEquals("job_battle_analyze_target_graph", result.getGraphName());
        assertTrue(result.getPromptIds().contains("job_battle.jd_parse:v1"));
        assertTrue(result.getPromptIds().contains("job_battle.resume_match:v1"));
    }
}
