package com.xiaou.sre.service;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreIncidentEvidence;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.client.SrePrometheusEvidence;
import com.xiaou.sre.client.SreLokiEvidence;
import com.xiaou.sre.service.SreLokiEvidenceCollector;
import com.xiaou.sre.mapper.SreAlertEventMapper;
import com.xiaou.sre.mapper.SreIncidentEvidenceMapper;
import com.xiaou.sre.mapper.SreIncidentMapper;
import com.xiaou.sre.service.impl.SreEvidenceCollectionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreEvidenceCollectionServiceImplTest {

    @Mock
    private SreAlertEventMapper alertEventMapper;

    @Mock
    private SreIncidentMapper incidentMapper;

    @Mock
    private SreIncidentEvidenceMapper evidenceMapper;

    @Mock
    private SrePrometheusEvidenceCollector prometheusEvidenceCollector;

    @Mock
    private SreLokiEvidenceCollector lokiEvidenceCollector;

    @Test
    void collectCreatesBoundedAlertSnapshot() {
        SreEvidenceCollectionServiceImpl service = new SreEvidenceCollectionServiceImpl(
                alertEventMapper, incidentMapper, evidenceMapper, prometheusEvidenceCollector, lokiEvidenceCollector);
        SreOutboxEvent event = event(100L, 11L, 21L);
        SreIncident incident = new SreIncident();
        incident.setId(11L);
        incident.setIncidentNo("SRE-ABC123");
        incident.setService("api");
        incident.setState("OPEN");
        incident.setSummary("target down");
        SreAlertEvent alert = new SreAlertEvent();
        alert.setId(21L);
        alert.setSource("alertmanager");
        alert.setFingerprint("fp-1");
        alert.setStatus("FIRING");
        alert.setService("api");
        alert.setLabelsJson(JsonUtils.toJsonString(Map.of("alertname", "TargetDown")));
        alert.setAnnotationsJson(JsonUtils.toJsonString(Map.of("summary", "target down")));
        alert.setGeneratorUrl("http://prometheus/graph");
        when(evidenceMapper.selectByOutboxEventAndSource(100L, "ALERT_SNAPSHOT")).thenReturn(null);
        when(alertEventMapper.selectById(21L)).thenReturn(alert);
        when(incidentMapper.selectById(11L)).thenReturn(incident);

        service.collect(event);

        ArgumentCaptor<SreIncidentEvidence> captor = ArgumentCaptor.forClass(SreIncidentEvidence.class);
        verify(evidenceMapper).insert(captor.capture());
        SreIncidentEvidence evidence = captor.getValue();
        assertThat(evidence.getIncidentId()).isEqualTo(11L);
        assertThat(evidence.getOutboxEventId()).isEqualTo(100L);
        assertThat(evidence.getSourceType()).isEqualTo("ALERT_SNAPSHOT");
        assertThat(evidence.getQuery()).isEqualTo("sre_alert_event.id=21");
        assertThat(evidence.getSnapshotJson()).contains("TargetDown", "target down");
        assertThat(evidence.getSnapshotJson()).doesNotContain("rawPayload");
    }

    @Test
    void existingSnapshotIsIdempotent() {
        SreEvidenceCollectionServiceImpl service = new SreEvidenceCollectionServiceImpl(
                alertEventMapper, incidentMapper, evidenceMapper, prometheusEvidenceCollector, lokiEvidenceCollector);
        SreOutboxEvent event = event(100L, 11L, 21L);
        when(evidenceMapper.selectByOutboxEventAndSource(100L, "ALERT_SNAPSHOT"))
                .thenReturn(new SreIncidentEvidence());

        service.collect(event);

        verify(alertEventMapper, never()).selectById(any());
        verify(incidentMapper, never()).selectById(any());
        verify(evidenceMapper, never()).insert(any());
    }

    @Test
    void malformedPayloadIsRejectedWithoutPersistence() {
        SreEvidenceCollectionServiceImpl service = new SreEvidenceCollectionServiceImpl(
                alertEventMapper, incidentMapper, evidenceMapper, prometheusEvidenceCollector, lokiEvidenceCollector);
        SreOutboxEvent event = event(100L, 11L, 21L);
        event.setPayloadJson("{not-json}");
        when(evidenceMapper.selectByOutboxEventAndSource(100L, "ALERT_SNAPSHOT")).thenReturn(null);

        assertThatThrownBy(() -> service.collect(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON");

        verify(evidenceMapper, never()).insert(any());
    }

    @Test
    void enabledPrometheusCollectorAddsSecondEvidenceRecord() {
        SreEvidenceCollectionServiceImpl service = new SreEvidenceCollectionServiceImpl(
                alertEventMapper, incidentMapper, evidenceMapper, prometheusEvidenceCollector, lokiEvidenceCollector);
        SreOutboxEvent event = event(100L, 11L, 21L);
        SreIncident incident = new SreIncident();
        incident.setId(11L);
        SreAlertEvent alert = new SreAlertEvent();
        alert.setId(21L);
        alert.setAlertName("CodeNestTargetDown");
        when(prometheusEvidenceCollector.isEnabled()).thenReturn(true);
        when(evidenceMapper.selectByOutboxEventAndSource(anyLong(), anyString())).thenReturn(null);
        when(alertEventMapper.selectById(21L)).thenReturn(alert);
        when(incidentMapper.selectById(11L)).thenReturn(incident);
        SrePrometheusEvidence prometheusEvidence = new SrePrometheusEvidence();
        prometheusEvidence.setSourceType(SrePrometheusEvidenceCollector.PROMETHEUS_SOURCE_TYPE);
        prometheusEvidence.setSourceRef("target_up");
        prometheusEvidence.setQuery("up{job=\"code-nest\"}");
        prometheusEvidence.setSnapshotJson("{\"status\":\"success\"}");
        when(prometheusEvidenceCollector.collect(event, alert, incident)).thenReturn(prometheusEvidence);

        service.collect(event);

        org.mockito.ArgumentCaptor<SreIncidentEvidence> captor =
                org.mockito.ArgumentCaptor.forClass(SreIncidentEvidence.class);
        verify(evidenceMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(SreIncidentEvidence::getSourceType)
                .containsExactly("ALERT_SNAPSHOT", SrePrometheusEvidenceCollector.PROMETHEUS_SOURCE_TYPE);
    }

    @Test
    void enabledLokiCollectorAddsLogEvidenceAfterAlertSnapshot() {
        SreEvidenceCollectionServiceImpl service = new SreEvidenceCollectionServiceImpl(
                alertEventMapper, incidentMapper, evidenceMapper, prometheusEvidenceCollector, lokiEvidenceCollector);
        SreOutboxEvent event = event(100L, 11L, 21L);
        SreIncident incident = new SreIncident();
        incident.setId(11L);
        SreAlertEvent alert = new SreAlertEvent();
        alert.setId(21L);
        alert.setAlertName("CodeNestTargetDown");
        when(lokiEvidenceCollector.isEnabled()).thenReturn(true);
        when(evidenceMapper.selectByOutboxEventAndSource(anyLong(), anyString())).thenReturn(null);
        when(alertEventMapper.selectById(21L)).thenReturn(alert);
        when(incidentMapper.selectById(11L)).thenReturn(incident);
        SreLokiEvidence lokiEvidence = new SreLokiEvidence();
        lokiEvidence.setSourceType(SreLokiEvidenceCollector.LOKI_SOURCE_TYPE);
        lokiEvidence.setSourceRef("application_errors");
        lokiEvidence.setQuery("{job=\"code-nest\"} |~ \"(?i)(error|exception|timeout)\"");
        lokiEvidence.setSnapshotJson("{\"status\":\"success\"}");
        when(lokiEvidenceCollector.collect(event, alert, incident)).thenReturn(lokiEvidence);

        service.collect(event);

        ArgumentCaptor<SreIncidentEvidence> captor = ArgumentCaptor.forClass(SreIncidentEvidence.class);
        verify(evidenceMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(SreIncidentEvidence::getSourceType)
                .containsExactly("ALERT_SNAPSHOT", SreLokiEvidenceCollector.LOKI_SOURCE_TYPE);
    }

    private SreOutboxEvent event(Long id, Long incidentId, Long alertEventId) {
        SreOutboxEvent event = new SreOutboxEvent();
        event.setId(id);
        event.setAggregateId(incidentId);
        event.setEventType("EVIDENCE_COLLECTION_REQUESTED");
        event.setPayloadJson(JsonUtils.toJsonString(Map.of(
                "incidentId", incidentId,
                "alertEventId", alertEventId,
                "status", "FIRING"
        )));
        return event;
    }
}
