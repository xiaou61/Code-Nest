package com.xiaou.sre.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.xiaou.sre.domain.SreInvestigationArtifact;
import com.xiaou.sre.domain.SreInvestigationFeedback;
import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.mapper.SreInvestigationArtifactMapper;
import com.xiaou.sre.mapper.SreInvestigationFeedbackMapper;
import com.xiaou.sre.mapper.SreInvestigationRunMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationCaseMapper;
import com.xiaou.sre.service.impl.SreRcaEvaluationCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreRcaEvaluationCaseServiceImplTest {

    @Mock
    private SreInvestigationRunMapper runMapper;

    @Mock
    private SreInvestigationArtifactMapper artifactMapper;

    @Mock
    private SreInvestigationFeedbackMapper feedbackMapper;

    @Mock
    private SreRcaEvaluationCaseMapper caseMapper;

    @Test
    void promotionFreezesTheExactRunArtifactAndFeedbackRevision() {
        SreRcaEvaluationCaseServiceImpl service = service();
        when(runMapper.selectByIncidentIdAndId(11L, 91L)).thenReturn(run());
        when(artifactMapper.selectByIncidentIdAndRunId(11L, 91L)).thenReturn(artifact());
        when(feedbackMapper.selectByIdAndRunId(101L, 91L)).thenReturn(feedback());
        when(caseMapper.selectBySourceFeedbackId(101L)).thenReturn(null);
        when(caseMapper.insert(org.mockito.ArgumentMatchers.any(SreRcaEvaluationCase.class)))
                .thenAnswer(invocation -> {
                    SreRcaEvaluationCase value = invocation.getArgument(0);
                    value.setId(301L);
                    return 1;
                });

        SreRcaEvaluationCase promoted = service.promote(11L, 91L, 101L, 7L).orElseThrow();

        assertThat(promoted.getId()).isEqualTo(301L);
        assertThat(promoted.getSourceRunId()).isEqualTo(91L);
        assertThat(promoted.getSourceArtifactId()).isEqualTo(201L);
        assertThat(promoted.getSourceFeedbackId()).isEqualTo(101L);
        assertThat(promoted.getContextJson()).isEqualTo("{\"incident\":11}");
        assertThat(promoted.getBaselineReportJson()).isEqualTo("{\"executiveSummary\":\"旧结论\"}");
        assertThat(promoted.getExpectedConclusion()).isEqualTo("发布变更导致故障");
        assertThat(promoted.getFeedbackAccuracy()).isEqualTo("PARTIAL");
        assertThat(promoted.getFeedbackGapType()).isEqualTo("RETRIEVAL_GAP");
        assertThat(promoted.getSourcePromptId()).isEqualTo("sre.incident.rca:v1");
        assertThat(promoted.getPromotedBy()).isEqualTo(7L);

        ArgumentCaptor<SreRcaEvaluationCase> capture = ArgumentCaptor.forClass(SreRcaEvaluationCase.class);
        verify(caseMapper).insert(capture.capture());
        assertThat(capture.getValue().getContextSha256()).hasSize(64);
        assertThat(capture.getValue().getPromotedAt()).isNotNull();
    }

    @Test
    void repeatedPromotionOfTheSameFeedbackRevisionIsIdempotent() {
        SreRcaEvaluationCaseServiceImpl service = service();
        SreRcaEvaluationCase existing = new SreRcaEvaluationCase();
        existing.setId(301L);
        existing.setIncidentId(11L);
        existing.setSourceRunId(91L);
        existing.setSourceFeedbackId(101L);
        when(runMapper.selectByIncidentIdAndId(11L, 91L)).thenReturn(run());
        when(artifactMapper.selectByIncidentIdAndRunId(11L, 91L)).thenReturn(artifact());
        when(feedbackMapper.selectByIdAndRunId(101L, 91L)).thenReturn(feedback());
        when(caseMapper.selectBySourceFeedbackId(101L)).thenReturn(existing);

        assertThat(service.promote(11L, 91L, 101L, 7L)).containsSame(existing);

        verify(caseMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void crossIncidentOrCrossRunFeedbackCannotBePromoted() {
        SreRcaEvaluationCaseServiceImpl service = service();
        when(runMapper.selectByIncidentIdAndId(12L, 91L)).thenReturn(null);

        assertThat(service.promote(12L, 91L, 101L, 7L)).isEmpty();

        verify(artifactMapper, never()).selectByIncidentIdAndRunId(12L, 91L);
        verify(caseMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void promotionRejectsArtifactWithRehashedUnredactedCredentials() {
        SreRcaEvaluationCaseServiceImpl service = service();
        SreInvestigationArtifact tamperedArtifact = artifact();
        String contextJson = "{\"authorization\":\"Bearer production-secret-token\"}";
        tamperedArtifact.setContextJson(contextJson);
        tamperedArtifact.setContextLength(contextJson.length());
        tamperedArtifact.setContextSha256(DigestUtil.sha256Hex(contextJson));
        when(runMapper.selectByIncidentIdAndId(11L, 91L)).thenReturn(run());
        when(artifactMapper.selectByIncidentIdAndRunId(11L, 91L)).thenReturn(tamperedArtifact);

        assertThatThrownBy(() -> service.promote(11L, 91L, 101L, 7L))
                .isInstanceOf(SreValidationException.class)
                .hasMessage("RCA 回放 artifact 包含未脱敏凭据");

        verify(feedbackMapper, never()).selectByIdAndRunId(101L, 91L);
        verify(caseMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    private SreRcaEvaluationCaseServiceImpl service() {
        return new SreRcaEvaluationCaseServiceImpl(runMapper, artifactMapper, feedbackMapper, caseMapper);
    }

    private SreInvestigationRun run() {
        SreInvestigationRun run = new SreInvestigationRun();
        run.setId(91L);
        run.setIncidentId(11L);
        run.setStatus("SUCCEEDED");
        run.setReportJson("{\"executiveSummary\":\"旧结论\"}");
        return run;
    }

    private SreInvestigationArtifact artifact() {
        String contextJson = "{\"incident\":11}";
        SreInvestigationArtifact artifact = new SreInvestigationArtifact();
        artifact.setId(201L);
        artifact.setIncidentId(11L);
        artifact.setRunId(91L);
        artifact.setContextJson(contextJson);
        artifact.setContextSha256(DigestUtil.sha256Hex(contextJson));
        artifact.setContextLength(contextJson.length());
        artifact.setContextTruncated(false);
        artifact.setPromptId("sre.incident.rca:v1");
        artifact.setSchemaId("xiaou://ai/structured-output/sre.incident.rca:v1");
        artifact.setProvider("openai-compatible");
        artifact.setConfiguredModel("configured-model");
        artifact.setActualModel("runtime-model");
        artifact.setInvocationOutcome("SUCCESS");
        artifact.setCreatedAt(LocalDateTime.of(2026, 7, 27, 11, 30));
        return artifact;
    }

    private SreInvestigationFeedback feedback() {
        SreInvestigationFeedback feedback = new SreInvestigationFeedback();
        feedback.setId(101L);
        feedback.setRunId(91L);
        feedback.setAccuracy("PARTIAL");
        feedback.setGapType("RETRIEVAL_GAP");
        feedback.setExpectedConclusion("发布变更导致故障");
        feedback.setReviewedBy(7L);
        feedback.setReviewedAt(LocalDateTime.of(2026, 7, 27, 12, 0));
        return feedback;
    }
}
