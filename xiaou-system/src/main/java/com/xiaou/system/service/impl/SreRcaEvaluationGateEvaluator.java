package com.xiaou.system.service.impl;

import com.xiaou.system.dto.SreRcaEvaluationGateDecision;
import com.xiaou.system.dto.SreRcaEvaluationGateInput;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic aggregate gate. It never invokes a model judge.
 *
 * @author xiaou
 */
@Component
public class SreRcaEvaluationGateEvaluator {

    public SreRcaEvaluationGateDecision evaluate(SreRcaEvaluationGateInput input) {
        if (input == null || input.expectedCaseCount() <= 0) {
            throw new IllegalArgumentException("质量门禁输入不合法");
        }
        if (input.completedCount() < 0
                || input.passedCount() < 0
                || input.passedCount() > input.completedCount()) {
            throw new IllegalArgumentException("质量门禁计数不合法");
        }
        BigDecimal passRate = BigDecimal.valueOf(input.passedCount())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(input.expectedCaseCount()), 2, RoundingMode.HALF_UP);
        if (!input.applicable()) {
            return new SreRcaEvaluationGateDecision("NOT_APPLICABLE", passRate, List.of());
        }

        BigDecimal minimumPassRate = percentage(input.minimumPassRate(), "最低通过率不合法");
        BigDecimal minimumAverageScore = percentage(input.minimumAverageScore(), "最低平均分不合法");
        BigDecimal averageScore = percentage(input.averageScore(), "平均分不合法");
        List<String> failures = new ArrayList<>();
        if (input.completedCount() != input.expectedCaseCount()) {
            failures.add("INCOMPLETE_CASE_SET");
        }
        if (passRate.compareTo(minimumPassRate) < 0) {
            failures.add("PASS_RATE_BELOW_THRESHOLD");
        }
        if (averageScore.compareTo(minimumAverageScore) < 0) {
            failures.add("AVERAGE_SCORE_BELOW_THRESHOLD");
        }
        if (input.requireAllSafety() && input.unsafeCount() > 0) {
            failures.add("UNSAFE_RESULT");
        }
        if (input.requireNoDegraded() && input.degradedCount() > 0) {
            failures.add("DEGRADED_RESULT");
        }
        return new SreRcaEvaluationGateDecision(
                failures.isEmpty() ? "PASSED" : "FAILED",
                passRate,
                failures
        );
    }

    private BigDecimal percentage(BigDecimal value, String message) {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
