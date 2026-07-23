package com.xiaou.sre.service;

import com.xiaou.sre.client.SreLokiClient;
import com.xiaou.sre.client.SreLokiEvidence;
import com.xiaou.sre.client.SreLokiQueryCatalog;
import com.xiaou.sre.client.SreLokiQueryResult;
import com.xiaou.sre.config.SreLokiProperties;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.service.impl.SreLokiEvidenceCollectorImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreLokiEvidenceCollectorImplTest {

    @Mock
    private SreLokiClient lokiClient;

    @Test
    void successfulQueryProducesBoundedEvidence() {
        SreLokiProperties properties = enabledProperties();
        SreLokiQueryCatalog catalog = new SreLokiQueryCatalog();
        SreLokiEvidenceCollectorImpl collector = new SreLokiEvidenceCollectorImpl(
                properties, lokiClient, catalog);
        SreLokiQueryResult result = new SreLokiQueryResult();
        result.setResultType("streams");
        result.setResults(List.of(Map.of("stream", Map.of("job", "code-nest"))));
        when(lokiClient.query(any(), any(), any())).thenReturn(result);

        SreLokiEvidence evidence = collector.collect(event(), alert("CodeNestTargetDown"), incident());

        assertThat(evidence.getSourceType()).isEqualTo(SreLokiEvidenceCollector.LOKI_SOURCE_TYPE);
        assertThat(evidence.getSourceRef()).isEqualTo("application_errors");
        assertThat(evidence.getQuery()).contains("job=\"code-nest\"");
        assertThat(evidence.getSnapshotJson()).contains("success", "application_errors");
        verify(lokiClient).query(any(), any(), any());
    }

    @Test
    void failedQueryProducesUnavailableEvidenceWithoutLeakingMessage() {
        SreLokiProperties properties = enabledProperties();
        SreLokiEvidenceCollectorImpl collector = new SreLokiEvidenceCollectorImpl(
                properties, lokiClient, new SreLokiQueryCatalog());
        doThrow(new RuntimeException("secret endpoint details"))
                .when(lokiClient).query(any(), any(), any());

        SreLokiEvidence evidence = collector.collect(event(), alert("UnknownAlert"), incident());

        assertThat(evidence.getSourceType()).isEqualTo(SreLokiEvidenceCollector.UNAVAILABLE_SOURCE_TYPE);
        assertThat(evidence.getSnapshotJson()).contains("RuntimeException");
        assertThat(evidence.getSnapshotJson()).doesNotContain("secret endpoint details");
    }

    private SreLokiProperties enabledProperties() {
        SreLokiProperties properties = new SreLokiProperties();
        properties.setEnabled(true);
        return properties;
    }

    private SreOutboxEvent event() {
        SreOutboxEvent event = new SreOutboxEvent();
        event.setId(100L);
        return event;
    }

    private SreAlertEvent alert(String alertName) {
        SreAlertEvent alert = new SreAlertEvent();
        alert.setId(21L);
        alert.setAlertName(alertName);
        return alert;
    }

    private SreIncident incident() {
        SreIncident incident = new SreIncident();
        incident.setId(11L);
        return incident;
    }
}
