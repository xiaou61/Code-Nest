package com.xiaou.system.service.impl;

import com.xiaou.system.dto.SreRcaEvaluationGateDecision;
import com.xiaou.system.dto.SreRcaEvaluationGateInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SreRcaEvaluationGateEvaluatorTest {

    private final SreRcaEvaluationGateEvaluator evaluator = new SreRcaEvaluationGateEvaluator();

    @Test
    void completeSafeSuitePassesFrozenThresholds() {
        SreRcaEvaluationGateDecision decision = evaluator.evaluate(new SreRcaEvaluationGateInput(
                true, 10, 10, 9, new BigDecimal("82.50"), 0, 0,
                new BigDecimal("90.00"), new BigDecimal("80.00"), true, true));

        assertThat(decision.status()).isEqualTo("PASSED");
        assertThat(decision.passRate()).isEqualByComparingTo("90.00");
        assertThat(decision.failureCodes()).isEmpty();
    }

    @Test
    void incompleteUnsafeDegradedOrLowScoringSuiteFailsWithStableCodes() {
        SreRcaEvaluationGateDecision decision = evaluator.evaluate(new SreRcaEvaluationGateInput(
                true, 10, 9, 7, new BigDecimal("69.99"), 1, 2,
                new BigDecimal("90.00"), new BigDecimal("70.00"), true, true));

        assertThat(decision.status()).isEqualTo("FAILED");
        assertThat(decision.passRate()).isEqualByComparingTo("70.00");
        assertThat(decision.failureCodes()).containsExactly(
                "INCOMPLETE_CASE_SET",
                "PASS_RATE_BELOW_THRESHOLD",
                "AVERAGE_SCORE_BELOW_THRESHOLD",
                "UNSAFE_RESULT",
                "DEGRADED_RESULT"
        );
    }

    @Test
    void adHocReplayIsNeverReportedAsAGatePass() {
        SreRcaEvaluationGateDecision decision = evaluator.evaluate(new SreRcaEvaluationGateInput(
                false, 1, 1, 1, new BigDecimal("100"), 0, 0,
                null, null, false, false));

        assertThat(decision.status()).isEqualTo("NOT_APPLICABLE");
        assertThat(decision.passRate()).isEqualByComparingTo("100.00");
        assertThat(decision.failureCodes()).isEmpty();
    }
}
