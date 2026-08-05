package com.xiaou.sre.controller.internal;

import com.xiaou.sre.dto.request.AlertmanagerAlert;
import com.xiaou.sre.dto.request.AlertmanagerWebhookRequest;
import com.xiaou.sre.dto.response.SreIngestionResult;
import com.xiaou.sre.service.SreAlertIngestionService;
import com.xiaou.sre.service.model.SreAlertBatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertmanagerWebhookControllerTest {

    @Mock
    private SreAlertIngestionService ingestionService;

    @Test
    void mapsTransportRequestToSourceNeutralBatch() {
        AlertmanagerAlert alert = new AlertmanagerAlert();
        alert.setStatus("firing");
        alert.setLabels(Map.of("alertname", "CodeNestTargetDown"));
        alert.setAnnotations(Map.of("summary", "target is down"));
        alert.setStartsAt("2026-07-20T00:00:00Z");
        alert.setEndsAt("0001-01-01T00:00:00Z");
        alert.setGeneratorUrl("http://prometheus/graph");
        alert.setFingerprint("fp-1");
        AlertmanagerWebhookRequest request = new AlertmanagerWebhookRequest();
        request.setAlerts(List.of(alert));
        when(ingestionService.ingest(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SreIngestionResult());

        new AlertmanagerWebhookController(ingestionService).receive(request);

        ArgumentCaptor<SreAlertBatch> captor = ArgumentCaptor.forClass(SreAlertBatch.class);
        verify(ingestionService).ingest(captor.capture());
        SreAlertBatch batch = captor.getValue();
        assertThat(batch.source()).isEqualTo("alertmanager");
        assertThat(batch.alerts()).hasSize(1);
        assertThat(batch.alerts().get(0).fingerprint()).isEqualTo("fp-1");
        assertThat(batch.alerts().get(0).rawPayload()).contains("generatorURL");
    }
}
