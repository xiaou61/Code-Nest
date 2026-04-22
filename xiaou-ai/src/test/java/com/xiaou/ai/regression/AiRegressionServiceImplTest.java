package com.xiaou.ai.regression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRegressionServiceImplTest {

    private final AiRegressionService service = new AiRegressionServiceImpl();

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
    }
}
