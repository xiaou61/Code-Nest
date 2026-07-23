package com.xiaou.sre.service.impl;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.sre.client.SrePrometheusClient;
import com.xiaou.sre.client.SrePrometheusEvidence;
import com.xiaou.sre.client.SrePrometheusQueryCatalog;
import com.xiaou.sre.client.SrePrometheusQueryResult;
import com.xiaou.sre.config.SrePrometheusProperties;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.service.SrePrometheusEvidenceCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prometheus 只读证据采集实现。
 *
 * <p>查询失败会转换为受限的不可用证据，不抛回 Outbox 处理器，保证本地告警快照
 * 和事故状态不被外部监控查询阻塞。</p>
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SrePrometheusEvidenceCollectorImpl implements SrePrometheusEvidenceCollector {

    private static final int MAX_SNAPSHOT_LENGTH = 1_000_000;

    private final SrePrometheusProperties properties;
    private final SrePrometheusClient prometheusClient;
    private final SrePrometheusQueryCatalog queryCatalog;

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public SrePrometheusEvidence collect(SreOutboxEvent event,
                                         SreAlertEvent alertEvent,
                                         SreIncident incident) {
        SrePrometheusQueryCatalog.QuerySpec querySpec = queryCatalog.find(alertEvent.getAlertName());
        try {
            SrePrometheusQueryResult queryResult = prometheusClient.query(querySpec);
            return evidence(
                    SrePrometheusEvidenceCollector.PROMETHEUS_SOURCE_TYPE,
                    querySpec.sourceRef(),
                    querySpec.promQl(),
                    successSnapshot(event, alertEvent, incident, queryResult)
            );
        } catch (RuntimeException exception) {
            log.warn("Prometheus 证据查询不可用: eventId={}, alertName={}, reason={}",
                    event.getId(), alertEvent.getAlertName(), exception.getClass().getSimpleName());
            return evidence(
                    SrePrometheusEvidenceCollector.UNAVAILABLE_SOURCE_TYPE,
                    querySpec.sourceRef(),
                    querySpec.promQl(),
                    unavailableSnapshot(event, alertEvent, exception)
            );
        }
    }

    private String successSnapshot(SreOutboxEvent event,
                                   SreAlertEvent alertEvent,
                                   SreIncident incident,
                                   SrePrometheusQueryResult queryResult) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", "success");
        snapshot.put("capturedAt", LocalDateTime.now().toString());
        snapshot.put("outboxEventId", event.getId());
        snapshot.put("incidentId", incident.getId());
        snapshot.put("alertName", alertEvent.getAlertName());
        snapshot.put("resultType", queryResult.getResultType());
        snapshot.put("results", queryResult.getResults());
        snapshot.put("truncated", queryResult.isTruncated());
        return boundedJson(snapshot);
    }

    private String unavailableSnapshot(SreOutboxEvent event,
                                       SreAlertEvent alertEvent,
                                       RuntimeException exception) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", "unavailable");
        snapshot.put("capturedAt", LocalDateTime.now().toString());
        snapshot.put("outboxEventId", event.getId());
        snapshot.put("alertName", alertEvent.getAlertName());
        snapshot.put("reason", exception.getClass().getSimpleName());
        return boundedJson(snapshot);
    }

    private SrePrometheusEvidence evidence(String sourceType,
                                           String sourceRef,
                                           String query,
                                           String snapshotJson) {
        SrePrometheusEvidence evidence = new SrePrometheusEvidence();
        evidence.setSourceType(sourceType);
        evidence.setSourceRef(sourceRef);
        evidence.setQuery(query);
        evidence.setSnapshotJson(snapshotJson);
        return evidence;
    }

    private String boundedJson(Map<String, Object> snapshot) {
        String json = JsonUtils.toJsonString(snapshot);
        if (json == null || json.length() > MAX_SNAPSHOT_LENGTH) {
            throw new IllegalStateException("Prometheus 证据快照超过大小限制");
        }
        return json;
    }
}
