package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.domain.SreInvestigationStep;
import com.xiaou.sre.mapper.SreInvestigationRunMapper;
import com.xiaou.sre.mapper.SreInvestigationStepMapper;
import com.xiaou.sre.service.impl.SreInvestigationRunServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreInvestigationRunServiceImplTest {

    @Mock
    private SreInvestigationRunMapper runMapper;

    @Mock
    private SreInvestigationStepMapper stepMapper;

    @Test
    void startPersistsBoundedAuditMetadata() {
        SreInvestigationRunServiceImpl service = service();
        doAnswer(invocation -> {
            invocation.<SreInvestigationRun>getArgument(0).setId(91L);
            return 1;
        }).when(runMapper).insert(any(SreInvestigationRun.class));

        SreInvestigationRun run = service.start(11L, "ADMIN_API", 7L, 2, 3, false);

        assertThat(run.getId()).isEqualTo(91L);
        assertThat(run.getIncidentId()).isEqualTo(11L);
        assertThat(run.getStatus()).isEqualTo("RUNNING");
        assertThat(run.getTriggerSource()).isEqualTo("ADMIN_API");
        assertThat(run.getRequestedBy()).isEqualTo(7L);
        assertThat(run.getAlertCount()).isEqualTo(2);
        assertThat(run.getEvidenceCount()).isEqualTo(3);
        assertThat(run.getContextTruncated()).isFalse();
    }

    @Test
    void invalidIncidentCannotStartRun() {
        SreInvestigationRunServiceImpl service = service();

        assertThatThrownBy(() -> service.start(0L, "ADMIN_API", 7L, 0, 0, false))
                .isInstanceOf(SreValidationException.class);

        verify(runMapper, never()).insert(any());
    }

    @Test
    void recordStepNormalizesMetadataAndBoundsDetail() {
        SreInvestigationRunServiceImpl service = service();
        String detail = "x".repeat(800);
        when(stepMapper.insert(any(SreInvestigationStep.class))).thenReturn(1);

        service.recordStep(91L, 2, "model_analysis", "degraded", detail);

        org.mockito.ArgumentCaptor<SreInvestigationStep> captor =
                org.mockito.ArgumentCaptor.forClass(SreInvestigationStep.class);
        verify(stepMapper).insert(captor.capture());
        assertThat(captor.getValue().getStepCode()).isEqualTo("MODEL_ANALYSIS");
        assertThat(captor.getValue().getStatus()).isEqualTo("DEGRADED");
        assertThat(captor.getValue().getDetail()).hasSize(500);
    }

    @Test
    void listRunsClampsLimitAndNormalizesNullMapperResult() {
        SreInvestigationRunServiceImpl service = service();
        when(runMapper.selectByIncidentId(11L, 50)).thenReturn(null);

        assertThat(service.listByIncidentId(11L, 999)).isEmpty();

        verify(runMapper).selectByIncidentId(11L, 50);
    }

    @Test
    void detailQueryRequiresRunToBelongToIncident() {
        SreInvestigationRunServiceImpl service = service();
        SreInvestigationRun run = new SreInvestigationRun();
        run.setId(91L);
        run.setIncidentId(11L);
        when(runMapper.selectByIncidentIdAndId(11L, 91L)).thenReturn(run);
        SreInvestigationStep step = new SreInvestigationStep();
        when(stepMapper.selectByRunId(91L)).thenReturn(List.of(step));

        assertThat(service.findByIncidentIdAndRunId(11L, 91L)).contains(run);
        assertThat(service.listSteps(91L)).containsExactly(step);
        assertThat(service.findByIncidentIdAndRunId(11L, 0L)).isEmpty();
    }

    private SreInvestigationRunServiceImpl service() {
        return new SreInvestigationRunServiceImpl(runMapper, stepMapper);
    }
}
