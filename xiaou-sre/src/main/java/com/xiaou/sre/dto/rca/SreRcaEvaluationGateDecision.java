package com.xiaou.sre.dto.rca;

import java.math.BigDecimal;
import java.util.List;

/**
 * Stable quality-gate status and machine-readable failure codes.
 *
 * @author xiaou
 */
public record SreRcaEvaluationGateDecision(
        String status,
        BigDecimal passRate,
        List<String> failureCodes
) {

    public SreRcaEvaluationGateDecision {
        failureCodes = failureCodes == null ? List.of() : List.copyOf(failureCodes);
    }
}
