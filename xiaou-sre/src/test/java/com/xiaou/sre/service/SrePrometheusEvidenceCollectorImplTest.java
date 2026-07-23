package com.xiaou.sre.service;

import com.xiaou.sre.client.SrePrometheusClient;
import com.xiaou.sre.client.SrePrometheusEvidence;
import com.xiaou.sre.client.SrePrometheusQueryCatalog;
import com.xiaou.sre.client.SrePrometheusQueryResult;
import com.xiaou.sre.config.SrePrometheusProperties;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.service.impl.SrePrometheusEvidenceCollectorImpl;
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
class SrePrometheusEvidenceCollectorImplTest {

    @Mock
    private SrePrometheusClient prometheusClient;

    @Test
    void successfulQueryProducesBoundedEvidence() {
        SrePrometheusProperties properties = enabledProperties();
        SrePrometheusQueryCatalog catalog = new SrePrometheusQueryCatalog();
        SrePrometheusEvidenceCollectorImpl collector = new SrePrometheusEvidenceCollectorImpl(
                properties, prometheusClient, catalog);
        SrePrometheusQueryResult result = new SrePrometheusQueryResult();
        result.setResultType("vector");
        result.setResults(List.of(Map.of("metric", Map.of("instance", "app:9999"))));
        when(prometheusClient.query(any())).thenReturn(result);

        SrePrometheusEvidence evidence = collector.collect(event(), alert("CodeNestTargetDown"), incident());

        assertThat(evidence.getSourceType()).isEqualTo(SrePrometheusEvidenceCollector.PROMETHEUS_SOURCE_TYPE);
        assertThat(evidence.getSourceRef()).isEqualTo("target_up");
        assertThat(evidence.getQuery()).isEqualTo("up{job=\"code-nest\"}");
        assertThat(evidence.getSnapshotJson()).contains("success", "app:9999");
        verify(prometheusClient).query(catalog.find("CodeNestTargetDown"));
    }

    @Test
    void failedQueryProducesUnavailableEvidenceWithoutLeakingMessage() {
        SrePrometheusProperties properties = enabledProperties();
        SrePrometheusEvidenceCollectorImpl collector = new SrePrometheusEvidenceCollectorImpl(
                properties, prometheusClient, new SrePrometheusQueryCatalog());
        doThrow(new RuntimeException("secret endpoint details"))
                .when(prometheusClient).query(any());

        SrePrometheusEvidence evidence = collector.collect(event(), alert("UnknownAlert"), incident());

        assertThat(evidence.getSourceType()).isEqualTo(SrePrometheusEvidenceCollector.UNAVAILABLE_SOURCE_TYPE);
        assertThat(evidence.getSnapshotJson()).contains("RuntimeException");
        assertThat(evidence.getSnapshotJson()).doesNotContain("secret endpoint details");
    }

    private SrePrometheusProperties enabledProperties() {
        SrePrometheusProperties properties = new SrePrometheusProperties();
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
