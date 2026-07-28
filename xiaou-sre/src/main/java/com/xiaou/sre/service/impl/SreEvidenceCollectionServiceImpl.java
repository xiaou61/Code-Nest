package com.xiaou.sre.service.impl;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreIncidentEvidence;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.client.SreLokiEvidence;
import com.xiaou.sre.client.SrePrometheusEvidence;
import com.xiaou.sre.mapper.SreAlertEventMapper;
import com.xiaou.sre.mapper.SreIncidentEvidenceMapper;
import com.xiaou.sre.mapper.SreIncidentMapper;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.service.SreEvidenceCollectionService;
import com.xiaou.sre.service.SreLokiEvidenceCollector;
import com.xiaou.sre.service.SrePrometheusEvidenceCollector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第一版只读证据采集：把告警事件、事故当前状态和可选 Prometheus 指标固化成可回溯快照。
 *
 * <p>Prometheus 和 Loki 通过固定查询白名单接入；服务器命令仍未接入。</p>
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreEvidenceCollectionServiceImpl implements SreEvidenceCollectionService {

    private static final String EVIDENCE_EVENT_TYPE = "EVIDENCE_COLLECTION_REQUESTED";
    private static final String SOURCE_TYPE = "ALERT_SNAPSHOT";
    private static final int MAX_SNAPSHOT_LENGTH = 1_000_000;

    private final SreAlertEventMapper alertEventMapper;
    private final SreIncidentMapper incidentMapper;
    private final SreIncidentEvidenceMapper evidenceMapper;
    private final SrePrometheusEvidenceCollector prometheusEvidenceCollector;
    private final SreLokiEvidenceCollector lokiEvidenceCollector;
    private final SreMetricsRecorder metricsRecorder;

    @Override
    public void collect(SreOutboxEvent event) {
        long startNanos = System.nanoTime();
        String outcome = "failed";
        try {
            outcome = collectInternal(event);
        } finally {
            recordCollectionMetric(outcome, System.nanoTime() - startNanos);
        }
    }

    private String collectInternal(SreOutboxEvent event) {
        if (!EVIDENCE_EVENT_TYPE.equals(event.getEventType())) {
            throw new IllegalArgumentException("不支持的 SRE Outbox 事件类型");
        }
        if (event.getId() == null) {
            throw new IllegalArgumentException("Outbox 事件 ID 不能为空");
        }
        boolean prometheusEnabled = prometheusEvidenceCollector != null
                && prometheusEvidenceCollector.isEnabled();
        boolean lokiEnabled = lokiEvidenceCollector != null && lokiEvidenceCollector.isEnabled();
        boolean externalEvidenceEnabled = prometheusEnabled || lokiEnabled;
        boolean snapshotExists = evidenceMapper.selectByOutboxEventAndSource(event.getId(), SOURCE_TYPE) != null;
        if (snapshotExists && !externalEvidenceEnabled) {
            return "duplicate";
        }
        if (!JsonUtils.isValidJson(event.getPayloadJson())) {
            throw new IllegalArgumentException("证据任务载荷不是合法 JSON");
        }
        Map<String, Object> payload = JsonUtils.parseMap(event.getPayloadJson());
        Long alertEventId = numberAsLong(payload == null ? null : payload.get("alertEventId"));
        Long incidentId = numberAsLong(payload == null ? null : payload.get("incidentId"));
        if (alertEventId == null || incidentId == null || incidentId <= 0 || alertEventId <= 0) {
            throw new IllegalArgumentException("证据任务载荷缺少合法的告警或事故 ID");
        }
        if (event.getAggregateId() != null && !event.getAggregateId().equals(incidentId)) {
            throw new IllegalArgumentException("证据任务载荷与 Outbox 聚合 ID 不一致");
        }

        SreAlertEvent alertEvent = alertEventMapper.selectById(alertEventId);
        SreIncident incident = incidentMapper.selectById(incidentId);
        if (alertEvent == null || incident == null) {
            throw new IllegalStateException("证据任务关联的告警或事故不存在");
        }

        if (evidenceMapper.selectByOutboxEventAndSource(event.getId(), SOURCE_TYPE) == null) {
            LocalDateTime capturedAt = LocalDateTime.now();
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("capturedAt", capturedAt.toString());
            snapshot.put("incident", incidentSnapshot(incident));
            snapshot.put("alert", alertSnapshot(alertEvent));
            String snapshotJson = JsonUtils.toJsonString(snapshot);
            if (!StringUtils.hasText(snapshotJson) || snapshotJson.length() > MAX_SNAPSHOT_LENGTH) {
                throw new IllegalStateException("证据快照为空或超过大小限制");
            }

            SreIncidentEvidence evidence = new SreIncidentEvidence();
            evidence.setIncidentId(incidentId);
            evidence.setOutboxEventId(event.getId());
            evidence.setSourceType(SOURCE_TYPE);
            evidence.setSourceRef(String.valueOf(alertEventId));
            evidence.setQuery("sre_alert_event.id=" + alertEventId);
            evidence.setSnapshotJson(snapshotJson);
            evidence.setCapturedAt(capturedAt);
            evidenceMapper.insert(evidence);
        }

        if (prometheusEnabled
                && evidenceMapper.selectByOutboxEventAndSource(
                event.getId(), SrePrometheusEvidenceCollector.PROMETHEUS_SOURCE_TYPE) == null
                && evidenceMapper.selectByOutboxEventAndSource(
                event.getId(), SrePrometheusEvidenceCollector.UNAVAILABLE_SOURCE_TYPE) == null) {
            SrePrometheusEvidence prometheusEvidence = prometheusEvidenceCollector.collect(event, alertEvent, incident);
            insertExternalEvidence(incidentId, event.getId(), prometheusEvidence);
        }

        if (lokiEnabled
                && evidenceMapper.selectByOutboxEventAndSource(
                event.getId(), SreLokiEvidenceCollector.LOKI_SOURCE_TYPE) == null
                && evidenceMapper.selectByOutboxEventAndSource(
                event.getId(), SreLokiEvidenceCollector.UNAVAILABLE_SOURCE_TYPE) == null) {
            SreLokiEvidence lokiEvidence = lokiEvidenceCollector.collect(event, alertEvent, incident);
            insertExternalEvidence(incidentId, event.getId(), lokiEvidence);
        }
        return "succeeded";
    }

    private void recordCollectionMetric(String outcome, long durationNanos) {
        try {
            metricsRecorder.recordEvidenceCollection(outcome, durationNanos);
        } catch (RuntimeException ignored) {
            // 指标不可用不能改变 Outbox 证据事务结果。
        }
    }

    private void insertExternalEvidence(Long incidentId,
                                       Long outboxEventId,
                                       SrePrometheusEvidence prometheusEvidence) {
        if (prometheusEvidence == null) {
            return;
        }
        SreIncidentEvidence evidence = new SreIncidentEvidence();
        evidence.setIncidentId(incidentId);
        evidence.setOutboxEventId(outboxEventId);
        evidence.setSourceType(prometheusEvidence.getSourceType());
        evidence.setSourceRef(prometheusEvidence.getSourceRef());
        evidence.setQuery(prometheusEvidence.getQuery());
        evidence.setSnapshotJson(prometheusEvidence.getSnapshotJson());
        evidence.setCapturedAt(LocalDateTime.now());
        evidenceMapper.insert(evidence);
    }

    private void insertExternalEvidence(Long incidentId,
                                       Long outboxEventId,
                                       SreLokiEvidence lokiEvidence) {
        if (lokiEvidence == null) {
            return;
        }
        SreIncidentEvidence evidence = new SreIncidentEvidence();
        evidence.setIncidentId(incidentId);
        evidence.setOutboxEventId(outboxEventId);
        evidence.setSourceType(lokiEvidence.getSourceType());
        evidence.setSourceRef(lokiEvidence.getSourceRef());
        evidence.setQuery(lokiEvidence.getQuery());
        evidence.setSnapshotJson(lokiEvidence.getSnapshotJson());
        evidence.setCapturedAt(LocalDateTime.now());
        evidenceMapper.insert(evidence);
    }

    private Map<String, Object> incidentSnapshot(SreIncident incident) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", incident.getId());
        snapshot.put("incidentNo", incident.getIncidentNo());
        snapshot.put("incidentKey", incident.getIncidentKey());
        snapshot.put("service", incident.getService());
        snapshot.put("alertName", incident.getAlertName());
        snapshot.put("severity", incident.getSeverity());
        snapshot.put("state", incident.getState());
        snapshot.put("summary", incident.getSummary());
        snapshot.put("firstSeen", incident.getFirstSeen());
        snapshot.put("lastSeen", incident.getLastSeen());
        snapshot.put("resolvedAt", incident.getResolvedAt());
        return snapshot;
    }

    private Map<String, Object> alertSnapshot(SreAlertEvent alertEvent) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", alertEvent.getId());
        snapshot.put("source", alertEvent.getSource());
        snapshot.put("fingerprint", alertEvent.getFingerprint());
        snapshot.put("alertName", alertEvent.getAlertName());
        snapshot.put("status", alertEvent.getStatus());
        snapshot.put("severity", alertEvent.getSeverity());
        snapshot.put("service", alertEvent.getService());
        snapshot.put("labels", parseMapQuietly(alertEvent.getLabelsJson()));
        snapshot.put("annotations", parseMapQuietly(alertEvent.getAnnotationsJson()));
        snapshot.put("startsAt", alertEvent.getStartsAt());
        snapshot.put("endsAt", alertEvent.getEndsAt());
        snapshot.put("generatorUrl", alertEvent.getGeneratorUrl());
        return snapshot;
    }

    private Long numberAsLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.valueOf(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> parseMapQuietly(String json) {
        return JsonUtils.isValidJson(json) ? JsonUtils.parseMap(json) : null;
    }
}
