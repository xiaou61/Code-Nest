package com.xiaou.sre.domain;

import java.util.List;

/**
 * Verified suite version with full cases for the internal replay boundary.
 *
 * @author xiaou
 */
public record SreRcaEvaluationSuiteVersionSnapshot(
        SreRcaEvaluationSuite suite,
        SreRcaEvaluationSuiteVersion version,
        List<SreRcaEvaluationCase> cases
) {

    public SreRcaEvaluationSuiteVersionSnapshot {
        cases = cases == null ? List.of() : List.copyOf(cases);
    }
}
