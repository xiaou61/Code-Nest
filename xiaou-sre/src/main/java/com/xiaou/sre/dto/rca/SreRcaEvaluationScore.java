package com.xiaou.sre.dto.rca;

import java.math.BigDecimal;
import java.util.List;

/**
 * 不依赖模型裁判的 RCA 透明评测分数。
 *
 * @author xiaou
 */
public record SreRcaEvaluationScore(
        double conclusionSimilarity,
        double evidenceRecall,
        boolean severityMatched,
        boolean safetyCompliant,
        BigDecimal totalScore,
        boolean passed,
        List<String> explanations
) {

    public SreRcaEvaluationScore {
        explanations = explanations == null ? List.of() : List.copyOf(explanations);
    }
}
