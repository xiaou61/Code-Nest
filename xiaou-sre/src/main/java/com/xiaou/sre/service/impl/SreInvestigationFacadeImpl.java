package com.xiaou.sre.service.impl;

import com.alibaba.fastjson2.JSON;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreIncidentEvidence;
import com.xiaou.sre.dto.response.SreInvestigationContext;
import com.xiaou.sre.mapper.SreAlertEventMapper;
import com.xiaou.sre.mapper.SreIncidentEvidenceMapper;
import com.xiaou.sre.mapper.SreIncidentMapper;
import com.xiaou.sre.service.SreInvestigationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 只读调查上下文实现。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreInvestigationFacadeImpl implements SreInvestigationFacade {

    private static final int MAX_ALERTS = 20;
    private static final int MAX_EVIDENCE = 20;
    private static final int LOAD_LIMIT = 21;
    private static final int MAX_SNAPSHOT_INPUT_LENGTH = 1_000_000;
    private static final int MAX_SNAPSHOT_DEPTH = 6;
    private static final int MAX_SNAPSHOT_NODES = 200;
    private static final int MAX_MAP_ENTRIES = 40;
    private static final int MAX_LIST_ENTRIES = 20;
    private static final int MAX_SNAPSHOT_STRING_LENGTH = 1_000;
    private static final int MAX_KEY_LENGTH = 128;

    private static final Set<String> OMITTED_KEYS = Set.of("rawpayload");
    private static final Pattern INLINE_SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(authorization|proxy[-_ ]?authorization|password|passwd|token|secret|"
                    + "api[-_ ]?key|cookie|credential)\\b\\s*[:=]\\s*([^\\s,;]+)");
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]{6,}");
    private static final Pattern STANDALONE_CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)\\b(?:sk-[A-Za-z0-9_-]{16,}|gh[pousr]_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16})\\b");

    private final SreIncidentMapper incidentMapper;
    private final SreAlertEventMapper alertEventMapper;
    private final SreIncidentEvidenceMapper evidenceMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<SreInvestigationContext> findByIncidentId(Long incidentId) {
        if (incidentId == null || incidentId <= 0) {
            return Optional.empty();
        }
        SreIncident incident = incidentMapper.selectById(incidentId);
        if (incident == null) {
            return Optional.empty();
        }

        List<SreAlertEvent> loadedAlerts = normalize(alertEventMapper.selectByIncidentId(incidentId, LOAD_LIMIT));
        List<SreIncidentEvidence> loadedEvidence = normalize(
                evidenceMapper.selectForInvestigation(incidentId, LOAD_LIMIT));

        List<SreInvestigationContext.Alert> alerts = loadedAlerts.stream()
                .limit(MAX_ALERTS)
                .map(this::toAlert)
                .toList();
        List<SreInvestigationContext.Evidence> evidence = loadedEvidence.stream()
                .limit(MAX_EVIDENCE)
                .map(this::toEvidence)
                .toList();

        return Optional.of(new SreInvestigationContext(
                toIncident(incident),
                alerts,
                evidence,
                loadedAlerts.size() > MAX_ALERTS,
                loadedEvidence.size() > MAX_EVIDENCE,
                LocalDateTime.now()
        ));
    }

    private SreInvestigationContext.Incident toIncident(SreIncident incident) {
        return new SreInvestigationContext.Incident(
                incident.getId(),
                boundedText(incident.getIncidentNo(), 64),
                boundedText(incident.getService(), 100),
                boundedText(incident.getAlertName(), 128),
                boundedText(incident.getSeverity(), 32),
                boundedText(incident.getState(), 32),
                boundedText(incident.getSummary(), 1_000),
                incident.getFirstSeen(),
                incident.getLastSeen(),
                incident.getResolvedAt()
        );
    }

    private SreInvestigationContext.Alert toAlert(SreAlertEvent alert) {
        LocalDateTime observedAt = alert.getUpdateTime() == null ? alert.getCreateTime() : alert.getUpdateTime();
        return new SreInvestigationContext.Alert(
                alert.getId(),
                boundedText(alert.getSource(), 64),
                boundedText(alert.getAlertName(), 128),
                boundedText(alert.getStatus(), 32),
                boundedText(alert.getSeverity(), 32),
                boundedText(alert.getService(), 100),
                boundedText(alert.getStartsAt(), 64),
                boundedText(alert.getEndsAt(), 64),
                observedAt
        );
    }

    private SreInvestigationContext.Evidence toEvidence(SreIncidentEvidence evidence) {
        SnapshotResult snapshot = sanitizeSnapshot(evidence.getSnapshotJson(), evidence.getSourceType());
        return new SreInvestigationContext.Evidence(
                evidence.getId(),
                boundedText(evidence.getSourceType(), 64),
                boundedText(evidence.getSourceRef(), 200),
                boundedText(evidence.getQuery(), 500),
                evidence.getCapturedAt(),
                snapshot.status(),
                snapshot.content(),
                snapshot.truncated()
        );
    }

    private SnapshotResult sanitizeSnapshot(String snapshotJson, String sourceType) {
        if (!StringUtils.hasText(snapshotJson) || snapshotJson.length() > MAX_SNAPSHOT_INPUT_LENGTH) {
            return invalidSnapshot();
        }

        Object parsed;
        try {
            parsed = JSON.parse(snapshotJson);
        } catch (RuntimeException ignored) {
            return invalidSnapshot();
        }
        if (parsed == null) {
            return invalidSnapshot();
        }

        String payloadStatus = statusFromPayload(parsed);
        SanitizationBudget budget = new SanitizationBudget(MAX_SNAPSHOT_NODES);
        Object sanitized = sanitizeValue(parsed, 0, budget);
        Map<String, Object> content = asRootMap(sanitized);
        String status = unavailable(sourceType, payloadStatus) ? "UNAVAILABLE" : "AVAILABLE";
        return new SnapshotResult(content, status, budget.truncated);
    }

    private SnapshotResult invalidSnapshot() {
        return new SnapshotResult(
                Map.of("reason", "evidence_payload_invalid"),
                "INVALID",
                true
        );
    }

    private Object sanitizeValue(Object value, int depth, SanitizationBudget budget) {
        if (!budget.consume()) {
            return "[TRUNCATED]";
        }
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof CharSequence sequence) {
            return sanitizeText(sequence.toString(), MAX_SNAPSHOT_STRING_LENGTH, budget);
        }
        if (depth >= MAX_SNAPSHOT_DEPTH && (value instanceof Map<?, ?> || value instanceof Collection<?>)) {
            budget.truncated = true;
            return "[TRUNCATED_DEPTH]";
        }
        if (value instanceof Map<?, ?> map) {
            return sanitizeMap(map, depth, budget);
        }
        if (value instanceof Collection<?> collection) {
            return sanitizeCollection(collection, depth, budget);
        }
        return sanitizeText(String.valueOf(value), MAX_SNAPSHOT_STRING_LENGTH, budget);
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> source, int depth, SanitizationBudget budget) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (result.size() >= MAX_MAP_ENTRIES) {
                budget.truncated = true;
                break;
            }
            if (entry.getKey() == null) {
                budget.truncated = true;
                continue;
            }
            String key = sanitizeText(String.valueOf(entry.getKey()), MAX_KEY_LENGTH, budget);
            String normalizedKey = normalizeKey(key);
            if (OMITTED_KEYS.contains(normalizedKey)) {
                budget.truncated = true;
                continue;
            }
            Object sanitizedValue = isSensitiveKey(normalizedKey)
                    ? redacted(budget)
                    : sanitizeValue(entry.getValue(), depth + 1, budget);
            result.put(key, sanitizedValue);
        }
        if (source.size() > MAX_MAP_ENTRIES) {
            budget.truncated = true;
        }
        return result;
    }

    private List<Object> sanitizeCollection(Collection<?> source, int depth, SanitizationBudget budget) {
        List<Object> result = new ArrayList<>(Math.min(source.size(), MAX_LIST_ENTRIES));
        int index = 0;
        for (Object item : source) {
            if (index >= MAX_LIST_ENTRIES) {
                budget.truncated = true;
                break;
            }
            result.add(sanitizeValue(item, depth + 1, budget));
            index++;
        }
        return result;
    }

    private Map<String, Object> asRootMap(Object sanitized) {
        if (sanitized instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (key != null) {
                    result.put(String.valueOf(key), value);
                }
            });
            return result;
        }
        return Map.of("value", sanitized == null ? "" : sanitized);
    }

    private String statusFromPayload(Object parsed) {
        if (!(parsed instanceof Map<?, ?> map)) {
            return "";
        }
        Object value = map.get("status");
        return value == null ? "" : String.valueOf(value);
    }

    private boolean unavailable(String sourceType, String payloadStatus) {
        return (sourceType != null && sourceType.toUpperCase(Locale.ROOT).contains("UNAVAILABLE"))
                || "unavailable".equalsIgnoreCase(payloadStatus);
    }

    private String boundedText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = redactInline(value);
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private String sanitizeText(String value, int maxLength, SanitizationBudget budget) {
        String sanitized = redactInline(value);
        if (!sanitized.equals(value)) {
            budget.truncated = true;
        }
        if (sanitized.length() > maxLength) {
            budget.truncated = true;
            return sanitized.substring(0, maxLength);
        }
        return sanitized;
    }

    private String redactInline(String value) {
        String redacted = BEARER_PATTERN.matcher(value).replaceAll("Bearer [REDACTED]");
        redacted = INLINE_SECRET_PATTERN.matcher(redacted).replaceAll("$1=[REDACTED]");
        return STANDALONE_CREDENTIAL_PATTERN.matcher(redacted).replaceAll("[REDACTED]");
    }

    private String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private boolean isSensitiveKey(String normalizedKey) {
        return normalizedKey.endsWith("authorization")
                || normalizedKey.endsWith("password")
                || normalizedKey.endsWith("passwd")
                || normalizedKey.endsWith("token")
                || normalizedKey.endsWith("secret")
                || normalizedKey.endsWith("apikey")
                || normalizedKey.endsWith("cookie")
                || normalizedKey.endsWith("credential")
                || normalizedKey.endsWith("privatekey");
    }

    private String redacted(SanitizationBudget budget) {
        budget.truncated = true;
        return "[REDACTED]";
    }

    private <T> List<T> normalize(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record SnapshotResult(Map<String, Object> content, String status, boolean truncated) {
    }

    private static final class SanitizationBudget {
        private int remaining;
        private boolean truncated;

        private SanitizationBudget(int remaining) {
            this.remaining = remaining;
        }

        private boolean consume() {
            if (remaining <= 0) {
                truncated = true;
                return false;
            }
            remaining--;
            return true;
        }
    }
}
