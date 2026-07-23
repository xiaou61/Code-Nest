package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreIncidentEvidence;
import com.xiaou.sre.dto.response.SreInvestigationContext;
import com.xiaou.sre.mapper.SreAlertEventMapper;
import com.xiaou.sre.mapper.SreIncidentEvidenceMapper;
import com.xiaou.sre.mapper.SreIncidentMapper;
import com.xiaou.sre.service.impl.SreInvestigationFacadeImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreInvestigationFacadeImplTest {

    @Mock
    private SreIncidentMapper incidentMapper;

    @Mock
    private SreAlertEventMapper alertEventMapper;

    @Mock
    private SreIncidentEvidenceMapper evidenceMapper;

    @Test
    void missingIncidentReturnsEmptyWithoutLoadingRelatedData() {
        SreInvestigationFacadeImpl facade = facade();
        when(incidentMapper.selectById(11L)).thenReturn(null);

        Optional<SreInvestigationContext> result = facade.findByIncidentId(11L);

        assertThat(result).isEmpty();
        verify(alertEventMapper, never()).selectByIncidentId(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
        verify(evidenceMapper, never()).selectForInvestigation(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void contextIsBoundedAndRemovesSensitiveEvidence() {
        SreInvestigationFacadeImpl facade = facade();
        SreIncident incident = incident();
        incident.setSummary("s".repeat(1_200));
        when(incidentMapper.selectById(11L)).thenReturn(incident);
        when(alertEventMapper.selectByIncidentId(11L, 21)).thenReturn(IntStream.rangeClosed(1, 21)
                .mapToObj(this::alert)
                .toList());
        when(evidenceMapper.selectForInvestigation(11L, 21)).thenReturn(IntStream.rangeClosed(1, 21)
                .mapToObj(this::evidence)
                .toList());

        SreInvestigationContext context = facade.findByIncidentId(11L).orElseThrow();

        assertThat(context.incident().summary()).hasSize(1_000);
        assertThat(context.alerts()).hasSize(20);
        assertThat(context.evidence()).hasSize(20);
        assertThat(context.alertsTruncated()).isTrue();
        assertThat(context.evidenceTruncated()).isTrue();

        SreInvestigationContext.Evidence first = context.evidence().get(0);
        assertThat(first.queryDescription()).hasSize(500);
        assertThat(first.status()).isEqualTo("AVAILABLE");
        assertThat(first.snapshot()).doesNotContainKey("rawPayload");
        assertThat(first.snapshot()).containsEntry("password", "[REDACTED]");
        assertThat(first.snapshot().get("message").toString())
                .doesNotContain("Bearer abcdefghijklmnop")
                .doesNotContain("abcdefghijklmnop")
                .contains("[REDACTED]");
        assertThat(first.snapshot().get("detail").toString()).hasSize(1_000);
        assertThat(first.snapshotTruncated()).isTrue();
    }

    @Test
    void emptyRelatedRowsAreNormalizedToEmptyLists() {
        SreInvestigationFacadeImpl facade = facade();
        when(incidentMapper.selectById(11L)).thenReturn(incident());
        when(alertEventMapper.selectByIncidentId(11L, 21)).thenReturn(null);
        when(evidenceMapper.selectForInvestigation(11L, 21)).thenReturn(null);

        SreInvestigationContext context = facade.findByIncidentId(11L).orElseThrow();

        assertThat(context.alerts()).isEmpty();
        assertThat(context.evidence()).isEmpty();
        assertThat(context.alertsTruncated()).isFalse();
        assertThat(context.evidenceTruncated()).isFalse();
    }

    @Test
    void malformedSnapshotIsMarkedInvalidWithoutReturningOriginalText() {
        SreInvestigationFacadeImpl facade = facade();
        when(incidentMapper.selectById(11L)).thenReturn(incident());
        when(alertEventMapper.selectByIncidentId(11L, 21)).thenReturn(List.of());
        SreIncidentEvidence evidence = new SreIncidentEvidence();
        evidence.setId(1L);
        evidence.setSourceType("LOKI_SNAPSHOT");
        evidence.setSnapshotJson("not-json Authorization=Bearer-secret");
        when(evidenceMapper.selectForInvestigation(11L, 21)).thenReturn(List.of(evidence));

        SreInvestigationContext.Evidence result = facade.findByIncidentId(11L).orElseThrow()
                .evidence().get(0);

        assertThat(result.status()).isEqualTo("INVALID");
        assertThat(result.snapshot()).containsEntry("reason", "evidence_payload_invalid");
        assertThat(result.snapshot().toString()).doesNotContain("Bearer-secret");
        assertThat(result.snapshotTruncated()).isTrue();
    }

    private SreInvestigationFacadeImpl facade() {
        return new SreInvestigationFacadeImpl(incidentMapper, alertEventMapper, evidenceMapper);
    }

    private SreIncident incident() {
        SreIncident incident = new SreIncident();
        incident.setId(11L);
        incident.setIncidentNo("SRE-20260723-001");
        incident.setService("code-nest");
        incident.setAlertName("CodeNestTargetDown");
        incident.setSeverity("critical");
        incident.setState("OPEN");
        incident.setSummary("target down");
        incident.setFirstSeen(LocalDateTime.of(2026, 7, 23, 1, 0));
        incident.setLastSeen(LocalDateTime.of(2026, 7, 23, 1, 5));
        return incident;
    }

    private SreAlertEvent alert(int index) {
        SreAlertEvent alert = new SreAlertEvent();
        alert.setId((long) index);
        alert.setSource("alertmanager");
        alert.setAlertName("CodeNestTargetDown");
        alert.setStatus("FIRING");
        alert.setSeverity("critical");
        alert.setService("code-nest");
        alert.setStartsAt("2026-07-23T01:00:00Z");
        alert.setUpdateTime(LocalDateTime.of(2026, 7, 23, 1, 5));
        return alert;
    }

    private SreIncidentEvidence evidence(int index) {
        SreIncidentEvidence evidence = new SreIncidentEvidence();
        evidence.setId((long) index);
        evidence.setIncidentId(11L);
        evidence.setSourceType("LOKI_SNAPSHOT");
        evidence.setSourceRef("application_errors");
        evidence.setQuery("q".repeat(600));
        evidence.setSnapshotJson("""
                {
                  "status": "success",
                  "rawPayload": "must-not-leak",
                  "password": "database-password",
                  "message": "request failed Authorization: Bearer abcdefghijklmnop",
                  "detail": "%s"
                }
                """.formatted("d".repeat(1_100)));
        evidence.setCapturedAt(LocalDateTime.of(2026, 7, 23, 1, 6));
        return evidence;
    }
}
