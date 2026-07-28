package com.xiaou.sre.metrics;

import com.xiaou.sre.config.SreMetricsProperties;
import com.xiaou.sre.mapper.SreOperationalMetricsMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SreOperationalMetricsPublisherTest {

    @Test
    void shouldSkipDatabaseWhenMetricsSnapshotIsDisabled() {
        SreMetricsProperties properties = new SreMetricsProperties();
        SreOperationalMetricsMapper mapper = mock(SreOperationalMetricsMapper.class);
        SreMetricsRecorder recorder = mock(SreMetricsRecorder.class);
        SreOperationalMetricsPublisher publisher = new SreOperationalMetricsPublisher(
                properties, mapper, recorder);

        publisher.refresh();

        verifyNoInteractions(mapper, recorder);
    }

    @Test
    void shouldPublishOneConsistentDatabaseSnapshot() {
        SreMetricsProperties properties = new SreMetricsProperties();
        properties.setEnabled(true);
        SreOperationalMetricsMapper mapper = mock(SreOperationalMetricsMapper.class);
        SreMetricsRecorder recorder = mock(SreMetricsRecorder.class);
        SreOperationalMetricsRow row = new SreOperationalMetricsRow();
        row.setOpenIncidents(4L);
        row.setOutboxBacklog(7L);
        row.setOutboxOldestAgeSeconds(81L);
        row.setEvaluationBacklog(3L);
        row.setEvaluationOldestAgeSeconds(42L);
        row.setActiveInvestigations(1L);
        when(mapper.selectSnapshot()).thenReturn(row);
        SreOperationalMetricsPublisher publisher = new SreOperationalMetricsPublisher(
                properties, mapper, recorder);

        publisher.refresh();

        verify(recorder).updateOperationalSnapshot(new SreOperationalMetricsSnapshot(
                4, 7, 81, 3, 42, 1));
    }

    @Test
    void shouldKeepLastSnapshotWhenDatabaseQueryFails() {
        SreMetricsProperties properties = new SreMetricsProperties();
        properties.setEnabled(true);
        SreOperationalMetricsMapper mapper = mock(SreOperationalMetricsMapper.class);
        SreMetricsRecorder recorder = mock(SreMetricsRecorder.class);
        when(mapper.selectSnapshot()).thenThrow(new IllegalStateException("database unavailable"));
        SreOperationalMetricsPublisher publisher = new SreOperationalMetricsPublisher(
                properties, mapper, recorder);

        assertDoesNotThrow(publisher::refresh);

        verify(recorder, never()).updateOperationalSnapshot(org.mockito.ArgumentMatchers.any());
    }
}
