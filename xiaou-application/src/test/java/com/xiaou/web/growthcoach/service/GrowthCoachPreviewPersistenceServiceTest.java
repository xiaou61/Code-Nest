package com.xiaou.web.growthcoach.service;

import com.xiaou.web.growthcoach.domain.GrowthCoachActionEvent;
import com.xiaou.web.growthcoach.domain.GrowthCoachActionRun;
import com.xiaou.web.growthcoach.mapper.GrowthCoachActionEventMapper;
import com.xiaou.web.growthcoach.mapper.GrowthCoachActionRunMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthCoachPreviewPersistenceServiceTest {

    @Mock
    private GrowthCoachActionRunMapper actionRunMapper;
    @Mock
    private GrowthCoachActionEventMapper actionEventMapper;

    @Test
    void persistsThePreviewAndItsInitialAuditEventTogether() {
        GrowthCoachPreviewPersistenceService service = new GrowthCoachPreviewPersistenceService(
                actionRunMapper, actionEventMapper
        );
        GrowthCoachActionRun run = new GrowthCoachActionRun();
        run.setRunId("growth-run-1");
        run.setStatus("PREVIEW");
        when(actionRunMapper.insert(run)).thenReturn(1);
        when(actionEventMapper.selectNextSequence("growth-run-1")).thenReturn(1);
        when(actionEventMapper.insert(org.mockito.ArgumentMatchers.any(GrowthCoachActionEvent.class))).thenReturn(1);

        service.persistPreview(run);

        verify(actionRunMapper).insert(run);
        ArgumentCaptor<GrowthCoachActionEvent> eventCaptor = ArgumentCaptor.forClass(GrowthCoachActionEvent.class);
        verify(actionEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getRunId()).isEqualTo("growth-run-1");
        assertThat(eventCaptor.getValue().getToStatus()).isEqualTo("PREVIEW");
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("preview_created");
    }
}
