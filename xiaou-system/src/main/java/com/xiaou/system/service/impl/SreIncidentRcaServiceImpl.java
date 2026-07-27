package com.xiaou.system.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.prompt.sre.SreRcaPromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputValidator;
import com.xiaou.ai.structured.sre.SreRcaStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.ai.util.AiJsonResponseParser;
import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.domain.SreInvestigationStep;
import com.xiaou.sre.dto.response.SreInvestigationContext;
import com.xiaou.sre.service.SreInvestigationFacade;
import com.xiaou.sre.service.SreInvestigationRunService;
import com.xiaou.system.dto.SreRcaReport;
import com.xiaou.system.dto.SreRcaRunDetail;
import com.xiaou.system.dto.SreRcaRunSummary;
import com.xiaou.system.service.SreIncidentRcaService;
import com.xiaou.system.service.SreRcaTriggerSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 组合受限事故上下文与统一 AI 运行时生成 RCA 报告。
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SreIncidentRcaServiceImpl implements SreIncidentRcaService {

    private static final String SCENE = "sre.incident.rca";
    private static final int MAX_CONTEXT_LENGTH = 60_000;
    private static final int MAX_ALERTS = 10;
    private static final int MAX_EVIDENCE = 12;
    private static final int MAX_SNAPSHOT_EXCERPT = 3_000;
    private static final int MAX_OBSERVATIONS = 8;
    private static final int MAX_HYPOTHESES = 5;
    private static final int MAX_RECOMMENDATIONS = 5;
    private static final int MAX_LIMITATIONS = 10;
    private static final int MAX_SNAPSHOT_DEPTH = 6;
    private static final int MAX_SNAPSHOT_NODES = 200;
    private static final int MAX_SNAPSHOT_MAP_ENTRIES = 40;
    private static final int MAX_SNAPSHOT_LIST_ENTRIES = 20;
    private static final int MAX_SNAPSHOT_KEY_LENGTH = 128;

    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]{6,}");
    private static final Pattern INLINE_SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(authorization|password|passwd|token|secret|api[-_ ]?key|cookie|credential)"
                    + "\\b\\s*[:=]\\s*([^\\s,;]+)");
    private static final Pattern CONTROL_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    private final SreInvestigationFacade investigationFacade;
    private final AiExecutionSupport aiExecutionSupport;
    private final ObjectMapper objectMapper;
    private final SreInvestigationRunService investigationRunService;

    @Override
    public Optional<SreRcaReport> investigate(Long incidentId,
                                              SreRcaTriggerSource triggerSource,
                                              Long requestedBy) {
        if (incidentId == null || incidentId <= 0) {
            return Optional.empty();
        }
        Optional<SreInvestigationContext> contextOptional = investigationFacade.findByIncidentId(incidentId);
        if (contextOptional.isEmpty()) {
            return Optional.empty();
        }

        SreInvestigationContext context = contextOptional.get();
        SreInvestigationRun run = investigationRunService.start(
                incidentId,
                triggerSource == null ? SreRcaTriggerSource.SYSTEM.name() : triggerSource.name(),
                requestedBy,
                context.alerts().size(),
                context.evidence().size(),
                context.alertsTruncated() || context.evidenceTruncated()
        );
        try {
            investigationRunService.recordStep(
                    run.getId(), 1, "CONTEXT_LOADED", "SUCCEEDED", contextStepDetail(context));
        } catch (RuntimeException exception) {
            markRunFailed(run.getId(), exception);
            throw exception;
        }

        ModelContext modelContext;
        try {
            modelContext = buildModelContext(context);
        } catch (RuntimeException exception) {
            log.warn("SRE RCA 上下文构建失败，返回证据不足报告: incidentId={}, reason={}",
                    incidentId, exception.getClass().getSimpleName());
            SreRcaReport report = fallbackReport(context, references(context, Set.of()), true);
            try {
                investigationRunService.recordStep(
                        run.getId(), 2, "MODEL_CONTEXT_BUILT", "DEGRADED", "上下文构建失败，已进入确定性降级。"
                );
                investigationRunService.recordStep(
                        run.getId(), 3, "MODEL_ANALYSIS", "SKIPPED", "未调用模型。"
                );
                investigationRunService.recordStep(
                        run.getId(), 4, "REPORT_VALIDATED", "SUCCEEDED", "已生成只含事实的降级报告。"
                );
                persistCompletedRun(run.getId(), report);
            } catch (RuntimeException persistenceException) {
                markRunFailed(run.getId(), persistenceException);
                throw persistenceException;
            }
            return Optional.of(report);
        }
        try {
            investigationRunService.recordStep(
                    run.getId(), 2, "MODEL_CONTEXT_BUILT", "SUCCEEDED", modelContextStepDetail(modelContext));
        } catch (RuntimeException exception) {
            markRunFailed(run.getId(), exception);
            throw exception;
        }

        try {
            SreRcaReport report = aiExecutionSupport.chatWithFallback(
                    SCENE,
                    SreRcaPromptSpecs.INVESTIGATE,
                    Map.of("incidentContextJson", modelContext.json()),
                    response -> parseReport(response, context, modelContext),
                    () -> fallbackReport(context, references(context, modelContext.evidenceIds()),
                            modelContext.truncated())
            );
            if (report == null) {
                report = fallbackReport(context, references(context, modelContext.evidenceIds()),
                        modelContext.truncated());
            }

            String analysisStatus = "AI".equals(report.generationMode()) ? "SUCCEEDED" : "DEGRADED";
            String analysisDetail = "AI".equals(report.generationMode())
                    ? "模型返回了通过结构化契约校验的报告。"
                    : "模型不可用或输出无效，已使用确定性降级报告。";
            investigationRunService.recordStep(
                    run.getId(), 3, "MODEL_ANALYSIS", analysisStatus, analysisDetail);
            investigationRunService.recordStep(
                    run.getId(), 4, "REPORT_VALIDATED", "SUCCEEDED", "证据引用和只读风险边界校验完成。"
            );
            persistCompletedRun(run.getId(), report);

            log.info("SRE RCA 调查完成: incidentId={}, runId={}, mode={}, conclusionStatus={}, evidenceIds={}",
                    incidentId, run.getId(), report.generationMode(), report.conclusionStatus(),
                    modelContext.evidenceIds());
            return Optional.of(report);
        } catch (RuntimeException exception) {
            markRunFailed(run.getId(), exception);
            throw exception;
        }
    }

    @Override
    public List<SreRcaRunSummary> listRuns(Long incidentId, int limit) {
        return investigationRunService.listByIncidentId(incidentId, limit).stream()
                .map(this::toRunSummary)
                .toList();
    }

    @Override
    public Optional<SreRcaRunDetail> getRun(Long incidentId, Long runId) {
        return investigationRunService.findByIncidentIdAndRunId(incidentId, runId)
                .map(run -> new SreRcaRunDetail(
                        toRunSummary(run),
                        investigationRunService.listSteps(run.getId()).stream()
                                .map(this::toRunStep)
                                .toList(),
                        readPersistedReport(run)
                ));
    }

    private ModelContext buildModelContext(SreInvestigationContext context) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("dataClassification", "UNTRUSTED_EVIDENCE_ONLY");
        root.put("incident", incidentMap(context.incident()));

        List<Map<String, Object>> alerts = context.alerts().stream()
                .limit(MAX_ALERTS)
                .map(this::alertMap)
                .toList();
        root.put("alerts", alerts);

        List<Map<String, Object>> evidence = new ArrayList<>();
        Set<Long> evidenceIds = new LinkedHashSet<>();
        MutableFlag truncated = new MutableFlag(
                context.alertsTruncated()
                        || context.evidenceTruncated()
                        || context.alerts().size() > MAX_ALERTS
                        || context.evidence().size() > MAX_EVIDENCE
        );
        context.evidence().stream().limit(MAX_EVIDENCE).forEach(item -> {
            evidence.add(evidenceMap(item, truncated));
            if (item.id() != null) {
                evidenceIds.add(item.id());
            }
        });
        root.put("evidence", evidence);
        root.put("contextTruncated", truncated.value);

        String json = writeJson(root);
        while (json.length() > MAX_CONTEXT_LENGTH && !evidence.isEmpty()) {
            Map<String, Object> removed = evidence.remove(evidence.size() - 1);
            Object removedId = removed.get("id");
            if (removedId instanceof Number number) {
                evidenceIds.remove(number.longValue());
            }
            truncated.value = true;
            root.put("contextTruncated", true);
            json = writeJson(root);
        }
        if (json.length() > MAX_CONTEXT_LENGTH) {
            throw new IllegalStateException("SRE RCA 上下文超过安全上限");
        }
        return new ModelContext(json, Set.copyOf(evidenceIds), truncated.value);
    }

    private Map<String, Object> incidentMap(SreInvestigationContext.Incident incident) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", incident.id());
        result.put("incidentNo", safeText(incident.incidentNo(), 64, null));
        result.put("service", safeText(incident.service(), 100, null));
        result.put("alertName", safeText(incident.alertName(), 128, null));
        result.put("severity", safeText(incident.severity(), 32, null));
        result.put("state", safeText(incident.state(), 32, null));
        result.put("summary", safeText(incident.summary(), 1_000, null));
        result.put("firstSeen", text(incident.firstSeen()));
        result.put("lastSeen", text(incident.lastSeen()));
        result.put("resolvedAt", text(incident.resolvedAt()));
        return result;
    }

    private Map<String, Object> alertMap(SreInvestigationContext.Alert alert) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", alert.id());
        result.put("source", safeText(alert.source(), 64, null));
        result.put("alertName", safeText(alert.alertName(), 128, null));
        result.put("status", safeText(alert.status(), 32, null));
        result.put("severity", safeText(alert.severity(), 32, null));
        result.put("service", safeText(alert.service(), 100, null));
        result.put("startsAt", safeText(alert.startsAt(), 64, null));
        result.put("endsAt", safeText(alert.endsAt(), 64, null));
        result.put("observedAt", text(alert.observedAt()));
        return result;
    }

    private Map<String, Object> evidenceMap(SreInvestigationContext.Evidence item, MutableFlag truncated) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.id());
        result.put("sourceType", safeText(item.sourceType(), 64, null));
        result.put("sourceRef", safeText(item.sourceRef(), 200, null));
        result.put("queryDescription", safeText(item.queryDescription(), 500, null));
        result.put("capturedAt", text(item.capturedAt()));
        result.put("status", safeText(item.status(), 32, null));
        SnapshotSanitizationBudget budget = new SnapshotSanitizationBudget(MAX_SNAPSHOT_NODES);
        String snapshot = writeJson(sanitizeSnapshotValue(item.snapshot(), 0, budget));
        if (snapshot.length() > MAX_SNAPSHOT_EXCERPT) {
            snapshot = snapshot.substring(0, MAX_SNAPSHOT_EXCERPT) + "[TRUNCATED]";
            truncated.value = true;
        }
        if (budget.truncated) {
            truncated.value = true;
        }
        result.put("snapshotExcerpt", snapshot);
        result.put("snapshotTruncated",
                item.snapshotTruncated() || budget.truncated || snapshot.endsWith("[TRUNCATED]"));
        return result;
    }

    private Object sanitizeSnapshotValue(Object value, int depth, SnapshotSanitizationBudget budget) {
        if (!budget.consume()) {
            return "[TRUNCATED]";
        }
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof CharSequence sequence) {
            return sanitizeSnapshotText(sequence.toString(), MAX_SNAPSHOT_EXCERPT, budget);
        }
        if (depth >= MAX_SNAPSHOT_DEPTH && (value instanceof Map<?, ?> || value instanceof Collection<?>)) {
            budget.truncated = true;
            return "[TRUNCATED_DEPTH]";
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (result.size() >= MAX_SNAPSHOT_MAP_ENTRIES) {
                    budget.truncated = true;
                    break;
                }
                if (entry.getKey() == null) {
                    budget.truncated = true;
                    continue;
                }
                String key = sanitizeSnapshotText(
                        String.valueOf(entry.getKey()), MAX_SNAPSHOT_KEY_LENGTH, budget);
                Object sanitizedValue = sensitiveKey(key)
                        ? redact(budget)
                        : sanitizeSnapshotValue(entry.getValue(), depth + 1, budget);
                result.put(key, sanitizedValue);
            }
            if (map.size() > MAX_SNAPSHOT_MAP_ENTRIES) {
                budget.truncated = true;
            }
            return result;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>(Math.min(collection.size(), MAX_SNAPSHOT_LIST_ENTRIES));
            int index = 0;
            for (Object item : collection) {
                if (index >= MAX_SNAPSHOT_LIST_ENTRIES) {
                    budget.truncated = true;
                    break;
                }
                result.add(sanitizeSnapshotValue(item, depth + 1, budget));
                index++;
            }
            return result;
        }
        return sanitizeSnapshotText(String.valueOf(value), MAX_SNAPSHOT_EXCERPT, budget);
    }

    private String sanitizeSnapshotText(String value, int maxLength, SnapshotSanitizationBudget budget) {
        String sanitized = safeText(value, maxLength, "");
        if (!sanitized.equals(value)) {
            budget.truncated = true;
        }
        return sanitized;
    }

    private boolean sensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.endsWith("authorization")
                || normalized.endsWith("password")
                || normalized.endsWith("passwd")
                || normalized.endsWith("token")
                || normalized.endsWith("secret")
                || normalized.endsWith("apikey")
                || normalized.endsWith("cookie")
                || normalized.endsWith("credential")
                || normalized.endsWith("privatekey");
    }

    private String redact(SnapshotSanitizationBudget budget) {
        budget.truncated = true;
        return "[REDACTED]";
    }

    private SreRcaReport parseReport(String response,
                                     SreInvestigationContext context,
                                     ModelContext modelContext) {
        JSONObject json = AiJsonResponseParser.parse(response);
        AiStructuredOutputValidator.ValidationResult validation =
                SreRcaStructuredOutputSpecs.REPORT.validateObject(json);
        if (!validation.valid()) {
            throw new IllegalArgumentException("SRE RCA 输出不符合结构化契约: " + validation.reason());
        }

        ParseState state = new ParseState();
        List<SreRcaReport.Observation> observations = parseObjects(
                json.getJSONArray("observations"),
                MAX_OBSERVATIONS,
                item -> observation(item, modelContext.evidenceIds(), state)
        ).stream().filter(java.util.Objects::nonNull).toList();
        List<SreRcaReport.Hypothesis> hypotheses = parseObjects(
                json.getJSONArray("hypotheses"),
                MAX_HYPOTHESES,
                item -> hypothesis(item, modelContext.evidenceIds(), state)
        );
        List<SreRcaReport.RecommendedNextStep> recommendations = parseObjects(
                json.getJSONArray("recommendedNextSteps"),
                MAX_RECOMMENDATIONS,
                item -> recommendation(item, modelContext.evidenceIds(), state)
        );
        List<String> limitations = stringList(json.getJSONArray("limitations"), MAX_LIMITATIONS, 500);
        if (state.invalidReference) {
            limitations = appendDistinct(limitations, "模型输出包含无有效证据引用，已由后端移除或降级。",
                    MAX_LIMITATIONS);
        }
        if (modelContext.truncated()) {
            limitations = appendDistinct(limitations, "输入模型的事故上下文已按安全上限裁剪。",
                    MAX_LIMITATIONS);
        }

        String conclusionStatus = normalizeConclusion(
                json.getStr("conclusionStatus"), observations, hypotheses);
        return new SreRcaReport(
                context.incident().id(),
                safeText(context.incident().incidentNo(), 64, "UNKNOWN"),
                "AI",
                conclusionStatus,
                json.getStr("severityAssessment"),
                safeText(json.getStr("executiveSummary"), 2_000, "未生成有效摘要。"),
                observations,
                hypotheses,
                recommendations,
                references(context, modelContext.evidenceIds()),
                limitations,
                modelContext.truncated(),
                false,
                LocalDateTime.now()
        );
    }

    private SreRcaReport.Observation observation(JSONObject item,
                                                 Set<Long> allowedEvidenceIds,
                                                 ParseState state) {
        List<Long> evidenceIds = evidenceIds(item.getJSONArray("evidenceIds"), allowedEvidenceIds, state);
        if (evidenceIds.isEmpty()) {
            return null;
        }
        return new SreRcaReport.Observation(
                safeText(item.getStr("statement"), 1_000, "未提供观察描述。"),
                evidenceIds
        );
    }

    private SreRcaReport.Hypothesis hypothesis(JSONObject item,
                                               Set<Long> allowedEvidenceIds,
                                               ParseState state) {
        List<Long> evidenceIds = evidenceIds(item.getJSONArray("evidenceIds"), allowedEvidenceIds, state);
        List<Long> counterEvidenceIds = evidenceIds(
                item.getJSONArray("counterEvidenceIds"), allowedEvidenceIds, state);
        boolean supported = !evidenceIds.isEmpty();
        Double rawConfidence = item.getDouble("confidence", 0D);
        double confidence = rawConfidence == null ? 0D : Math.max(0D, Math.min(1D, rawConfidence));
        if (!supported) {
            confidence = Math.min(confidence, 0.2D);
        }
        return new SreRcaReport.Hypothesis(
                safeText(item.getStr("title"), 200, "未命名假设"),
                safeText(item.getStr("reasoning"), 2_000, "证据不足。"),
                confidence,
                supported ? "SUPPORTED" : "INSUFFICIENT",
                evidenceIds,
                counterEvidenceIds,
                stringList(item.getJSONArray("nextChecks"), 5, 500)
        );
    }

    private SreRcaReport.RecommendedNextStep recommendation(JSONObject item,
                                                            Set<Long> allowedEvidenceIds,
                                                            ParseState state) {
        return new SreRcaReport.RecommendedNextStep(
                safeText(item.getStr("description"), 1_000, "人工复核事故证据。"),
                item.getStr("risk"),
                evidenceIds(item.getJSONArray("evidenceIds"), allowedEvidenceIds, state)
        );
    }

    private String normalizeConclusion(String requested,
                                       List<SreRcaReport.Observation> observations,
                                       List<SreRcaReport.Hypothesis> hypotheses) {
        long supportedHypotheses = hypotheses.stream()
                .filter(item -> "SUPPORTED".equals(item.evidenceStatus()))
                .count();
        if (observations.isEmpty() && supportedHypotheses == 0) {
            return "INSUFFICIENT_EVIDENCE";
        }
        if (hypotheses.isEmpty()) {
            return "PARTIAL";
        }
        if (supportedHypotheses < hypotheses.size() && "SUPPORTED".equals(requested)) {
            return "PARTIAL";
        }
        return requested;
    }

    private SreRcaReport fallbackReport(SreInvestigationContext context,
                                        List<SreRcaReport.EvidenceReference> references,
                                        boolean contextTruncated) {
        List<SreRcaReport.Observation> observations = references.stream()
                .limit(MAX_OBSERVATIONS)
                .map(item -> new SreRcaReport.Observation(
                        "已采集 " + safeText(item.sourceType(), 64, "UNKNOWN")
                                + " 证据，状态为 " + safeText(item.status(), 32, "UNKNOWN") + "。",
                        List.of(item.id())
                ))
                .toList();
        List<Long> referenceIds = references.stream()
                .map(SreRcaReport.EvidenceReference::id)
                .filter(java.util.Objects::nonNull)
                .limit(5)
                .toList();
        List<String> limitations = new ArrayList<>(List.of(
                "AI 运行时不可用、超时或输出不符合结构化契约。",
                "当前仅返回已采集事实，未形成自动根因结论。"
        ));
        if (contextTruncated) {
            limitations.add("事故上下文已按安全上限裁剪。");
        }
        String summary = "事故 " + safeText(context.incident().incidentNo(), 64, "UNKNOWN")
                + " 已收集 " + references.size() + " 条可引用证据；当前证据不足以生成可靠自动根因结论。";
        return new SreRcaReport(
                context.incident().id(),
                safeText(context.incident().incidentNo(), 64, "UNKNOWN"),
                "FALLBACK",
                "INSUFFICIENT_EVIDENCE",
                severity(context.incident().severity()),
                summary,
                observations,
                List.of(),
                List.of(new SreRcaReport.RecommendedNextStep(
                        "由管理员人工复核现有事故证据和监控时间窗口。",
                        "READ_ONLY",
                        referenceIds
                )),
                references,
                limitations,
                contextTruncated,
                false,
                LocalDateTime.now()
        );
    }

    private List<SreRcaReport.EvidenceReference> references(SreInvestigationContext context,
                                                            Set<Long> allowedEvidenceIds) {
        return context.evidence().stream()
                .filter(item -> allowedEvidenceIds.isEmpty() || allowedEvidenceIds.contains(item.id()))
                .limit(MAX_EVIDENCE)
                .map(item -> new SreRcaReport.EvidenceReference(
                        item.id(),
                        safeText(item.sourceType(), 64, "UNKNOWN"),
                        safeText(item.sourceRef(), 200, "UNKNOWN"),
                        item.capturedAt(),
                        safeText(item.status(), 32, "UNKNOWN")))
                .toList();
    }

    private List<Long> evidenceIds(JSONArray array, Set<Long> allowed, ParseState state) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        if (array == null) {
            state.invalidReference = true;
            return List.of();
        }
        for (int i = 0; i < array.size() && result.size() < MAX_EVIDENCE; i++) {
            try {
                Long id = Long.valueOf(String.valueOf(array.get(i)).trim());
                if (allowed.contains(id)) {
                    result.add(id);
                } else {
                    state.invalidReference = true;
                }
            } catch (RuntimeException ignored) {
                state.invalidReference = true;
            }
        }
        return List.copyOf(result);
    }

    private List<String> stringList(JSONArray array, int maxItems, int maxLength) {
        if (array == null) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int i = 0; i < array.size() && result.size() < maxItems; i++) {
            String value = safeText(String.valueOf(array.get(i)), maxLength, "");
            if (StringUtils.hasText(value)) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private <T> List<T> parseObjects(JSONArray array, int maxItems, Function<JSONObject, T> mapper) {
        List<T> result = new ArrayList<>();
        for (int i = 0; array != null && i < array.size() && result.size() < maxItems; i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                result.add(mapper.apply(item));
            }
        }
        return result;
    }

    private List<String> appendDistinct(List<String> values, String value, int maxItems) {
        LinkedHashSet<String> result = new LinkedHashSet<>(values);
        result.add(value);
        return result.stream().limit(maxItems).toList();
    }

    private String contextStepDetail(SreInvestigationContext context) {
        boolean truncated = context.alertsTruncated() || context.evidenceTruncated();
        return "已加载 " + context.alerts().size() + " 条告警、" + context.evidence().size()
                + " 条证据；输入裁剪=" + truncated + "。";
    }

    private String modelContextStepDetail(ModelContext modelContext) {
        return "已选择 " + modelContext.evidenceIds().size() + " 条可引用证据；模型上下文裁剪="
                + modelContext.truncated() + "。";
    }

    private void persistCompletedRun(Long runId, SreRcaReport report) {
        String status = "AI".equals(report.generationMode()) ? "SUCCEEDED" : "DEGRADED";
        investigationRunService.complete(
                runId,
                status,
                report.generationMode(),
                report.conclusionStatus(),
                report.contextTruncated(),
                writeJson(report)
        );
    }

    private void markRunFailed(Long runId, RuntimeException exception) {
        try {
            investigationRunService.recordStep(
                    runId, 99, "RUN_FAILED", "FAILED", "调查运行异常中止，未执行任何修复动作。"
            );
        } catch (RuntimeException stepException) {
            log.warn("SRE RCA 失败步骤记录失败: runId={}, reason={}",
                    runId, stepException.getClass().getSimpleName());
        }
        try {
            investigationRunService.fail(runId, exception.getClass().getSimpleName());
        } catch (RuntimeException failureException) {
            log.warn("SRE RCA 运行失败状态写入失败: runId={}, reason={}",
                    runId, failureException.getClass().getSimpleName());
        }
    }

    private SreRcaRunSummary toRunSummary(SreInvestigationRun run) {
        return new SreRcaRunSummary(
                run.getId(),
                run.getIncidentId(),
                run.getStatus(),
                run.getTriggerSource(),
                run.getRequestedBy(),
                run.getGenerationMode(),
                run.getConclusionStatus(),
                run.getAlertCount() == null ? 0 : run.getAlertCount(),
                run.getEvidenceCount() == null ? 0 : run.getEvidenceCount(),
                Boolean.TRUE.equals(run.getContextTruncated()),
                run.getFailureCode(),
                run.getStartedAt(),
                run.getCompletedAt()
        );
    }

    private SreRcaRunDetail.Step toRunStep(SreInvestigationStep step) {
        return new SreRcaRunDetail.Step(
                step.getId(),
                step.getStepOrder() == null ? 0 : step.getStepOrder(),
                step.getStepCode(),
                step.getStatus(),
                step.getDetail(),
                step.getRecordedAt()
        );
    }

    private SreRcaReport readPersistedReport(SreInvestigationRun run) {
        if (!StringUtils.hasText(run.getReportJson())) {
            return null;
        }
        try {
            return objectMapper.readValue(run.getReportJson(), SreRcaReport.class);
        } catch (JsonProcessingException exception) {
            log.warn("SRE RCA 持久化报告解析失败: runId={}, reason={}",
                    run.getId(), exception.getClass().getSimpleName());
            return null;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("SRE RCA 上下文序列化失败", exception);
        }
    }

    private String safeText(String value, int maxLength, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String sanitized = CONTROL_PATTERN.matcher(value).replaceAll("").trim();
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer [REDACTED]");
        sanitized = INLINE_SECRET_PATTERN.matcher(sanitized).replaceAll("$1=[REDACTED]");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private String severity(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "critical" -> "CRITICAL";
            case "high" -> "HIGH";
            case "warning", "medium" -> "MEDIUM";
            case "low", "info" -> "LOW";
            default -> "UNKNOWN";
        };
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record ModelContext(String json, Set<Long> evidenceIds, boolean truncated) {
    }

    private static final class ParseState {
        private boolean invalidReference;
    }

    private static final class MutableFlag {
        private boolean value;

        private MutableFlag(boolean value) {
            this.value = value;
        }
    }

    private static final class SnapshotSanitizationBudget {
        private int remaining;
        private boolean truncated;

        private SnapshotSanitizationBudget(int remaining) {
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
