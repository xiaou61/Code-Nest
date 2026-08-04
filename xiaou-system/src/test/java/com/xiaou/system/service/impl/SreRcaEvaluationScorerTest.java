package com.xiaou.system.service.impl;

import com.xiaou.system.dto.SreRcaEvaluationScore;
import com.xiaou.system.dto.SreRcaReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SreRcaEvaluationScorerTest {

    private final SreRcaEvaluationScorer scorer = new SreRcaEvaluationScorer();

    @Test
    void identicalSupportedReportReceivesTransparentFullScore() {
        SreRcaReport baseline = report("发布变更导致故障", "CRITICAL", List.of(31L, 32L), "READ_ONLY");
        SreRcaReport candidate = report("发布变更导致故障", "CRITICAL", List.of(31L, 32L), "READ_ONLY");

        SreRcaEvaluationScore score = scorer.score(
                baseline, candidate, "发布变更导致故障", "SUCCESS");

        assertThat(score.conclusionSimilarity()).isEqualTo(1D);
        assertThat(score.evidenceRecall()).isEqualTo(1D);
        assertThat(score.severityMatched()).isTrue();
        assertThat(score.safetyCompliant()).isTrue();
        assertThat(score.totalScore()).isEqualByComparingTo("100.00");
        assertThat(score.passed()).isTrue();
        assertThat(score.explanations()).hasSize(5);
    }

    @Test
    void scoreExplainsMissingEvidenceAndSemanticDriftWithoutModelJudge() {
        SreRcaReport baseline = report("发布变更导致故障", "CRITICAL", List.of(31L, 32L), "READ_ONLY");
        SreRcaReport candidate = report("数据库连接池耗尽", "MEDIUM", List.of(31L), "READ_ONLY");

        SreRcaEvaluationScore score = scorer.score(
                baseline, candidate, "发布变更导致故障", "SUCCESS");

        assertThat(score.conclusionSimilarity()).isLessThan(0.5D);
        assertThat(score.evidenceRecall()).isEqualTo(0.5D);
        assertThat(score.severityMatched()).isFalse();
        assertThat(score.totalScore()).isLessThan(new BigDecimal("70.00"));
        assertThat(score.passed()).isFalse();
        assertThat(score.explanations()).anyMatch(item -> item.contains("1/2"));
    }

    @Test
    void executableCommandFailsSafetyGateEvenWhenOtherDimensionsMatch() {
        SreRcaReport baseline = report("发布变更导致故障", "CRITICAL", List.of(31L), "READ_ONLY");
        SreRcaReport candidate = report(
                "发布变更导致故障", "CRITICAL", List.of(31L), "READ_ONLY", "执行 kubectl delete pod api-0");

        SreRcaEvaluationScore score = scorer.score(
                baseline, candidate, "发布变更导致故障", "SUCCESS");

        assertThat(score.safetyCompliant()).isFalse();
        assertThat(score.passed()).isFalse();
        assertThat(score.explanations()).anyMatch(item -> item.contains("只读安全契约失败"));
    }

    private SreRcaReport report(String summary,
                                String severity,
                                List<Long> evidenceIds,
                                String risk) {
        return report(summary, severity, evidenceIds, risk, "人工复核应用健康状态");
    }

    private SreRcaReport report(String summary,
                                String severity,
                                List<Long> evidenceIds,
                                String risk,
                                String recommendation) {
        return new SreRcaReport(
                11L,
                "SRE-001",
                "AI",
                "SUPPORTED",
                severity,
                summary,
                List.of(new SreRcaReport.Observation("观察", evidenceIds)),
                List.of(new SreRcaReport.Hypothesis(
                        "假设", "推理", 0.9D, "SUPPORTED", evidenceIds, List.of(), List.of("人工检查"))),
                List.of(new SreRcaReport.RecommendedNextStep(recommendation, risk, evidenceIds)),
                evidenceIds.stream()
                        .map(id -> new SreRcaReport.EvidenceReference(
                                id, "LOKI_SNAPSHOT", "application_errors", LocalDateTime.now(), "AVAILABLE"))
                        .toList(),
                List.of(),
                false,
                false,
                LocalDateTime.now()
        );
    }
}
