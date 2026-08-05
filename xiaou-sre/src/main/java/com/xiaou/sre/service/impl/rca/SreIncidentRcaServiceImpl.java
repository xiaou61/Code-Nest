package com.xiaou.sre.service.impl.rca;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.sre.domain.SreInvestigationArtifact;
import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.domain.SreInvestigationStep;
import com.xiaou.sre.domain.SreInvestigationFeedback;
import com.xiaou.sre.dto.request.SreInvestigationArtifactCapture;
import com.xiaou.sre.dto.response.SreInvestigationContext;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.service.SreInvestigationArtifactService;
import com.xiaou.sre.service.SreInvestigationFacade;
import com.xiaou.sre.service.SreInvestigationFeedbackService;
import com.xiaou.sre.service.SreInvestigationRunService;
import com.xiaou.sre.service.SreReadOnlyInvestigationToolService;
import com.xiaou.sre.service.SreReadOnlyToolResult;
import com.xiaou.sre.service.SreValidationException;
import com.xiaou.sre.dto.rca.SreRcaEvaluationSample;
import com.xiaou.sre.dto.rca.SreRcaFeedback;
import com.xiaou.sre.dto.rca.SreRcaFeedbackRequest;
import com.xiaou.sre.dto.rca.SreRcaReport;
import com.xiaou.sre.dto.rca.SreRcaRunDetail;
import com.xiaou.sre.dto.rca.SreRcaRunSummary;
import com.xiaou.sre.service.rca.SreIncidentRcaService;
import com.xiaou.sre.service.rca.SreInvestigationPlan;
import com.xiaou.sre.service.rca.SreInvestigationPlanner;
import com.xiaou.sre.service.rca.SreInvestigationPlanningInput;
import com.xiaou.sre.service.rca.SreModelExecution;
import com.xiaou.sre.service.rca.SreRcaAnalysisInput;
import com.xiaou.sre.service.rca.SreRcaAnalyzer;
import com.xiaou.sre.service.rca.SreRcaTriggerSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    private static final String EVALUATION_SCHEMA_VERSION = "code-nest.sre.rca-eval.v1";
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
    private static final int MAX_INVESTIGATION_ROUNDS = 5;

    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]{6,}");
    private static final Pattern INLINE_SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(authorization|password|passwd|token|secret|api[-_ ]?key|cookie|credential)"
                    + "\\b\\s*[:=]\\s*([^\\s,;]+)");
    private static final Pattern STANDALONE_CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)\\b(?:sk-[A-Za-z0-9_-]{16,}|gh[pousr]_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16})\\b");
    private static final Pattern CONTROL_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    private final SreInvestigationFacade investigationFacade;
    private final SreInvestigationPlanner investigationPlanner;
    private final SreReadOnlyInvestigationToolService readOnlyToolService;
    private final SreRcaAnalyzer rcaAnalyzer;
    private final ObjectMapper objectMapper;
    private final SreInvestigationRunService investigationRunService;
    private final SreInvestigationFeedbackService investigationFeedbackService;
    private final SreInvestigationArtifactService investigationArtifactService;
    private final SreMetricsRecorder metricsRecorder;

    @Override
    public Optional<SreRcaReport> investigate(Long incidentId,
                                              SreRcaTriggerSource triggerSource,
                                              Long requestedBy) {
        long startNanos = System.nanoTime();
        InvestigationMetrics metrics = new InvestigationMetrics();
        try {
            Optional<SreRcaReport> result = investigateInternal(
                    incidentId, triggerSource, requestedBy, metrics);
            if (result.isEmpty()) {
                metrics.outcome = "skipped";
            }
            return result;
        } catch (RuntimeException exception) {
            metrics.outcome = "failed";
            throw exception;
        } finally {
            recordInvestigationMetrics(metrics, System.nanoTime() - startNanos);
        }
    }

    private Optional<SreRcaReport> investigateInternal(Long incidentId,
                                                       SreRcaTriggerSource triggerSource,
                                                       Long requestedBy,
                                                       InvestigationMetrics metrics) {
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

        ModelContext initialModelContext;
        try {
            initialModelContext = buildModelContext(context);
        } catch (RuntimeException exception) {
            log.warn("SRE RCA 上下文构建失败，返回证据不足报告: incidentId={}, reason={}",
                    incidentId, exception.getClass().getSimpleName());
            SreRcaReport report = rcaAnalyzer.fallback(analysisInput(context, "{}", Set.of(), true));
            try {
                investigationRunService.recordStep(
                        run.getId(), 2, "INVESTIGATION_PLAN", "SKIPPED", "上下文构建失败，未生成调查计划。"
                );
                investigationRunService.recordStep(
                        run.getId(), 3, "MODEL_CONTEXT_BUILT", "DEGRADED", "上下文构建失败，已进入确定性降级。"
                );
                investigationRunService.recordStep(
                        run.getId(), 4, "MODEL_ANALYSIS", "SKIPPED", "未调用模型。"
                );
                investigationRunService.recordStep(
                        run.getId(), 5, "REPORT_VALIDATED", "SUCCEEDED", "已生成只含事实的降级报告。"
                );
                persistCompletedRun(run.getId(), report);
            } catch (RuntimeException persistenceException) {
                markRunFailed(run.getId(), persistenceException);
                throw persistenceException;
            }
            metrics.generationMode = "fallback";
            metrics.outcome = "degraded";
            return Optional.of(report);
        }

        int nextStepOrder = 2;
        SreInvestigationPlan plan;
        try {
            List<String> availableToolKeys = Optional.ofNullable(readOnlyToolService.availableToolKeys())
                    .orElse(List.of());
            plan = investigationPlanner.plan(new SreInvestigationPlanningInput(
                    initialModelContext.json(),
                    new LinkedHashSet<>(availableToolKeys),
                    readOnlyToolService.fallbackToolKeys(context.incident().alertName())
            ));
            List<String> plannedTools = boundedPlannedTools(plan, availableToolKeys);
            plan = normalizedPlan(plan, plannedTools);
            investigationRunService.recordStep(
                    run.getId(), nextStepOrder++, "INVESTIGATION_PLAN", planStepStatus(plan), planStepDetail(plan));
        } catch (RuntimeException exception) {
            markRunFailed(run.getId(), exception);
            throw exception;
        }

        try {
            int createdEvidence = 0;
            for (String toolKey : plan.toolKeys()) {
                SreReadOnlyToolResult toolResult = readOnlyToolService.execute(
                        incidentId, run.getId(), toolKey);
                metrics.rounds++;
                investigationRunService.recordStep(
                        run.getId(),
                        nextStepOrder++,
                        "READ_ONLY_TOOL",
                        toolStepStatus(toolResult),
                        toolStepDetail(metrics.rounds, toolResult)
                );
                if (!toolResult.created()) {
                    break;
                }
                createdEvidence++;
            }

            if (createdEvidence > 0) {
                context = investigationFacade.findByIncidentId(incidentId)
                        .orElseThrow(() -> new IllegalStateException("只读调查后无法重新加载事故证据"));
                investigationRunService.recordStep(
                        run.getId(),
                        nextStepOrder++,
                        "EVIDENCE_RELOADED",
                        "SUCCEEDED",
                        "只读调查新增 " + createdEvidence + " 条证据，已重新加载受限上下文。"
                );
            }

            ModelContext modelContext = buildModelContext(context);
            investigationRunService.recordStep(
                    run.getId(),
                    nextStepOrder++,
                    "MODEL_CONTEXT_BUILT",
                    "SUCCEEDED",
                    modelContextStepDetail(modelContext)
            );
            SreRcaAnalysisInput analysisInput = analysisInput(context, modelContext);
            SreModelExecution<SreRcaReport> execution = rcaAnalyzer.analyze(analysisInput);
            SreRcaReport report = execution.value();
            if (report == null) {
                report = rcaAnalyzer.fallback(analysisInput);
            }

            captureArtifact(incidentId, run.getId(), modelContext, execution);

            String analysisStatus = "AI".equals(report.generationMode()) ? "SUCCEEDED" : "DEGRADED";
            String analysisDetail = "AI".equals(report.generationMode())
                    ? "模型返回了通过结构化契约校验的报告。"
                    : "模型不可用或输出无效，已使用确定性降级报告。";
            investigationRunService.recordStep(
                    run.getId(), nextStepOrder++, "MODEL_ANALYSIS", analysisStatus, analysisDetail);
            investigationRunService.recordStep(
                    run.getId(), nextStepOrder, "REPORT_VALIDATED", "SUCCEEDED", "证据引用和只读风险边界校验完成。"
            );
            persistCompletedRun(run.getId(), report);

            metrics.generationMode = report.generationMode().toLowerCase(Locale.ROOT);
            metrics.outcome = "AI".equals(report.generationMode()) ? "succeeded" : "degraded";

            log.info("SRE RCA 调查完成: incidentId={}, runId={}, mode={}, conclusionStatus={}, evidenceIds={}",
                    incidentId, run.getId(), report.generationMode(), report.conclusionStatus(),
                    modelContext.evidenceIds());
            return Optional.of(report);
        } catch (RuntimeException exception) {
            markRunFailed(run.getId(), exception);
            throw exception;
        }
    }

    private List<String> boundedPlannedTools(SreInvestigationPlan plan,
                                             List<String> availableToolKeys) {
        if (plan == null || !"INVESTIGATE".equalsIgnoreCase(plan.decision())
                || plan.toolKeys() == null || plan.toolKeys().isEmpty()) {
            return List.of();
        }
        Set<String> allowed = availableToolKeys == null
                ? Set.of()
                : availableToolKeys.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String toolKey : plan.toolKeys()) {
            if (result.size() >= MAX_INVESTIGATION_ROUNDS) {
                break;
            }
            if (!StringUtils.hasText(toolKey)) {
                continue;
            }
            String normalized = toolKey.trim().toLowerCase(Locale.ROOT);
            if (allowed.contains(normalized)) {
                result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    private SreInvestigationPlan normalizedPlan(SreInvestigationPlan plan, List<String> plannedTools) {
        String generationMode = plan == null ? "FALLBACK" : safeText(
                plan.generationMode(), 16, "FALLBACK").toUpperCase(Locale.ROOT);
        String invocationOutcome = plan == null ? "UNEXPECTED_FAILURE" : safeText(
                plan.invocationOutcome(), 64, "UNEXPECTED_FAILURE").toUpperCase(Locale.ROOT);
        String reason = plan == null
                ? "调查计划不可用。"
                : safeText(plan.reason(), 120, "未提供调查理由。");
        return new SreInvestigationPlan(
                plannedTools.isEmpty() ? "STOP" : "INVESTIGATE",
                reason,
                plannedTools,
                generationMode,
                invocationOutcome
        );
    }

    private String planStepStatus(SreInvestigationPlan plan) {
        return "AI".equals(plan.generationMode()) && "SUCCESS".equals(plan.invocationOutcome())
                ? "SUCCEEDED"
                : "DEGRADED";
    }

    private String planStepDetail(SreInvestigationPlan plan) {
        return "固定只读调查计划已收敛；计划轮数=" + plan.toolKeys().size()
                + "；工具=" + (plan.toolKeys().isEmpty() ? "无" : String.join(",", plan.toolKeys()))
                + "；生成模式=" + plan.generationMode()
                + "；结果=" + plan.invocationOutcome()
                + "；理由=" + plan.reason() + "。";
    }

    private String toolStepStatus(SreReadOnlyToolResult result) {
        if (result == null) {
            return "FAILED";
        }
        return switch (result.status()) {
            case "AVAILABLE" -> "SUCCEEDED";
            case "DUPLICATE" -> "SKIPPED";
            case "UNAVAILABLE" -> "DEGRADED";
            default -> "FAILED";
        };
    }

    private String toolStepDetail(int round, SreReadOnlyToolResult result) {
        if (result == null) {
            return "第 " + round + " 轮固定只读工具未返回结果。";
        }
        return "第 " + round + " 轮固定只读工具=" + result.toolKey()
                + "；状态=" + result.status()
                + "；证据ID=" + result.evidenceId() + "。";
    }

    private void recordInvestigationMetrics(InvestigationMetrics metrics, long durationNanos) {
        try {
            metricsRecorder.recordInvestigation(
                    metrics.outcome, metrics.generationMode, metrics.rounds, durationNanos);
        } catch (RuntimeException exception) {
            log.warn("SRE RCA 调查指标记录失败: reason={}", exception.getClass().getSimpleName());
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
                        readPersistedReport(run),
                        investigationArtifactService.findByIncidentIdAndRunId(incidentId, runId)
                                .map(this::toProvenance)
                                .orElse(null),
                        investigationFeedbackService.findLatest(incidentId, runId)
                                .map(this::toFeedback)
                                .orElse(null)
                ));
    }

    @Override
    public Optional<SreRcaFeedback> saveFeedback(Long incidentId,
                                                 Long runId,
                                                 SreRcaFeedbackRequest request,
                                                 Long reviewedBy) {
        if (request == null) {
            throw new IllegalArgumentException("RCA 反馈不能为空");
        }
        try {
            return investigationFeedbackService.save(
                    incidentId,
                    runId,
                    request.accuracy(),
                    request.gapType(),
                    request.note(),
                    request.expectedConclusion(),
                    reviewedBy
            ).map(this::toFeedback);
        } catch (SreValidationException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    @Override
    public Optional<SreRcaEvaluationSample> getEvaluationSample(Long incidentId, Long runId) {
        return getRun(incidentId, runId)
                .filter(detail -> detail.report() != null && detail.feedback() != null)
                .map(detail -> {
                    SreRcaFeedback feedback = detail.feedback();
                    SreRcaReport report = sanitizeEvaluationReport(detail.report());
                    String expectedConclusion = safeText(feedback.expectedConclusion(), 2_000, null);
                    if (!StringUtils.hasText(expectedConclusion)
                            && "ACCURATE".equals(feedback.accuracy())) {
                        expectedConclusion = report.executiveSummary();
                    }
                    return new SreRcaEvaluationSample(
                            EVALUATION_SCHEMA_VERSION,
                            detail.run().id(),
                            detail.run().generationMode(),
                            detail.run().conclusionStatus(),
                            report,
                            new SreRcaEvaluationSample.Evaluation(
                                    feedback.accuracy(),
                                    feedback.gapType(),
                                    expectedConclusion,
                                    feedback.reviewedAt()
                            )
                    );
                });
    }

    private SreRcaReport sanitizeEvaluationReport(SreRcaReport report) {
        List<SreRcaReport.Observation> observations = report.observations().stream()
                .limit(MAX_OBSERVATIONS)
                .map(item -> new SreRcaReport.Observation(
                        safeText(item.statement(), 1_000, ""),
                        sanitizeEvidenceIds(item.evidenceIds())
                ))
                .toList();
        List<SreRcaReport.Hypothesis> hypotheses = report.hypotheses().stream()
                .limit(MAX_HYPOTHESES)
                .map(item -> new SreRcaReport.Hypothesis(
                        safeText(item.title(), 200, ""),
                        safeText(item.reasoning(), 2_000, ""),
                        Double.isFinite(item.confidence())
                                ? Math.max(0D, Math.min(1D, item.confidence()))
                                : 0D,
                        safeText(item.evidenceStatus(), 32, "INSUFFICIENT"),
                        sanitizeEvidenceIds(item.evidenceIds()),
                        sanitizeEvidenceIds(item.counterEvidenceIds()),
                        sanitizeStrings(item.nextChecks(), 8, 500)
                ))
                .toList();
        List<SreRcaReport.RecommendedNextStep> recommendations = report.recommendedNextSteps().stream()
                .limit(MAX_RECOMMENDATIONS)
                .map(item -> new SreRcaReport.RecommendedNextStep(
                        safeText(item.description(), 1_000, ""),
                        safeText(item.risk(), 32, "READ_ONLY"),
                        sanitizeEvidenceIds(item.evidenceIds())
                ))
                .toList();
        List<SreRcaReport.EvidenceReference> references = report.evidenceReferences().stream()
                .limit(MAX_EVIDENCE)
                .map(item -> new SreRcaReport.EvidenceReference(
                        item.id(),
                        safeText(item.sourceType(), 64, "UNKNOWN"),
                        safeText(item.sourceRef(), 200, "UNKNOWN"),
                        item.capturedAt(),
                        safeText(item.status(), 32, "UNKNOWN")
                ))
                .toList();
        return new SreRcaReport(
                report.incidentId(),
                safeText(report.incidentNo(), 64, "UNKNOWN"),
                safeText(report.generationMode(), 16, "UNKNOWN"),
                safeText(report.conclusionStatus(), 32, "INSUFFICIENT_EVIDENCE"),
                safeText(report.severityAssessment(), 32, "UNKNOWN"),
                safeText(report.executiveSummary(), 2_000, ""),
                observations,
                hypotheses,
                recommendations,
                references,
                sanitizeStrings(report.limitations(), MAX_LIMITATIONS, 1_000),
                report.contextTruncated(),
                false,
                report.generatedAt()
        );
    }

    private List<Long> sanitizeEvidenceIds(Collection<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .limit(MAX_EVIDENCE)
                .toList();
    }

    private List<String> sanitizeStrings(Collection<String> values, int maxItems, int maxLength) {
        if (values == null) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (result.size() >= maxItems) {
                break;
            }
            String sanitized = safeText(value, maxLength, "");
            if (StringUtils.hasText(sanitized)) {
                result.add(sanitized);
            }
        }
        return List.copyOf(result);
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

    private SreRcaAnalysisInput analysisInput(SreInvestigationContext context, ModelContext modelContext) {
        return analysisInput(
                context,
                modelContext.json(),
                modelContext.evidenceIds(),
                modelContext.truncated()
        );
    }

    private SreRcaAnalysisInput analysisInput(SreInvestigationContext context,
                                              String contextJson,
                                              Set<Long> evidenceIds,
                                              boolean contextTruncated) {
        return new SreRcaAnalysisInput(
                contextJson,
                context.incident().id(),
                context.incident().incidentNo(),
                context.incident().severity(),
                references(context, evidenceIds),
                contextTruncated
        );
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

    private void captureArtifact(Long incidentId,
                                 Long runId,
                                 ModelContext modelContext,
                                 SreModelExecution<SreRcaReport> execution) {
        investigationArtifactService.capture(new SreInvestigationArtifactCapture(
                incidentId,
                runId,
                modelContext.json(),
                modelContext.truncated(),
                rcaAnalyzer.promptId(),
                rcaAnalyzer.schemaId(),
                execution.provider(),
                execution.configuredModel(),
                execution.actualModel(),
                execution.outcome()
        )).orElseThrow(() -> new IllegalStateException("SRE 调查回放产物归属校验失败"));
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

    private SreRcaFeedback toFeedback(SreInvestigationFeedback feedback) {
        return new SreRcaFeedback(
                feedback.getId(),
                feedback.getRunId(),
                feedback.getAccuracy(),
                feedback.getGapType(),
                feedback.getNote(),
                feedback.getExpectedConclusion(),
                feedback.getReviewedBy(),
                feedback.getReviewedAt()
        );
    }

    private SreRcaRunDetail.Provenance toProvenance(SreInvestigationArtifact artifact) {
        return new SreRcaRunDetail.Provenance(
                artifact.getId(),
                artifact.getPromptId(),
                artifact.getSchemaId(),
                artifact.getProvider(),
                artifact.getConfiguredModel(),
                artifact.getActualModel(),
                artifact.getInvocationOutcome(),
                artifact.getContextSha256(),
                artifact.getContextLength() == null ? 0 : artifact.getContextLength(),
                Boolean.TRUE.equals(artifact.getContextTruncated()),
                artifact.getCreatedAt()
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
        sanitized = STANDALONE_CREDENTIAL_PATTERN.matcher(sanitized).replaceAll("[REDACTED]");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record ModelContext(String json, Set<Long> evidenceIds, boolean truncated) {
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

    private static final class InvestigationMetrics {
        private String outcome = "skipped";
        private String generationMode = "unknown";
        private int rounds;
    }
}
