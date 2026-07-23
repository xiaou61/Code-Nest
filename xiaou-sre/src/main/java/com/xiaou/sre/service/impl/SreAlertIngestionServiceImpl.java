package com.xiaou.sre.service.impl;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreIncidentAlertRelation;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.dto.request.AlertmanagerAlert;
import com.xiaou.sre.dto.request.AlertmanagerWebhookRequest;
import com.xiaou.sre.dto.response.SreIngestionResult;
import com.xiaou.sre.mapper.SreAlertEventMapper;
import com.xiaou.sre.mapper.SreIncidentAlertRelationMapper;
import com.xiaou.sre.mapper.SreIncidentMapper;
import com.xiaou.sre.mapper.SreOutboxEventMapper;
import com.xiaou.sre.service.SreAlertIngestionService;
import com.xiaou.sre.service.SreValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Alertmanager 告警事件接收与事故聚合实现。
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SreAlertIngestionServiceImpl implements SreAlertIngestionService {

    private static final String SOURCE = "alertmanager";
    private static final String FIRING = "FIRING";
    private static final String RESOLVED = "RESOLVED";
    private static final String INCIDENT_OPEN = "OPEN";
    private static final String INCIDENT_RESOLVED = "RESOLVED";
    private static final String OUTBOX_PENDING = "PENDING";
    private static final String ACTIVE_EVIDENCE_EVENT = "EVIDENCE_COLLECTION_REQUESTED";
    /**
     * SRE 的 DATETIME 字段与项目现有 API 展示统一使用中国标准时间。
     * Alertmanager 的 startsAt/endsAt 是带时区的 Instant，不能直接当作 UTC 的
     * LocalDateTime 写入 MySQL，否则会比 create_time/用户看到的时间早八小时。
     */
    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MAX_MAP_ENTRIES = 100;
    private static final int MAX_MAP_VALUE_LENGTH = 2_000;
    private static final int MAX_PAYLOAD_LENGTH = 1_000_000;

    private final SreAlertEventMapper alertEventMapper;
    private final SreIncidentMapper incidentMapper;
    private final SreIncidentAlertRelationMapper relationMapper;
    private final SreOutboxEventMapper outboxEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SreIngestionResult ingest(AlertmanagerWebhookRequest request) {
        validateRequest(request);

        SreIngestionResult result = new SreIngestionResult();
        for (AlertmanagerAlert alert : request.getAlerts()) {
            result.incrementReceived();
            processAlert(alert, result);
        }
        return result;
    }

    private void processAlert(AlertmanagerAlert alert, SreIngestionResult result) {
        String status = normalizeStatus(alert.getStatus());
        String startsAt = normalizeTimestamp(alert.getStartsAt(), "startsAt");
        String endsAt = normalizeOptionalTimestamp(alert.getEndsAt(), "endsAt");
        Map<String, String> labels = safeMap(alert.getLabels(), "labels");
        Map<String, String> annotations = safeMap(alert.getAnnotations(), "annotations");

        String fingerprint = requireText(alert.getFingerprint(), "fingerprint");
        if (fingerprint.length() > 128) {
            throw new SreValidationException("fingerprint 不能超过128个字符");
        }
        String alertName = bounded(firstText(labels.get("alertname"), "unknown"), 200);
        String service = bounded(firstText(labels.get("service"), labels.get("job"), "unknown"), 100);
        String severity = bounded(firstText(labels.get("severity"), "warning"), 32).toUpperCase();
        String incidentKey = service + "|" + alertName;
        String labelsJson = toJson(labels, "labels");
        String annotationsJson = toJson(annotations, "annotations");
        String rawPayload = toJson(alert, "alert");

        SreAlertEvent existing = alertEventMapper.selectByFingerprintAndStartsAt(fingerprint, startsAt);
        if (existing == null) {
            SreAlertEvent event = buildEvent(alert, fingerprint, alertName, status, severity, service,
                    labelsJson, annotationsJson, startsAt, endsAt, rawPayload);
            alertEventMapper.insert(event);
            result.incrementCreatedEvents();

            SreIncident incident = findOrCreateIncident(incidentKey, alertName, service, severity,
                    summary(annotations, alertName), status, startsAt, endsAt, result);
            relationMapper.insert(new SreIncidentAlertRelation(incident.getId(), event.getId()));
            enqueueEvidence(event, incident);
            if (RESOLVED.equals(status)) {
                result.incrementResolvedIncidents();
            }
            return;
        }

        if (status.equalsIgnoreCase(existing.getStatus())) {
            result.incrementDuplicates();
            return;
        }

        existing.setStatus(status);
        existing.setAlertName(alertName);
        existing.setSeverity(severity);
        existing.setService(service);
        existing.setLabelsJson(labelsJson);
        existing.setAnnotationsJson(annotationsJson);
        existing.setEndsAt(endsAt);
        existing.setGeneratorUrl(bounded(alert.getGeneratorUrl(), 1000));
        existing.setRawPayload(rawPayload);
        alertEventMapper.updateStatusAndPayload(existing);
        result.incrementUpdatedEvents();

        SreIncidentAlertRelation relation = relationMapper.selectByAlertEventId(existing.getId());
        SreIncident incident = relation == null ? null : incidentMapper.selectById(relation.getIncidentId());
        if (incident == null) {
            incident = findOrCreateIncident(incidentKey, alertName, service, severity,
                    summary(annotations, alertName), status, startsAt, endsAt, result);
            if (relation == null) {
                relationMapper.insert(new SreIncidentAlertRelation(incident.getId(), existing.getId()));
            }
        }

        if (FIRING.equals(status)) {
            if (INCIDENT_RESOLVED.equalsIgnoreCase(incident.getState())) {
                incident.setState(INCIDENT_OPEN);
                incident.setResolvedAt(null);
                incident.setLastSeen(now());
                incidentMapper.reopen(incident);
            } else {
                incident.setLastSeen(now());
                incidentMapper.updateLastSeen(incident);
            }
        } else {
            int activeCount = relationMapper.countActiveByIncidentId(incident.getId());
            if (activeCount == 0 && !INCIDENT_RESOLVED.equalsIgnoreCase(incident.getState())) {
                incident.setState(INCIDENT_RESOLVED);
                incident.setResolvedAt(parseToLocalDateTime(endsAt));
                incident.setLastSeen(now());
                incidentMapper.markResolved(incident);
                result.incrementResolvedIncidents();
            } else {
                incident.setLastSeen(now());
                incidentMapper.updateLastSeen(incident);
            }
        }
        enqueueEvidence(existing, incident);
    }

    private SreIncident findOrCreateIncident(String incidentKey,
                                             String alertName,
                                             String service,
                                             String severity,
                                             String summary,
                                             String status,
                                             String startsAt,
                                             String endsAt,
                                             SreIngestionResult result) {
        SreIncident incident = FIRING.equals(status)
                ? incidentMapper.selectActiveByIncidentKey(incidentKey)
                : null;
        if (incident != null) {
            incident.setLastSeen(now());
            incidentMapper.updateLastSeen(incident);
            return incident;
        }

        incident = new SreIncident();
        incident.setIncidentNo(generateIncidentNo());
        incident.setIncidentKey(incidentKey);
        incident.setService(service);
        incident.setAlertName(alertName);
        incident.setSeverity(severity);
        incident.setState(FIRING.equals(status) ? INCIDENT_OPEN : INCIDENT_RESOLVED);
        incident.setSummary(bounded(summary, 500));
        incident.setFirstSeen(parseToLocalDateTime(startsAt));
        incident.setLastSeen(now());
        if (RESOLVED.equals(status)) {
            incident.setResolvedAt(parseToLocalDateTime(endsAt));
        }
        incidentMapper.insert(incident);
        result.incrementCreatedIncidents();
        return incident;
    }

    private void enqueueEvidence(SreAlertEvent event, SreIncident incident) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alertEventId", event.getId());
        payload.put("incidentId", incident.getId());
        payload.put("status", event.getStatus());

        SreOutboxEvent outboxEvent = new SreOutboxEvent();
        outboxEvent.setAggregateType("INCIDENT");
        outboxEvent.setAggregateId(incident.getId());
        outboxEvent.setEventType(ACTIVE_EVIDENCE_EVENT);
        outboxEvent.setPayloadJson(toJson(payload, "outbox payload"));
        outboxEvent.setState(OUTBOX_PENDING);
        outboxEvent.setAttempts(0);
        outboxEvent.setNextAttemptAt(now());
        outboxEventMapper.insert(outboxEvent);
    }

    private SreAlertEvent buildEvent(AlertmanagerAlert alert,
                                     String fingerprint,
                                     String alertName,
                                     String status,
                                     String severity,
                                     String service,
                                     String labelsJson,
                                     String annotationsJson,
                                     String startsAt,
                                     String endsAt,
                                     String rawPayload) {
        SreAlertEvent event = new SreAlertEvent();
        event.setSource(SOURCE);
        event.setFingerprint(fingerprint);
        event.setAlertName(alertName);
        event.setStatus(status);
        event.setSeverity(severity);
        event.setService(service);
        event.setLabelsJson(labelsJson);
        event.setAnnotationsJson(annotationsJson);
        event.setStartsAt(startsAt);
        event.setEndsAt(endsAt);
        event.setGeneratorUrl(bounded(alert.getGeneratorUrl(), 1000));
        event.setRawPayload(rawPayload);
        return event;
    }

    private void validateRequest(AlertmanagerWebhookRequest request) {
        if (request == null || request.getAlerts() == null || request.getAlerts().isEmpty()) {
            throw new SreValidationException("alerts 不能为空");
        }
        if (request.getAlerts().size() > 100) {
            throw new SreValidationException("单次 webhook 告警数量不能超过100");
        }
        if (request.getAlerts().stream().anyMatch(Objects::isNull)) {
            throw new SreValidationException("alerts 不能包含空元素");
        }
    }

    private Map<String, String> safeMap(Map<String, String> values, String field) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        if (values.size() > MAX_MAP_ENTRIES) {
            throw new SreValidationException(field + " 条目数量超限");
        }
        values.forEach((key, value) -> {
            if (!StringUtils.hasText(key) || key.length() > 128
                    || (value != null && value.length() > MAX_MAP_VALUE_LENGTH)) {
                throw new SreValidationException(field + " 包含非法或过长字段");
            }
        });
        return values;
    }

    private String toJson(Object value, String field) {
        String json = JsonUtils.toJsonString(value);
        if (json == null) {
            throw new SreValidationException(field + " 无法序列化");
        }
        if (json.length() > MAX_PAYLOAD_LENGTH) {
            throw new SreValidationException(field + " 内容过大");
        }
        return json;
    }

    private String normalizeStatus(String value) {
        if ("firing".equalsIgnoreCase(value)) {
            return FIRING;
        }
        if ("resolved".equalsIgnoreCase(value)) {
            return RESOLVED;
        }
        throw new SreValidationException("status 只支持 firing 或 resolved");
    }

    private String normalizeTimestamp(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new SreValidationException(field + " 不能为空");
        }
        try {
            return Instant.parse(value.trim()).toString();
        } catch (DateTimeParseException exception) {
            throw new SreValidationException(field + " 不是合法 ISO-8601 时间");
        }
    }

    private String normalizeOptionalTimestamp(String value, String field) {
        if (!StringUtils.hasText(value) || value.trim().startsWith("0001-01-01T00:00:00")) {
            return null;
        }
        return normalizeTimestamp(value, field);
    }

    private LocalDateTime parseToLocalDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return now();
        }
        return Instant.parse(value).atZone(APPLICATION_ZONE).toLocalDateTime();
    }

    private String summary(Map<String, String> annotations, String alertName) {
        return firstText(annotations.get("summary"), annotations.get("description"), alertName);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "unknown";
    }

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new SreValidationException(field + " 不能为空");
        }
        return value.trim();
    }

    private String bounded(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private String generateIncidentNo() {
        return "SRE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
