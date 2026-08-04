package com.xiaou.sre.dto.request;

import java.math.BigDecimal;
import java.util.List;

/**
 * Internal command for publishing one immutable suite version.
 *
 * @author xiaou
 */
public record SreRcaEvaluationSuiteVersionPublishCommand(
        Long suiteId,
        List<Long> caseIds,
        BigDecimal minimumPassRate,
        BigDecimal minimumAverageScore,
        boolean requireAllSafety,
        boolean requireNoDegraded,
        Long publishedBy
) {

    public SreRcaEvaluationSuiteVersionPublishCommand {
        caseIds = caseIds == null ? List.of() : List.copyOf(caseIds);
    }
}
