package com.xiaou.web.growthcoach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.web.growthcoach.domain.GrowthEvidence;
import com.xiaou.web.growthcoach.domain.GrowthEvidenceProjectionCursor;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceSummaryResponse;
import com.xiaou.web.growthcoach.evidence.GrowthEvidenceAdapter;
import com.xiaou.web.growthcoach.evidence.GrowthEvidenceProjection;
import com.xiaou.web.growthcoach.mapper.GrowthEvidenceMapper;
import com.xiaou.web.growthcoach.mapper.GrowthEvidenceProjectionCursorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthEvidenceProjectorServiceTest {

    private static final Long USER_ID = 7L;
    private static final LocalDateTime SOURCE_TIME = LocalDateTime.of(2026, 7, 20, 9, 30);

    @Mock
    private GrowthEvidenceMapper evidenceMapper;
    @Mock
    private GrowthEvidenceProjectionCursorMapper cursorMapper;
    @Mock
    private GrowthEvidenceAdapter adapter;
    @Mock
    private GrowthEvidenceProjectorService projectorService;

    private GrowthEvidenceProjectorService service;
    private GrowthEvidenceQueryService queryService;

    @BeforeEach
    void setUp() {
        service = new GrowthEvidenceProjectorService(
                evidenceMapper,
                cursorMapper,
                List.of(adapter),
                new GrowthCoachProperties(),
                new ObjectMapper().findAndRegisterModules()
        );
        queryService = new GrowthEvidenceQueryService(evidenceMapper, projectorService, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void keepsTheSameEvidenceIdentityWhenTheSameSourceIsProjectedAgain() {
        GrowthEvidenceProjection projection = activeTaskProjection(45);
        GrowthEvidenceProjectionCursor cursor = cursor(SOURCE_TIME, 42L);
        when(adapter.sourceKey()).thenReturn("growth_autopilot_task");
        when(cursorMapper.selectByUserAndSourceKey(USER_ID, "growth_autopilot_task"))
                .thenReturn(null, cursor);
        when(adapter.loadChanges(eq(USER_ID), any(), anyLong(), anyInt()))
                .thenReturn(List.of(projection), List.of(projection));

        service.refreshForUser(USER_ID);
        service.refreshForUser(USER_ID);

        ArgumentCaptor<GrowthEvidence> evidenceCaptor = ArgumentCaptor.forClass(GrowthEvidence.class);
        verify(evidenceMapper, times(2)).upsert(evidenceCaptor.capture());
        List<GrowthEvidence> projected = evidenceCaptor.getAllValues();
        assertThat(projected.get(0).getEvidenceId()).isEqualTo(projected.get(1).getEvidenceId());
        assertThat(projected.get(0).getContentHash()).isEqualTo(projected.get(1).getContentHash());
        verify(cursorMapper, times(2)).upsert(any(GrowthEvidenceProjectionCursor.class));
    }

    @Test
    void refreshesEvidenceSummaryWhenTheSourceRecordChanges() {
        GrowthEvidenceProjection first = activeTaskProjection(45);
        GrowthEvidenceProjection changed = activeTaskProjection(90);
        GrowthEvidenceProjectionCursor cursor = cursor(SOURCE_TIME, 42L);
        when(adapter.sourceKey()).thenReturn("growth_autopilot_task");
        when(cursorMapper.selectByUserAndSourceKey(USER_ID, "growth_autopilot_task"))
                .thenReturn(null, cursor);
        when(adapter.loadChanges(eq(USER_ID), any(), anyLong(), anyInt()))
                .thenReturn(List.of(first), List.of(changed));

        service.refreshForUser(USER_ID);
        service.refreshForUser(USER_ID);

        ArgumentCaptor<GrowthEvidence> evidenceCaptor = ArgumentCaptor.forClass(GrowthEvidence.class);
        verify(evidenceMapper, times(2)).upsert(evidenceCaptor.capture());
        List<GrowthEvidence> projected = evidenceCaptor.getAllValues();
        assertThat(projected.get(0).getEvidenceId()).isEqualTo(projected.get(1).getEvidenceId());
        assertThat(projected.get(0).getContentHash()).isNotEqualTo(projected.get(1).getContentHash());
        assertThat(projected.get(1).getSummaryJson()).contains("90");
    }

    @Test
    void invalidatesEvidenceInsteadOfDeletingItWhenTheSourceIsNoLongerCompleted() {
        GrowthEvidenceProjection projection = activeTaskProjection(45);
        projection.setActive(false);
        when(adapter.sourceKey()).thenReturn("growth_autopilot_task");
        when(cursorMapper.selectByUserAndSourceKey(USER_ID, "growth_autopilot_task")).thenReturn(null);
        when(adapter.loadChanges(eq(USER_ID), any(), anyLong(), anyInt())).thenReturn(List.of(projection));

        service.refreshForUser(USER_ID);

        verify(evidenceMapper).invalidateBySource(
                USER_ID,
                "plan",
                "growth_autopilot_task",
                "42",
                "TASK_COMPLETED",
                "v1",
                SOURCE_TIME
        );
        verify(evidenceMapper, org.mockito.Mockito.never()).upsert(any(GrowthEvidence.class));
    }

    @Test
    void returnsOnlyEvidenceOwnedByTheCurrentUser() {
        GrowthEvidence ownEvidence = evidence("evidence-own", USER_ID);
        GrowthEvidence foreignEvidence = evidence("evidence-foreign", 9L);
        when(evidenceMapper.selectRecentValidByUser(USER_ID, 10)).thenReturn(List.of(ownEvidence, foreignEvidence));

        List<GrowthEvidenceSummaryResponse> response = queryService.listForUser(USER_ID, 10);

        verify(projectorService).refreshForUser(USER_ID);
        verify(evidenceMapper).selectRecentValidByUser(USER_ID, 10);
        assertThat(response).extracting(GrowthEvidenceSummaryResponse::getEvidenceId)
                .containsExactly("evidence-own");
    }

    @Test
    void skipsSourceReadsWhenEvidenceProjectionIsDisabled() {
        GrowthCoachProperties properties = new GrowthCoachProperties();
        properties.getEvidenceProjection().setEnabled(false);
        GrowthEvidenceProjectorService disabledService = new GrowthEvidenceProjectorService(
                evidenceMapper,
                cursorMapper,
                List.of(adapter),
                properties,
                new ObjectMapper().findAndRegisterModules()
        );

        disabledService.refreshForUser(USER_ID);

        verifyNoInteractions(adapter, cursorMapper, evidenceMapper);
    }

    private GrowthEvidenceProjection activeTaskProjection(int plannedMinutes) {
        GrowthEvidenceProjection projection = new GrowthEvidenceProjection();
        projection.setUserId(USER_ID);
        projection.setSourceModule("plan");
        projection.setSourceType("growth_autopilot_task");
        projection.setSourceId("42");
        projection.setSourceRecordId(42L);
        projection.setEvidenceType("TASK_COMPLETED");
        projection.setSkillKey("java");
        projection.setQualityLevel("VERIFIED");
        projection.setObservedAt(SOURCE_TIME);
        projection.setSourceUpdatedAt(SOURCE_TIME);
        projection.setActive(true);
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleKey", "java");
        summary.put("plannedMinutes", plannedMinutes);
        summary.put("planVersion", 2);
        projection.setSummary(summary);
        return projection;
    }

    private GrowthEvidenceProjectionCursor cursor(LocalDateTime cursorTime, Long cursorSourceId) {
        GrowthEvidenceProjectionCursor cursor = new GrowthEvidenceProjectionCursor();
        cursor.setUserId(USER_ID);
        cursor.setSourceKey("growth_autopilot_task");
        cursor.setCursorTime(cursorTime);
        cursor.setCursorSourceId(cursorSourceId);
        return cursor;
    }

    private GrowthEvidence evidence(String evidenceId, Long userId) {
        GrowthEvidence evidence = new GrowthEvidence();
        evidence.setEvidenceId(evidenceId);
        evidence.setUserId(userId);
        evidence.setEvidenceType("TASK_COMPLETED");
        evidence.setSourceModule("plan");
        evidence.setSourceType("growth_autopilot_task");
        evidence.setSourceId("42");
        evidence.setSkillKey("java");
        evidence.setQualityLevel("VERIFIED");
        evidence.setObservedAt(SOURCE_TIME);
        evidence.setSummaryJson("{\"plannedMinutes\":45}");
        return evidence;
    }
}
