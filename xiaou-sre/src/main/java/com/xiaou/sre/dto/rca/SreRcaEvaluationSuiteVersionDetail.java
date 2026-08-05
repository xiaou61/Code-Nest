package com.xiaou.sre.dto.rca;

import java.util.List;

/**
 * Verified suite version with case summaries only.
 *
 * @author xiaou
 */
public record SreRcaEvaluationSuiteVersionDetail(
        SreRcaEvaluationSuiteSummary suite,
        SreRcaEvaluationSuiteVersionSummary version,
        List<SreRcaEvaluationCaseSummary> cases
) {

    public SreRcaEvaluationSuiteVersionDetail {
        cases = cases == null ? List.of() : List.copyOf(cases);
    }
}
