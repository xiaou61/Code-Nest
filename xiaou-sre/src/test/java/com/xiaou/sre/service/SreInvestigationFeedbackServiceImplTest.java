package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreInvestigationFeedback;
import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.mapper.SreInvestigationFeedbackMapper;
import com.xiaou.sre.mapper.SreInvestigationRunMapper;
import com.xiaou.sre.service.impl.SreInvestigationFeedbackServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreInvestigationFeedbackServiceImplTest {

    @Mock
    private SreInvestigationRunMapper runMapper;

    @Mock
    private SreInvestigationFeedbackMapper feedbackMapper;

    @Test
    void partialFeedbackIsNormalizedSanitizedAndAppended() {
        SreInvestigationFeedbackServiceImpl service = service();
        String standaloneCredential = "sk-" + "x".repeat(24);
        when(runMapper.selectByIncidentIdAndId(11L, 91L)).thenReturn(run());
        doAnswer(invocation -> {
            invocation.<SreInvestigationFeedback>getArgument(0).setId(101L);
            return 1;
        }).when(feedbackMapper).insert(any(SreInvestigationFeedback.class));

        Optional<SreInvestigationFeedback> saved = service.save(
                11L,
                91L,
                " partial ",
                " retrieval_gap ",
                "需要补证据\u0000 password=do-not-store",
                " 应确认发布变更\u0007 token=do-not-store " + standaloneCredential,
                7L
        );

        assertThat(saved).isPresent();
        ArgumentCaptor<SreInvestigationFeedback> captor =
                ArgumentCaptor.forClass(SreInvestigationFeedback.class);
        verify(feedbackMapper).insert(captor.capture());
        SreInvestigationFeedback feedback = captor.getValue();
        assertThat(feedback.getId()).isEqualTo(101L);
        assertThat(feedback.getRunId()).isEqualTo(91L);
        assertThat(feedback.getAccuracy()).isEqualTo("PARTIAL");
        assertThat(feedback.getGapType()).isEqualTo("RETRIEVAL_GAP");
        assertThat(feedback.getNote()).isEqualTo("需要补证据 password=[REDACTED]");
        assertThat(feedback.getExpectedConclusion())
                .isEqualTo("应确认发布变更 token=[REDACTED] [REDACTED]")
                .doesNotContain(standaloneCredential);
        assertThat(feedback.getReviewedBy()).isEqualTo(7L);
        assertThat(feedback.getReviewedAt()).isNotNull();
    }

    @Test
    void feedbackRequiresWhitelistedCrossFieldCombination() {
        SreInvestigationFeedbackServiceImpl service = service();
        when(runMapper.selectByIncidentIdAndId(11L, 91L)).thenReturn(run());

        assertThatThrownBy(() -> service.save(
                11L, 91L, "UNKNOWN", null, null, null, 7L))
                .isInstanceOf(SreValidationException.class)
                .hasMessage("RCA 准确度评价不合法");
        assertThatThrownBy(() -> service.save(
                11L, 91L, "ACCURATE", "REASONING_GAP", null, null, 7L))
                .isInstanceOf(SreValidationException.class)
                .hasMessage("准确评价不能设置缺口类型");
        assertThatThrownBy(() -> service.save(
                11L, 91L, "PARTIAL", null, null, "期望结论", 7L))
                .isInstanceOf(SreValidationException.class)
                .hasMessage("部分准确或不准确评价必须设置缺口类型");
        assertThatThrownBy(() -> service.save(
                11L, 91L, "INACCURATE", "TOOL_FAILURE", null, null, 7L))
                .isInstanceOf(SreValidationException.class)
                .hasMessage("部分准确或不准确评价必须填写期望结论");

        verify(feedbackMapper, never()).insert(any());
    }

    @Test
    void feedbackCannotCrossIncidentBoundary() {
        SreInvestigationFeedbackServiceImpl service = service();
        when(runMapper.selectByIncidentIdAndId(12L, 91L)).thenReturn(null);

        assertThat(service.save(
                12L, 91L, "ACCURATE", null, "结论可信", null, 7L)).isEmpty();
        assertThat(service.findLatest(12L, 91L)).isEmpty();

        verify(feedbackMapper, never()).insert(any());
        verify(feedbackMapper, never()).selectLatestByRunId(any());
    }

    @Test
    void latestFeedbackIsReadOnlyAndBoundToRun() {
        SreInvestigationFeedbackServiceImpl service = service();
        SreInvestigationFeedback feedback = new SreInvestigationFeedback();
        feedback.setId(101L);
        feedback.setRunId(91L);
        when(runMapper.selectByIncidentIdAndId(11L, 91L)).thenReturn(run());
        when(feedbackMapper.selectLatestByRunId(91L)).thenReturn(feedback);

        assertThat(service.findLatest(11L, 91L)).contains(feedback);
    }

    private SreInvestigationFeedbackServiceImpl service() {
        return new SreInvestigationFeedbackServiceImpl(runMapper, feedbackMapper);
    }

    private SreInvestigationRun run() {
        SreInvestigationRun run = new SreInvestigationRun();
        run.setId(91L);
        run.setIncidentId(11L);
        return run;
    }
}
