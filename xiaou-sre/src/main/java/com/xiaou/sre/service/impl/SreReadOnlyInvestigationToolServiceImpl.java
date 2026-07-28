package com.xiaou.sre.service.impl;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.sre.client.SreLokiClient;
import com.xiaou.sre.client.SreLokiQueryCatalog;
import com.xiaou.sre.client.SreLokiQueryResult;
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
import com.xiaou.sre.service.SreLokiEvidenceCollector;
import com.xiaou.sre.service.SrePrometheusEvidenceCollector;
import com.xiaou.sre.service.SreReadOnlyInvestigationToolService;
import com.xiaou.sre.service.SreReadOnlyToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 仅执行代码内固定 Prometheus/Loki 查询并把结果固化为可引用证据。
 *
 * @author xiaou
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SreReadOnlyInvestigationToolServiceImpl implements SreReadOnlyInvestigationToolService {

    private static final int MAX_SNAPSHOT_LENGTH = 1_000_000;

    private final SrePrometheusProperties prometheusProperties;
    private final SreLokiProperties lokiProperties;
    private final SrePrometheusClient prometheusClient;
    private final SreLokiClient lokiClient;
    private final SrePrometheusQueryCatalog prometheusCatalog;
    private final SreLokiQueryCatalog lokiCatalog;
    private final SreIncidentEvidenceMapper evidenceMapper;
    private final SreInvestigationRunMapper runMapper;
    private final SreMetricsRecorder metricsRecorder;

    @Override
    public List<String> availableToolKeys() {
        List<String> result = new ArrayList<>();
        if (prometheusProperties.isEnabled()) {
            result.addAll(prometheusCatalog.toolKeys());
        }
        if (lokiProperties.isEnabled()) {
            result.addAll(lokiCatalog.toolKeys());
        }
        return List.copyOf(result);
    }

    @Override
    public List<String> fallbackToolKeys(String alertName) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (prometheusProperties.isEnabled()) {
            result.add(prometheusCatalog.toolKeyForAlert(alertName));
        }
        if (lokiProperties.isEnabled()) {
            result.add(lokiCatalog.toolKeyForAlert(alertName));
        }
        if (prometheusProperties.isEnabled()) {
            result.add(prometheusCatalog.toolKeyForAlert(null));
        }
        if (lokiProperties.isEnabled()) {
            result.add(lokiCatalog.toolKeyForAlert(null));
        }
        result.remove(null);
        return result.stream().limit(3).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SreReadOnlyToolResult execute(Long incidentId, Long runId, String toolKey) {
        requirePositive(incidentId, "事故 ID 不合法");
        requirePositive(runId, "调查运行 ID 不合法");
        String normalizedToolKey = normalizeToolKey(toolKey);
        ToolSpec toolSpec = resolveAvailableTool(normalizedToolKey);
        SreInvestigationRun run = runMapper.selectByIncidentIdAndIdForUpdate(incidentId, runId);
        if (run == null || !"RUNNING".equals(run.getStatus())) {
            throw new IllegalArgumentException("调查运行不存在、已结束或不属于该事故");
        }

        String fingerprint = sha256(normalizedToolKey + "\n" + toolSpec.query());
        SreIncidentEvidence existing = evidenceMapper.selectByInvestigationRunAndFingerprint(runId, fingerprint);
        if (existing != null && existing.getId() != null) {
            recordToolMetric(normalizedToolKey, "duplicate", 0L);
            return new SreReadOnlyToolResult(normalizedToolKey, existing.getId(), false, "DUPLICATE");
        }

        long startNanos = System.nanoTime();
        String metricOutcome = "failed";
        try {
            EvidencePayload payload = toolSpec.prometheus()
                    ? queryPrometheus(incidentId, runId, normalizedToolKey, toolSpec)
                    : queryLoki(incidentId, runId, normalizedToolKey, toolSpec);
            PersistResult persisted = persist(
                    incidentId, runId, normalizedToolKey, fingerprint, toolSpec.query(), payload);
            metricOutcome = persisted.created()
                    ? (payload.available() ? "succeeded" : "unavailable")
                    : "duplicate";
            return new SreReadOnlyToolResult(
                    normalizedToolKey,
                    persisted.evidence().getId(),
                    persisted.created(),
                    persisted.created()
                            ? (payload.available() ? "AVAILABLE" : "UNAVAILABLE")
                            : "DUPLICATE"
            );
        } finally {
            recordToolMetric(normalizedToolKey, metricOutcome, System.nanoTime() - startNanos);
        }
    }

    private void recordToolMetric(String toolKey, String outcome, long durationNanos) {
        try {
            metricsRecorder.recordReadOnlyTool(toolKey, outcome, durationNanos);
        } catch (RuntimeException exception) {
            log.warn("SRE 只读调查工具指标记录失败: reason={}", exception.getClass().getSimpleName());
        }
    }

    private EvidencePayload queryPrometheus(Long incidentId,
                                            Long runId,
                                            String toolKey,
                                            ToolSpec toolSpec) {
        try {
            SrePrometheusQueryResult result = prometheusClient.query(toolSpec.prometheusSpec());
            if (result == null) {
                throw new IllegalStateException("Prometheus 返回为空");
            }
            Map<String, Object> snapshot = baseSnapshot(incidentId, runId, toolKey, "success");
            snapshot.put("resultType", result.getResultType());
            snapshot.put("results", result.getResults());
            snapshot.put("truncated", result.isTruncated());
            return new EvidencePayload(
                    SrePrometheusEvidenceCollector.PROMETHEUS_SOURCE_TYPE,
                    boundedJson(snapshot),
                    true
            );
        } catch (RuntimeException exception) {
            return unavailablePayload(
                    SrePrometheusEvidenceCollector.UNAVAILABLE_SOURCE_TYPE,
                    incidentId,
                    runId,
                    toolKey,
                    exception
            );
        }
    }

    private EvidencePayload queryLoki(Long incidentId,
                                      Long runId,
                                      String toolKey,
                                      ToolSpec toolSpec) {
        Instant end = Instant.now();
        Instant start = end.minusSeconds(lokiProperties.normalizedLookbackMinutes() * 60L);
        try {
            SreLokiQueryResult result = lokiClient.query(toolSpec.lokiSpec(), start, end);
            if (result == null) {
                throw new IllegalStateException("Loki 返回为空");
            }
            Map<String, Object> snapshot = baseSnapshot(incidentId, runId, toolKey, "success");
            snapshot.put("windowStart", start.toString());
            snapshot.put("windowEnd", end.toString());
            snapshot.put("resultType", result.getResultType());
            snapshot.put("results", result.getResults());
            snapshot.put("truncated", result.isTruncated());
            return new EvidencePayload(
                    SreLokiEvidenceCollector.LOKI_SOURCE_TYPE,
                    boundedJson(snapshot),
                    true
            );
        } catch (RuntimeException exception) {
            Map<String, Object> snapshot = baseSnapshot(incidentId, runId, toolKey, "unavailable");
            snapshot.put("windowStart", start.toString());
            snapshot.put("windowEnd", end.toString());
            snapshot.put("reason", exception.getClass().getSimpleName());
            return new EvidencePayload(
                    SreLokiEvidenceCollector.UNAVAILABLE_SOURCE_TYPE,
                    boundedJson(snapshot),
                    false
            );
        }
    }

    private EvidencePayload unavailablePayload(String sourceType,
                                               Long incidentId,
                                               Long runId,
                                               String toolKey,
                                               RuntimeException exception) {
        Map<String, Object> snapshot = baseSnapshot(incidentId, runId, toolKey, "unavailable");
        snapshot.put("reason", exception.getClass().getSimpleName());
        return new EvidencePayload(sourceType, boundedJson(snapshot), false);
    }

    private PersistResult persist(Long incidentId,
                                  Long runId,
                                  String toolKey,
                                  String fingerprint,
                                  String query,
                                  EvidencePayload payload) {
        SreIncidentEvidence evidence = new SreIncidentEvidence();
        evidence.setIncidentId(incidentId);
        evidence.setInvestigationRunId(runId);
        evidence.setQueryFingerprint(fingerprint);
        evidence.setSourceType(payload.sourceType());
        evidence.setSourceRef(toolKey);
        evidence.setQuery(query);
        evidence.setSnapshotJson(payload.snapshotJson());
        evidence.setCapturedAt(LocalDateTime.now());
        int inserted = evidenceMapper.insert(evidence);
        if (inserted == 1 && evidence.getId() != null) {
            return new PersistResult(evidence, true);
        }
        SreIncidentEvidence stored = evidenceMapper.selectByInvestigationRunAndFingerprint(runId, fingerprint);
        if (stored == null || stored.getId() == null) {
            throw new IllegalStateException("只读调查证据写入失败");
        }
        return new PersistResult(stored, false);
    }

    private Map<String, Object> baseSnapshot(Long incidentId,
                                             Long runId,
                                             String toolKey,
                                             String status) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", status);
        snapshot.put("capturedAt", LocalDateTime.now().toString());
        snapshot.put("incidentId", incidentId);
        snapshot.put("investigationRunId", runId);
        snapshot.put("toolKey", toolKey);
        return snapshot;
    }

    private ToolSpec resolveAvailableTool(String toolKey) {
        SrePrometheusQueryCatalog.QuerySpec prometheusSpec = prometheusCatalog.findByToolKey(toolKey);
        if (prometheusSpec != null && prometheusProperties.isEnabled()) {
            return new ToolSpec(prometheusSpec.promQl(), prometheusSpec, null);
        }
        SreLokiQueryCatalog.QuerySpec lokiSpec = lokiCatalog.findByToolKey(toolKey);
        if (lokiSpec != null && lokiProperties.isEnabled()) {
            return new ToolSpec(lokiSpec.logQl(), null, lokiSpec);
        }
        throw new IllegalArgumentException("工具不在当前可用的固定只读工具白名单中");
    }

    private String normalizeToolKey(String toolKey) {
        if (!StringUtils.hasText(toolKey)) {
            throw new IllegalArgumentException("固定只读工具 key 不能为空");
        }
        return toolKey.trim().toLowerCase(Locale.ROOT);
    }

    private String boundedJson(Map<String, Object> snapshot) {
        String json = JsonUtils.toJsonString(snapshot);
        if (!StringUtils.hasText(json) || json.length() > MAX_SNAPSHOT_LENGTH) {
            throw new IllegalStateException("只读调查证据快照为空或超过大小限制");
        }
        return json;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private record ToolSpec(
            String query,
            SrePrometheusQueryCatalog.QuerySpec prometheusSpec,
            SreLokiQueryCatalog.QuerySpec lokiSpec
    ) {
        private boolean prometheus() {
            return prometheusSpec != null;
        }
    }

    private record EvidencePayload(String sourceType, String snapshotJson, boolean available) {
    }

    private record PersistResult(SreIncidentEvidence evidence, boolean created) {
    }
}
