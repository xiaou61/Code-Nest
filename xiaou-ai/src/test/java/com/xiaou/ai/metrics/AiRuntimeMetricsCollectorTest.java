package com.xiaou.ai.metrics;

import com.xiaou.ai.prompt.AiPromptSpec;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRuntimeMetricsCollectorTest {

    @Test
    void shouldAggregateOverviewSceneAndRecentCalls() {
        AiRuntimeMetricsCollector collector = new AiRuntimeMetricsCollector();
        AiPromptSpec promptSpec = AiPromptSpec.of(
                "job_battle.resume_match",
                "v1",
                "只输出 JSON",
                "{{resumeText}}"
        );

        collector.recordInvocation(
                "job_battle_resume_match_graph",
                promptSpec,
                "success",
                120_000_000L,
                "gpt-5.4",
                120,
                60,
                180,
                new BigDecimal("0.00120000")
        );
        collector.recordFallback("job_battle_resume_match_graph", promptSpec, "empty_response");
        collector.recordStructuredParseFailure("job_battle_resume_match_graph", promptSpec, "invalid_json");

        AiRuntimeMetricsSnapshot snapshot = collector.snapshot();

        assertEquals(1, snapshot.getOverview().getTotalInvocations());
        assertEquals(1, snapshot.getOverview().getSuccessCount());
        assertEquals(1, snapshot.getOverview().getFallbackCount());
        assertEquals(1, snapshot.getOverview().getStructuredParseFailureCount());
        assertEquals(180, snapshot.getOverview().getTotalTokens());
        assertEquals(new BigDecimal("0.00120000"), snapshot.getOverview().getEstimatedCost());
        assertEquals(1, snapshot.getSceneStats().size());
        assertEquals("job_battle_resume_match_graph", snapshot.getSceneStats().get(0).getScene());
        assertEquals("gpt-5.4", snapshot.getSceneStats().get(0).getLastModelName());
        assertEquals(1, snapshot.getModelStats().size());
        assertEquals("gpt-5.4", snapshot.getModelStats().get(0).getModelName());
        assertEquals(1, snapshot.getModelStats().get(0).getInvocations());
        assertEquals(1, snapshot.getModelStats().get(0).getSuccessCount());
        assertEquals(1, snapshot.getRecentCalls().size());
        assertEquals("success", snapshot.getRecentCalls().get(0).getOutcome());
    }

    @Test
    void shouldClearAllAggregatedMetrics() {
        AiRuntimeMetricsCollector collector = new AiRuntimeMetricsCollector();
        AiPromptSpec promptSpec = AiPromptSpec.of(
                "sql.optimize.analyze",
                "v2",
                "只输出 JSON",
                "{{sql}}"
        );

        collector.recordInvocation(
                "sql_optimize_graph",
                promptSpec,
                "error",
                88_000_000L,
                "gpt-5.4",
                90,
                20,
                110,
                new BigDecimal("0.00090000")
        );
        collector.recordFallback("sql_optimize_graph", promptSpec, "model_error");
        collector.recordStructuredParseFailure("sql_optimize_graph", promptSpec, "invalid_json");

        collector.clear();

        AiRuntimeMetricsSnapshot snapshot = collector.snapshot();
        assertEquals(0, snapshot.getOverview().getTotalInvocations());
        assertEquals(0, snapshot.getOverview().getFallbackCount());
        assertEquals(0, snapshot.getOverview().getStructuredParseFailureCount());
        assertEquals(0, snapshot.getSceneStats().size());
        assertEquals(0, snapshot.getModelStats().size());
        assertEquals(0, snapshot.getRecentCalls().size());
    }

    @Test
    void shouldFilterMetricsBySceneOutcomeAndRecentLimit() {
        AiRuntimeMetricsCollector collector = new AiRuntimeMetricsCollector();
        AiPromptSpec resumePrompt = AiPromptSpec.of(
                "job_battle.resume_match",
                "v1",
                "只输出 JSON",
                "{{resumeText}}"
        );
        AiPromptSpec sqlPrompt = AiPromptSpec.of(
                "sql.optimize.analyze",
                "v2",
                "只输出 JSON",
                "{{sql}}"
        );

        collector.recordInvocation(
                "job_battle_resume_match_graph",
                resumePrompt,
                "success",
                120_000_000L,
                "gpt-5.4",
                120,
                60,
                180,
                new BigDecimal("0.00120000")
        );
        collector.recordInvocation(
                "job_battle_resume_match_graph",
                resumePrompt,
                "error",
                90_000_000L,
                "gpt-5.4",
                80,
                20,
                100,
                new BigDecimal("0.00080000")
        );
        collector.recordFallback("job_battle_resume_match_graph", resumePrompt, "invocation_exception");
        collector.recordStructuredParseFailure("job_battle_resume_match_graph", resumePrompt, "invalid_json");
        collector.recordInvocation(
                "sql_optimize_graph",
                sqlPrompt,
                "error",
                60_000_000L,
                "claude-3.7",
                50,
                10,
                60,
                new BigDecimal("0.00040000")
        );

        AiRuntimeMetricsSnapshot snapshot = collector.snapshot("resume_match", "error", 1);

        assertEquals(1, snapshot.getOverview().getTotalInvocations());
        assertEquals(0, snapshot.getOverview().getSuccessCount());
        assertEquals(1, snapshot.getOverview().getErrorCount());
        assertEquals(1, snapshot.getOverview().getFallbackCount());
        assertEquals(1, snapshot.getOverview().getStructuredParseFailureCount());
        assertEquals(100, snapshot.getOverview().getTotalTokens());
        assertEquals(new BigDecimal("0.00080000"), snapshot.getOverview().getEstimatedCost());
        assertEquals(1, snapshot.getSceneStats().size());
        assertEquals("job_battle_resume_match_graph", snapshot.getSceneStats().get(0).getScene());
        assertEquals(1, snapshot.getSceneStats().get(0).getInvocations());
        assertEquals(0, snapshot.getSceneStats().get(0).getSuccessCount());
        assertEquals(1, snapshot.getSceneStats().get(0).getErrorCount());
        assertEquals(1, snapshot.getSceneStats().get(0).getFallbackCount());
        assertEquals(1, snapshot.getSceneStats().get(0).getStructuredParseFailureCount());
        assertEquals(1, snapshot.getModelStats().size());
        assertEquals("gpt-5.4", snapshot.getModelStats().get(0).getModelName());
        assertEquals(1, snapshot.getModelStats().get(0).getInvocations());
        assertEquals(0, snapshot.getModelStats().get(0).getSuccessCount());
        assertEquals(1, snapshot.getModelStats().get(0).getErrorCount());
        assertEquals(100, snapshot.getModelStats().get(0).getTotalTokens());
        assertEquals(1, snapshot.getRecentCalls().size());
        assertEquals("error", snapshot.getRecentCalls().get(0).getOutcome());
        assertNotNull(snapshot.getRecentCalls().get(0).getPromptKey());
    }

    @Test
    void shouldFilterMetricsByModelKeyword() {
        AiRuntimeMetricsCollector collector = new AiRuntimeMetricsCollector();
        AiPromptSpec resumePrompt = AiPromptSpec.of(
                "job_battle.resume_match",
                "v1",
                "只输出 JSON",
                "{{resumeText}}"
        );

        collector.recordInvocation(
                "job_battle_resume_match_graph",
                resumePrompt,
                "error",
                90_000_000L,
                "gpt-5.4",
                80,
                20,
                100,
                new BigDecimal("0.00080000")
        );
        collector.recordFallback("job_battle_resume_match_graph", resumePrompt, "invocation_exception", "gpt-5.4");
        collector.recordStructuredParseFailure("job_battle_resume_match_graph", resumePrompt, "invalid_json", "gpt-5.4");
        collector.recordInvocation(
                "job_battle_resume_match_graph",
                resumePrompt,
                "error",
                60_000_000L,
                "claude-3.7",
                50,
                10,
                60,
                new BigDecimal("0.00040000")
        );
        collector.recordFallback("job_battle_resume_match_graph", resumePrompt, "invocation_exception", "claude-3.7");

        AiRuntimeMetricsSnapshot snapshot = collector.snapshot("resume_match", "error", "gpt-5", 10);

        assertEquals(1, snapshot.getOverview().getTotalInvocations());
        assertEquals(1, snapshot.getOverview().getErrorCount());
        assertEquals(1, snapshot.getOverview().getFallbackCount());
        assertEquals(1, snapshot.getOverview().getStructuredParseFailureCount());
        assertEquals(100, snapshot.getOverview().getTotalTokens());
        assertEquals(new BigDecimal("0.00080000"), snapshot.getOverview().getEstimatedCost());
        assertEquals(1, snapshot.getSceneStats().size());
        assertEquals("job_battle_resume_match_graph", snapshot.getSceneStats().get(0).getScene());
        assertEquals("gpt-5.4", snapshot.getSceneStats().get(0).getLastModelName());
        assertEquals(1, snapshot.getSceneStats().get(0).getInvocations());
        assertEquals(1, snapshot.getSceneStats().get(0).getErrorCount());
        assertEquals(1, snapshot.getSceneStats().get(0).getFallbackCount());
        assertEquals(1, snapshot.getSceneStats().get(0).getStructuredParseFailureCount());
        assertEquals(1, snapshot.getModelStats().size());
        assertEquals("gpt-5.4", snapshot.getModelStats().get(0).getModelName());
        assertEquals(1, snapshot.getModelStats().get(0).getInvocations());
        assertEquals(1, snapshot.getRecentCalls().size());
        assertEquals("gpt-5.4", snapshot.getRecentCalls().get(0).getModelName());
    }

    @Test
    void shouldPersistAndRestoreMetricsState() {
        AiRuntimeMetricsCollector collector = new AiRuntimeMetricsCollector();
        InMemoryMetricsPersistence persistence = new InMemoryMetricsPersistence();
        collector.setPersistence(persistence);
        AiPromptSpec promptSpec = AiPromptSpec.of(
                "community.summary",
                "v1",
                "只输出 JSON",
                "{{content}}"
        );

        collector.recordInvocation(
                "community_summary",
                promptSpec,
                "success",
                50_000_000L,
                "gpt-5.4",
                30,
                20,
                50,
                new BigDecimal("0.00050000")
        );
        collector.recordFallback("community_summary", promptSpec, "empty_response", "gpt-5.4");

        assertTrue(collector.isPersistenceEnabled());
        assertEquals("memory-test", collector.persistenceMode());
        assertNotNull(persistence.state);
        assertFalse(persistence.cleared);

        AiRuntimeMetricsCollector restoredCollector = new AiRuntimeMetricsCollector();
        restoredCollector.setPersistence(persistence);

        AiRuntimeMetricsSnapshot snapshot = restoredCollector.snapshot();
        assertEquals(1, snapshot.getOverview().getTotalInvocations());
        assertEquals(1, snapshot.getOverview().getFallbackCount());
        assertEquals(1, snapshot.getSceneStats().size());
        assertEquals("community_summary", snapshot.getSceneStats().get(0).getScene());
        assertEquals(1, snapshot.getRecentCalls().size());
        assertEquals("gpt-5.4", snapshot.getRecentCalls().get(0).getModelName());
    }

    @Test
    void shouldClearPersistedMetricsState() {
        AiRuntimeMetricsCollector collector = new AiRuntimeMetricsCollector();
        InMemoryMetricsPersistence persistence = new InMemoryMetricsPersistence();
        collector.setPersistence(persistence);
        AiPromptSpec promptSpec = AiPromptSpec.of(
                "sql.optimize.analyze",
                "v2",
                "只输出 JSON",
                "{{sql}}"
        );

        collector.recordInvocation(
                "sql_optimize_graph",
                promptSpec,
                "error",
                40_000_000L,
                "gpt-5.4",
                10,
                5,
                15,
                new BigDecimal("0.00010000")
        );

        collector.clear();

        assertTrue(persistence.cleared);
        assertEquals(0, collector.snapshot().getOverview().getTotalInvocations());
    }

    private static final class InMemoryMetricsPersistence implements AiRuntimeMetricsPersistence {
        private AiRuntimeMetricsStoreState state;
        private boolean cleared;

        @Override
        public String mode() {
            return "memory-test";
        }

        @Override
        public AiRuntimeMetricsStoreState load() {
            return state;
        }

        @Override
        public void save(AiRuntimeMetricsStoreState state) {
            this.state = state;
            this.cleared = false;
        }

        @Override
        public void clear() {
            this.state = null;
            this.cleared = true;
        }
    }
}
