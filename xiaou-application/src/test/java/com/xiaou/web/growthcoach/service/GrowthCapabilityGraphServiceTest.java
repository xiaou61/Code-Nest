package com.xiaou.web.growthcoach.service;

import com.xiaou.web.growthcoach.dto.GrowthCapabilityGraphResponse;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceSummaryResponse;
import com.xiaou.web.growthcoach.dto.GrowthSkillInsightResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthCapabilityGraphServiceTest {

    @Mock
    private GrowthEvidenceQueryService evidenceQueryService;
    @Mock
    private GrowthSkillInsightService skillInsightService;

    @Test
    void buildsAnExplainableGraphFromVerifiedEvidenceAndExistingGaps() {
        LocalDateTime observedAt = LocalDateTime.now().minusDays(2);
        when(evidenceQueryService.listForUser(7L, 30)).thenReturn(List.of(
                evidence("oj-1", "OJ_SUBMISSION_RESULT", "VERIFIED", observedAt,
                        Map.of("status", "accepted")),
                evidence("interview-1", "INTERVIEW_SCORE", "VERIFIED", observedAt,
                        Map.of("totalScore", 82)),
                evidence("task-1", "TASK_COMPLETED", "VERIFIED", observedAt,
                        Map.of("moduleKey", "java")),
                evidence("artifact-1", "PUBLIC_CODE_ARTIFACT", "VERIFIED", observedAt,
                        Map.of("ownershipVerified", true))
        ));
        GrowthSkillInsightResponse insight = new GrowthSkillInsightResponse();
        insight.setSkillKey("question_set:11");
        insight.setTitle("题单需要巩固");
        insight.setLevel("needs_practice");
        insight.setConfidence(100);
        insight.setEvidenceCount(2);
        insight.setExplanation("有两道题尚未掌握");
        GrowthSkillInsightResponse.PracticeRecommendation recommendation =
                new GrowthSkillInsightResponse.PracticeRecommendation();
        recommendation.setRoutePath("/interview/questions/11/21");
        insight.setRecommendation(recommendation);
        when(skillInsightService.listForUser(eq(7L), anyList())).thenReturn(List.of(insight));

        GrowthCapabilityGraphResponse response = new GrowthCapabilityGraphService(
                evidenceQueryService,
                skillInsightService
        ).getForUser(7L);

        assertThat(response.getSchemaVersion()).isEqualTo("1");
        assertThat(response.getEvidenceCount()).isEqualTo(4);
        assertThat(response.getVerifiedEvidenceCount()).isEqualTo(4);
        assertThat(response.getOverallScore()).isGreaterThan(0);
        assertThat(response.getNodes()).extracting(GrowthCapabilityGraphResponse.Node::getKey)
                .containsExactly("problem-solving", "interview", "delivery", "execution", "career-readiness");
        assertThat(response.getNodes().get(1).getScore()).isEqualTo(82);
        assertThat(response.getNodes().get(1).getEvidenceRefs()).hasSize(1);
        assertThat(response.getEdges()).hasSize(5);
        verify(skillInsightService).listForUser(eq(7L), anyList());
        assertThat(response.getGaps()).singleElement().satisfies(gap -> {
            assertThat(gap.getSkillKey()).isEqualTo("question_set:11");
            assertThat(gap.getRoutePath()).isEqualTo("/interview/questions/11/21");
        });
    }

    @Test
    void returnsAnEmptyReadOnlyGraphForAnInvalidUser() {
        GrowthCapabilityGraphResponse response = new GrowthCapabilityGraphService(
                evidenceQueryService,
                skillInsightService
        ).getForUser(null);

        assertThat(response.getEvidenceCount()).isZero();
        assertThat(response.getVerifiedEvidenceCount()).isZero();
        assertThat(response.getOverallScore()).isZero();
        assertThat(response.getGaps()).isEmpty();
        verifyNoInteractions(evidenceQueryService, skillInsightService);
    }

    private GrowthEvidenceSummaryResponse evidence(
            String evidenceId,
            String evidenceType,
            String qualityLevel,
            LocalDateTime observedAt,
            Map<String, Object> summary
    ) {
        GrowthEvidenceSummaryResponse response = new GrowthEvidenceSummaryResponse();
        response.setEvidenceId(evidenceId);
        response.setEvidenceType(evidenceType);
        response.setQualityLevel(qualityLevel);
        response.setObservedAt(observedAt);
        response.setSummary(new LinkedHashMap<>(summary));
        return response;
    }
}
