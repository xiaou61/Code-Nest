package com.xiaou.sre.service.impl;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.sre.client.SreLokiClient;
import com.xiaou.sre.client.SreLokiEvidence;
import com.xiaou.sre.client.SreLokiQueryCatalog;
import com.xiaou.sre.client.SreLokiQueryResult;
import com.xiaou.sre.config.SreLokiProperties;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.service.SreLokiEvidenceCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loki 只读证据采集实现。
 *
 * <p>查询失败会转换为受限的不可用证据，不抛回 Outbox 处理器，保证告警快照、
 * Prometheus 证据和 QQ 告警链路不被日志系统阻塞。</p>
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SreLokiEvidenceCollectorImpl implements SreLokiEvidenceCollector {

    private static final int MAX_SNAPSHOT_LENGTH = 1_000_000;
    private static final Duration ALERT_CONTEXT_PADDING = Duration.ofMinutes(5);

    private final SreLokiProperties properties;
    private final SreLokiClient lokiClient;
    private final SreLokiQueryCatalog queryCatalog;

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public SreLokiEvidence collect(SreOutboxEvent event,
                                   SreAlertEvent alertEvent,
                                   SreIncident incident) {
        SreLokiQueryCatalog.QuerySpec querySpec = queryCatalog.find(alertEvent.getAlertName());
        Window window = queryWindow(alertEvent);
        try {
            SreLokiQueryResult queryResult = lokiClient.query(querySpec, window.start(), window.end());
            return evidence(
                    SreLokiEvidenceCollector.LOKI_SOURCE_TYPE,
                    querySpec.sourceRef(),
                    querySpec.logQl(),
                    successSnapshot(event, alertEvent, incident, querySpec, window, queryResult)
            );
        } catch (RuntimeException exception) {
            log.warn("Loki 证据查询不可用: eventId={}, alertName={}, reason={}",
                    event.getId(), alertEvent.getAlertName(), exception.getClass().getSimpleName());
            return evidence(
                    SreLokiEvidenceCollector.UNAVAILABLE_SOURCE_TYPE,
                    querySpec.sourceRef(),
                    querySpec.logQl(),
                    unavailableSnapshot(event, alertEvent, querySpec, window, exception)
            );
        }
    }

    private Window queryWindow(SreAlertEvent alertEvent) {
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofMinutes(properties.normalizedLookbackMinutes()));
        if (StringUtils.hasText(alertEvent.getStartsAt())) {
            try {
                Instant alertStart = Instant.parse(alertEvent.getStartsAt());
                Instant contextualStart = alertStart.minus(ALERT_CONTEXT_PADDING);
                if (contextualStart.isBefore(end)
                        && contextualStart.isAfter(end.minus(Duration.ofMinutes(properties.normalizedMaxRangeMinutes())))) {
                    start = contextualStart;
                }
            } catch (RuntimeException ignored) {
                // 入库时已经校验过时间；证据采集仍以安全的默认窗口继续。
            }
        }
        return new Window(start, end);
    }

    private String successSnapshot(SreOutboxEvent event,
                                   SreAlertEvent alertEvent,
                                   SreIncident incident,
                                   SreLokiQueryCatalog.QuerySpec querySpec,
                                   Window window,
                                   SreLokiQueryResult queryResult) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", "success");
        snapshot.put("capturedAt", LocalDateTime.now().toString());
        snapshot.put("outboxEventId", event.getId());
        snapshot.put("incidentId", incident.getId());
        snapshot.put("alertName", alertEvent.getAlertName());
        snapshot.put("sourceRef", querySpec.sourceRef());
        snapshot.put("windowStart", window.start().toString());
        snapshot.put("windowEnd", window.end().toString());
        snapshot.put("resultType", queryResult.getResultType());
        snapshot.put("results", queryResult.getResults());
        snapshot.put("truncated", queryResult.isTruncated());
        return boundedJson(snapshot);
    }

    private String unavailableSnapshot(SreOutboxEvent event,
                                       SreAlertEvent alertEvent,
                                       SreLokiQueryCatalog.QuerySpec querySpec,
                                       Window window,
                                       RuntimeException exception) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", "unavailable");
        snapshot.put("capturedAt", LocalDateTime.now().toString());
        snapshot.put("outboxEventId", event.getId());
        snapshot.put("alertName", alertEvent.getAlertName());
        snapshot.put("sourceRef", querySpec.sourceRef());
        snapshot.put("windowStart", window.start().toString());
        snapshot.put("windowEnd", window.end().toString());
        snapshot.put("reason", exception.getClass().getSimpleName());
        return boundedJson(snapshot);
    }

    private SreLokiEvidence evidence(String sourceType,
                                     String sourceRef,
                                     String query,
                                     String snapshotJson) {
        SreLokiEvidence evidence = new SreLokiEvidence();
        evidence.setSourceType(sourceType);
        evidence.setSourceRef(sourceRef);
        evidence.setQuery(query);
        evidence.setSnapshotJson(snapshotJson);
        return evidence;
    }

    private String boundedJson(Map<String, Object> snapshot) {
        String json = JsonUtils.toJsonString(snapshot);
        if (json == null || json.length() > MAX_SNAPSHOT_LENGTH) {
            throw new IllegalStateException("Loki 证据快照超过大小限制");
        }
        return json;
    }

    private record Window(Instant start, Instant end) {
    }
}
