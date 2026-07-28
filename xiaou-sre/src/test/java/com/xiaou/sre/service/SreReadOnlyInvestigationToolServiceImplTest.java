package com.xiaou.sre.service;

import com.xiaou.sre.client.SreLokiClient;
import com.xiaou.sre.client.SreLokiQueryCatalog;
import com.xiaou.sre.client.SrePrometheusClient;
import com.xiaou.sre.client.SrePrometheusQueryCatalog;
import com.xiaou.sre.client.SrePrometheusQueryResult;
import com.xiaou.sre.config.SreLokiProperties;
import com.xiaou.sre.config.SrePrometheusProperties;
import com.xiaou.sre.domain.SreIncidentEvidence;
import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.mapper.SreIncidentEvidenceMapper;
import com.xiaou.sre.mapper.SreInvestigationRunMapper;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.service.impl.SreReadOnlyInvestigationToolServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SreReadOnlyInvestigationToolServiceImplTest {

    private final SrePrometheusProperties prometheusProperties = new SrePrometheusProperties();
    private final SreLokiProperties lokiProperties = new SreLokiProperties();
    private final SrePrometheusClient prometheusClient = mock(SrePrometheusClient.class);
    private final SreLokiClient lokiClient = mock(SreLokiClient.class);
    private final SreIncidentEvidenceMapper evidenceMapper = mock(SreIncidentEvidenceMapper.class);
    private final SreInvestigationRunMapper runMapper = mock(SreInvestigationRunMapper.class);
    private final SreMetricsRecorder metricsRecorder = mock(SreMetricsRecorder.class);
    private final SrePrometheusQueryCatalog prometheusCatalog = new SrePrometheusQueryCatalog();
    private final SreLokiQueryCatalog lokiCatalog = new SreLokiQueryCatalog();
    private SreReadOnlyInvestigationToolServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SreReadOnlyInvestigationToolServiceImpl(
                prometheusProperties,
                lokiProperties,
                prometheusClient,
                lokiClient,
                prometheusCatalog,
                lokiCatalog,
                evidenceMapper,
                runMapper,
                metricsRecorder
        );
        SreInvestigationRun run = new SreInvestigationRun();
        run.setId(91L);
        run.setIncidentId(11L);
        run.setStatus("RUNNING");
        when(runMapper.selectByIncidentIdAndIdForUpdate(11L, 91L)).thenReturn(run);
    }

    @Test
    void executesOnlyCatalogPrometheusQueryAndPersistsCitableEvidence() {
        prometheusProperties.setEnabled(true);
        SrePrometheusQueryResult queryResult = new SrePrometheusQueryResult();
        queryResult.setResultType("vector");
        queryResult.setResults(List.of());
        when(prometheusClient.query(prometheusCatalog.findByToolKey("prom_target_up")))
                .thenReturn(queryResult);
        when(evidenceMapper.insert(any())).thenAnswer(invocation -> {
            SreIncidentEvidence evidence = invocation.getArgument(0);
            evidence.setId(301L);
            return 1;
        });

        SreReadOnlyToolResult result = service.execute(11L, 91L, "prom_target_up");

        assertThat(result.toolKey()).isEqualTo("prom_target_up");
        assertThat(result.evidenceId()).isEqualTo(301L);
        assertThat(result.created()).isTrue();
        assertThat(result.status()).isEqualTo("AVAILABLE");
        ArgumentCaptor<SreIncidentEvidence> captor = ArgumentCaptor.forClass(SreIncidentEvidence.class);
        verify(evidenceMapper).insert(captor.capture());
        assertThat(captor.getValue().getIncidentId()).isEqualTo(11L);
        assertThat(captor.getValue().getInvestigationRunId()).isEqualTo(91L);
        assertThat(captor.getValue().getOutboxEventId()).isNull();
        assertThat(captor.getValue().getQueryFingerprint()).hasSize(64);
        assertThat(captor.getValue().getSourceRef()).isEqualTo("prom_target_up");
        assertThat(captor.getValue().getSnapshotJson()).contains("\"toolKey\":\"prom_target_up\"");
        verify(metricsRecorder).recordReadOnlyTool(
                org.mockito.ArgumentMatchers.eq("prom_target_up"),
                org.mockito.ArgumentMatchers.eq("succeeded"),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void repeatedToolReturnsExistingEvidenceWithoutAnotherQuery() {
        prometheusProperties.setEnabled(true);
        SreIncidentEvidence existing = new SreIncidentEvidence();
        existing.setId(302L);
        existing.setSourceType("PROMETHEUS_INSTANT");
        when(evidenceMapper.selectByInvestigationRunAndFingerprint(
                org.mockito.ArgumentMatchers.eq(91L), any())).thenReturn(existing);

        SreReadOnlyToolResult result = service.execute(11L, 91L, "prom_target_up");

        assertThat(result.evidenceId()).isEqualTo(302L);
        assertThat(result.created()).isFalse();
        assertThat(result.status()).isEqualTo("DUPLICATE");
        verify(prometheusClient, never()).query(any());
        verify(evidenceMapper, never()).insert(any());
    }

    @Test
    void duplicateDetectedDuringInsertReturnsExistingEvidenceAndStopsTheRound() {
        prometheusProperties.setEnabled(true);
        SrePrometheusQueryResult queryResult = new SrePrometheusQueryResult();
        queryResult.setResultType("vector");
        queryResult.setResults(List.of());
        when(prometheusClient.query(prometheusCatalog.findByToolKey("prom_target_up")))
                .thenReturn(queryResult);
        SreIncidentEvidence existing = new SreIncidentEvidence();
        existing.setId(303L);
        when(evidenceMapper.selectByInvestigationRunAndFingerprint(
                org.mockito.ArgumentMatchers.eq(91L), any()))
                .thenReturn(null, existing);
        when(evidenceMapper.insert(any())).thenReturn(0);

        SreReadOnlyToolResult result = service.execute(11L, 91L, "prom_target_up");

        assertThat(result.evidenceId()).isEqualTo(303L);
        assertThat(result.created()).isFalse();
        assertThat(result.status()).isEqualTo("DUPLICATE");
        verify(metricsRecorder).recordReadOnlyTool(
                org.mockito.ArgumentMatchers.eq("prom_target_up"),
                org.mockito.ArgumentMatchers.eq("duplicate"),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void metricsFailureCannotBreakSuccessfulEvidenceCollection() {
        prometheusProperties.setEnabled(true);
        SrePrometheusQueryResult queryResult = new SrePrometheusQueryResult();
        queryResult.setResultType("vector");
        queryResult.setResults(List.of());
        when(prometheusClient.query(prometheusCatalog.findByToolKey("prom_target_up")))
                .thenReturn(queryResult);
        when(evidenceMapper.insert(any())).thenAnswer(invocation -> {
            SreIncidentEvidence evidence = invocation.getArgument(0);
            evidence.setId(304L);
            return 1;
        });
        org.mockito.Mockito.doThrow(new IllegalStateException("registry unavailable"))
                .when(metricsRecorder).recordReadOnlyTool(
                        any(), any(), org.mockito.ArgumentMatchers.anyLong());

        SreReadOnlyToolResult result = service.execute(11L, 91L, "prom_target_up");

        assertThat(result.evidenceId()).isEqualTo(304L);
        assertThat(result.created()).isTrue();
        assertThat(result.status()).isEqualTo("AVAILABLE");
    }

    @Test
    void unknownToolIsRejectedBeforeAnyExternalCall() {
        prometheusProperties.setEnabled(true);
        lokiProperties.setEnabled(true);

        assertThatThrownBy(() -> service.execute(11L, 91L, "shell_restart"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("固定只读工具");

        verify(prometheusClient, never()).query(any());
        verify(lokiClient, never()).query(any(), any(), any());
        verify(evidenceMapper, never()).insert(any());
    }

    @Test
    void availableToolsOnlyExposeEnabledReadOnlyProviders() {
        prometheusProperties.setEnabled(true);
        lokiProperties.setEnabled(false);

        assertThat(service.availableToolKeys())
                .contains("prom_target_up", "prom_http_5xx_rate")
                .allMatch(key -> key.startsWith("prom_"));
        assertThat(service.fallbackToolKeys("CodeNestTargetDown"))
                .contains("prom_target_up")
                .allMatch(key -> key.startsWith("prom_"));
    }
}
