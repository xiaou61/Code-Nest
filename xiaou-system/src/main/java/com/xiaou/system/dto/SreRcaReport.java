package com.xiaou.system.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SRE 只读根因分析报告。
 *
 * @author xiaou
 */
public record SreRcaReport(
        Long incidentId,
        String incidentNo,
        String generationMode,
        String conclusionStatus,
        String severityAssessment,
        String executiveSummary,
        List<Observation> observations,
        List<Hypothesis> hypotheses,
        List<RecommendedNextStep> recommendedNextSteps,
        List<EvidenceReference> evidenceReferences,
        List<String> limitations,
        boolean contextTruncated,
        boolean executionAllowed,
        LocalDateTime generatedAt
) {

    public SreRcaReport {
        observations = copy(observations);
        hypotheses = copy(hypotheses);
        recommendedNextSteps = copy(recommendedNextSteps);
        evidenceReferences = copy(evidenceReferences);
        limitations = copy(limitations);
        executionAllowed = false;
    }

    public record Observation(String statement, List<Long> evidenceIds) {
        public Observation {
            evidenceIds = copy(evidenceIds);
        }
    }

    public record Hypothesis(
            String title,
            String reasoning,
            double confidence,
            String evidenceStatus,
            List<Long> evidenceIds,
            List<Long> counterEvidenceIds,
            List<String> nextChecks
    ) {
        public Hypothesis {
            evidenceIds = copy(evidenceIds);
            counterEvidenceIds = copy(counterEvidenceIds);
            nextChecks = copy(nextChecks);
        }
    }

    public record RecommendedNextStep(String description, String risk, List<Long> evidenceIds) {
        public RecommendedNextStep {
            evidenceIds = copy(evidenceIds);
        }
    }

    public record EvidenceReference(
            Long id,
            String sourceType,
            String sourceRef,
            LocalDateTime capturedAt,
            String status
    ) {
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
