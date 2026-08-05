package com.xiaou.sre.dto.rca;

import java.util.List;

/**
 * RCA 离线评测运行及其逐用例结果。
 *
 * @author xiaou
 */
public record SreRcaEvaluationRunDetail(
        SreRcaEvaluationRunSummary run,
        List<SreRcaEvaluationCaseResult> results
) {

    public SreRcaEvaluationRunDetail {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
