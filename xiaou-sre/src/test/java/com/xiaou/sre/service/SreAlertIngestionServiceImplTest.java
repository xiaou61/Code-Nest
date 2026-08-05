package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreIncidentAlertRelation;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.dto.response.SreIngestionResult;
import com.xiaou.sre.mapper.SreAlertEventMapper;
import com.xiaou.sre.mapper.SreIncidentAlertRelationMapper;
import com.xiaou.sre.mapper.SreIncidentMapper;
import com.xiaou.sre.mapper.SreOutboxEventMapper;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.service.impl.SreAlertIngestionServiceImpl;
import com.xiaou.sre.service.model.SreAlertBatch;
import com.xiaou.sre.service.model.SreAlertRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreAlertIngestionServiceImplTest {

    @Mock
    private SreAlertEventMapper alertEventMapper;

    @Mock
    private SreIncidentMapper incidentMapper;

    @Mock
    private SreIncidentAlertRelationMapper relationMapper;

    @Mock
    private SreOutboxEventMapper outboxEventMapper;

    @Mock
    private SreMetricsRecorder metricsRecorder;

    @InjectMocks
    private SreAlertIngestionServiceImpl service;

    @Test
    void firingAlertCreatesEventIncidentRelationAndOutbox() {
        SreAlertBatch request = request(alert("firing", "fp-1", "api", "CodeNestTargetDown"));
        when(alertEventMapper.selectByFingerprintAndStartsAt(any(), any())).thenReturn(null);
        when(incidentMapper.selectActiveByIncidentKey("api|CodeNestTargetDown")).thenReturn(null);
        when(alertEventMapper.insert(any())).thenAnswer(invocation -> {
            SreAlertEvent event = invocation.getArgument(0);
            event.setId(21L);
            return 1;
        });
        when(incidentMapper.insert(any())).thenAnswer(invocation -> {
            SreIncident incident = invocation.getArgument(0);
            incident.setId(11L);
            return 1;
        });
        when(relationMapper.insert(any())).thenReturn(1);
        when(outboxEventMapper.insert(any())).thenReturn(1);

        SreIngestionResult result = service.ingest(request);

        assertThat(result.getReceived()).isEqualTo(1);
        assertThat(result.getCreatedEvents()).isEqualTo(1);
        assertThat(result.getCreatedIncidents()).isEqualTo(1);
        assertThat(result.getDuplicates()).isZero();
        verify(relationMapper).insert(any(SreIncidentAlertRelation.class));
        verify(outboxEventMapper).insert(any(SreOutboxEvent.class));
        verify(metricsRecorder).recordAlertIngestion(
                org.mockito.ArgumentMatchers.eq("succeeded"),
                org.mockito.ArgumentMatchers.eq(1),
                anyLong());
        verify(metricsRecorder).incrementQueueEvent("outbox", "enqueued", 1);

        ArgumentCaptor<SreAlertEvent> eventCaptor = ArgumentCaptor.forClass(SreAlertEvent.class);
        verify(alertEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStartsAt()).isEqualTo("2026-07-20T00:00:00Z");
        assertThat(eventCaptor.getValue().getGeneratorUrl()).isEqualTo("http://prometheus/graph");

        ArgumentCaptor<SreIncident> incidentCaptor = ArgumentCaptor.forClass(SreIncident.class);
        verify(incidentMapper).insert(incidentCaptor.capture());
        assertThat(incidentCaptor.getValue().getFirstSeen())
                .hasToString("2026-07-20T08:00");
    }

    @Test
    void duplicateAlertIsIdempotentAndDoesNotCreateAnotherIncident() {
        SreAlertEvent existing = new SreAlertEvent();
        existing.setId(21L);
        existing.setStatus("FIRING");
        existing.setFingerprint("fp-1");
        existing.setStartsAt("2026-07-20T00:00:00Z");
        when(alertEventMapper.selectByFingerprintAndStartsAt("fp-1", "2026-07-20T00:00:00Z"))
                .thenReturn(existing);

        SreIngestionResult result = service.ingest(request(alert("firing", "fp-1", "api", "CodeNestTargetDown")));

        assertThat(result.getReceived()).isEqualTo(1);
        assertThat(result.getDuplicates()).isEqualTo(1);
        assertThat(result.getCreatedEvents()).isZero();
        assertThat(result.getCreatedIncidents()).isZero();
        verify(alertEventMapper, never()).insert(any());
        verify(incidentMapper, never()).insert(any());
        verify(outboxEventMapper, never()).insert(any());
        verify(metricsRecorder).recordAlertIngestion(
                org.mockito.ArgumentMatchers.eq("duplicate"),
                org.mockito.ArgumentMatchers.eq(1),
                anyLong());
    }

    @Test
    void resolvedLastAlertClosesActiveIncident() {
        SreAlertEvent existing = new SreAlertEvent();
        existing.setId(21L);
        existing.setStatus("FIRING");
        existing.setFingerprint("fp-1");
        existing.setStartsAt("2026-07-20T00:00:00Z");
        SreIncident incident = new SreIncident();
        incident.setId(11L);
        incident.setState("OPEN");
        when(alertEventMapper.selectByFingerprintAndStartsAt("fp-1", "2026-07-20T00:00:00Z"))
                .thenReturn(existing);
        when(relationMapper.selectByAlertEventId(21L)).thenReturn(new SreIncidentAlertRelation(11L, 21L));
        when(relationMapper.countActiveByIncidentId(11L)).thenReturn(0);
        when(incidentMapper.selectById(11L)).thenReturn(incident);

        SreAlertRecord resolvedAlert = alert("resolved", "fp-1", "api", "CodeNestTargetDown", "x".repeat(1200));
        SreIngestionResult result = service.ingest(request(resolvedAlert));

        assertThat(result.getUpdatedEvents()).isEqualTo(1);
        ArgumentCaptor<SreAlertEvent> eventCaptor = ArgumentCaptor.forClass(SreAlertEvent.class);
        verify(alertEventMapper).updateStatusAndPayload(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getGeneratorUrl()).hasSize(1000);
        ArgumentCaptor<SreIncident> incidentCaptor = ArgumentCaptor.forClass(SreIncident.class);
        verify(incidentMapper).markResolved(incidentCaptor.capture());
        assertThat(incidentCaptor.getValue().getId()).isEqualTo(11L);
        assertThat(result.getCreatedIncidents()).isZero();
    }

    @Test
    void invalidAlertTimeIsRejectedWithoutPersistence() {
        SreAlertRecord original = alert("firing", "fp-1", "api", "CodeNestTargetDown");
        SreAlertRecord alert = new SreAlertRecord(
                original.status(), original.labels(), original.annotations(), "not-an-iso-time",
                original.endsAt(), original.generatorUrl(), original.fingerprint(), original.rawPayload());

        assertThatThrownBy(() -> service.ingest(request(alert)))
                .isInstanceOf(SreValidationException.class)
                .hasMessageContaining("startsAt");

        verify(alertEventMapper, never()).insert(any());
        verify(incidentMapper, never()).insert(any());
    }

    private SreAlertBatch request(SreAlertRecord alert) {
        return new SreAlertBatch("alertmanager", List.of(alert));
    }

    private SreAlertRecord alert(String status, String fingerprint, String service, String alertName) {
        return alert(status, fingerprint, service, alertName, "http://prometheus/graph");
    }

    private SreAlertRecord alert(String status,
                                 String fingerprint,
                                 String service,
                                 String alertName,
                                 String generatorUrl) {
        return new SreAlertRecord(
                status,
                Map.of(
                "alertname", alertName,
                "service", service,
                "severity", "critical"
                ),
                Map.of(
                "summary", "Code-Nest target is down",
                "description", "test alert"
                ),
                "2026-07-20T00:00:00Z",
                "0001-01-01T00:00:00Z",
                generatorUrl,
                fingerprint,
                "{\"status\":\"" + status + "\"}"
        );
    }
}
