package com.xiaou.system.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationResult;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.domain.SreRcaEvaluationSuite;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersion;
import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersionSnapshot;
import com.xiaou.sre.dto.request.SreRcaEvaluationResultCapture;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunCompletion;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunStart;
import com.xiaou.sre.dto.request.SreRcaEvaluationSuiteCreateCommand;
import com.xiaou.sre.dto.request.SreRcaEvaluationSuiteVersionPublishCommand;
import com.xiaou.sre.service.SreRcaEvaluationCaseService;
import com.xiaou.sre.service.SreRcaEvaluationRunService;
import com.xiaou.sre.service.SreRcaEvaluationSuiteService;
import com.xiaou.sre.service.SreReplayContextPolicy;
import com.xiaou.sre.service.SreValidationException;
import com.xiaou.system.dto.SreRcaEvaluationCaseResult;
import com.xiaou.system.dto.SreRcaEvaluationCaseSummary;
import com.xiaou.system.dto.SreRcaEvaluationGateDecision;
import com.xiaou.system.dto.SreRcaEvaluationGateInput;
import com.xiaou.system.dto.SreRcaEvaluationGateSummary;
import com.xiaou.system.dto.SreRcaEvaluationRunDetail;
import com.xiaou.system.dto.SreRcaEvaluationRunSummary;
import com.xiaou.system.dto.SreRcaEvaluationScore;
import com.xiaou.system.dto.SreRcaEvaluationSuiteCreateRequest;
import com.xiaou.system.dto.SreRcaEvaluationSuiteSummary;
import com.xiaou.system.dto.SreRcaEvaluationSuiteVersionDetail;
import com.xiaou.system.dto.SreRcaEvaluationSuiteVersionPublishRequest;
import com.xiaou.system.dto.SreRcaEvaluationSuiteVersionSummary;
import com.xiaou.system.dto.SreRcaReport;
import com.xiaou.system.service.SreRcaAnalysisInput;
import com.xiaou.system.service.SreRcaAnalyzer;
import com.xiaou.system.service.SreRcaEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 管理员显式触发的 RCA 离线回放编排。
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SreRcaEvaluationServiceImpl implements SreRcaEvaluationService {

    private static final int MAX_CASES_PER_RUN = 100;
    private static final int MAX_CONTEXT_LENGTH = 60_000;

    private final SreRcaEvaluationCaseService caseService;
    private final SreRcaEvaluationRunService runService;
    private final SreRcaEvaluationSuiteService suiteService;
    private final SreRcaAnalyzer analyzer;
    private final SreRcaEvaluationScorer scorer;
    private final SreRcaEvaluationGateEvaluator gateEvaluator;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<SreRcaEvaluationCaseSummary> promote(Long incidentId,
                                                         Long runId,
                                                         Long feedbackId,
                                                         Long promotedBy) {
        try {
            return caseService.promote(incidentId, runId, feedbackId, promotedBy)
                    .map(this::toCaseSummary);
        } catch (SreValidationException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    @Override
    public List<SreRcaEvaluationCaseSummary> listCases(int limit) {
        return caseService.listRecent(limit).stream().map(this::toCaseSummary).toList();
    }

    @Override
    public SreRcaEvaluationSuiteSummary createSuite(
            SreRcaEvaluationSuiteCreateRequest request, Long createdBy) {
        if (request == null) {
            throw new IllegalArgumentException("评测套件不能为空");
        }
        try {
            return toSuiteSummary(suiteService.create(new SreRcaEvaluationSuiteCreateCommand(
                    request.suiteKey(), request.name(), request.description(), createdBy)));
        } catch (SreValidationException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    @Override
    public List<SreRcaEvaluationSuiteSummary> listSuites(int limit) {
        return suiteService.listSuites(limit).stream().map(this::toSuiteSummary).toList();
    }

    @Override
    public SreRcaEvaluationSuiteVersionDetail publishSuiteVersion(
            Long suiteId,
            SreRcaEvaluationSuiteVersionPublishRequest request,
            Long publishedBy) {
        if (request == null) {
            throw new IllegalArgumentException("评测套件版本不能为空");
        }
        try {
            SreRcaEvaluationSuiteVersionSnapshot snapshot = suiteService.publishVersion(
                    new SreRcaEvaluationSuiteVersionPublishCommand(
                            suiteId,
                            request.caseIds(),
                            request.minimumPassRate(),
                            request.minimumAverageScore(),
                            Boolean.TRUE.equals(request.requireAllSafety()),
                            Boolean.TRUE.equals(request.requireNoDegraded()),
                            publishedBy
                    ));
            return toSuiteVersionDetail(snapshot);
        } catch (SreValidationException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    @Override
    public List<SreRcaEvaluationSuiteVersionSummary> listSuiteVersions(Long suiteId, int limit) {
        return suiteService.listVersions(suiteId, limit).stream()
                .map(this::toSuiteVersionSummary)
                .toList();
    }

    @Override
    public Optional<SreRcaEvaluationSuiteVersionDetail> getSuiteVersion(Long suiteVersionId) {
        try {
            return suiteService.findVersionById(suiteVersionId).map(this::toSuiteVersionDetail);
        } catch (SreValidationException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    @Override
    public SreRcaEvaluationRunDetail run(Long caseId, Long suiteVersionId, Long requestedBy) {
        if (requestedBy == null || requestedBy <= 0) {
            throw new IllegalArgumentException("评测发起管理员 ID 不合法");
        }
        EvaluationSelection selection = loadSelection(caseId, suiteVersionId);
        List<SreRcaEvaluationCase> cases = selection.cases();
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("当前没有可回放的 RCA 评测用例");
        }

        SreRcaEvaluationSuite suite = selection.suite();
        SreRcaEvaluationSuiteVersion suiteVersion = selection.version();
        SreRcaEvaluationRun run = runService.start(new SreRcaEvaluationRunStart(
                caseId,
                suiteVersion == null ? null : suiteVersion.getId(),
                suite == null ? null : suite.getSuiteKey(),
                suiteVersion == null ? null : suiteVersion.getVersionNo(),
                suiteVersion == null ? null : suiteVersion.getManifestSha256(),
                "MANUAL",
                suiteVersion == null ? null : suiteVersion.getScoringPolicyId(),
                suiteVersion == null ? null : suiteVersion.getGateEvaluatorId(),
                suiteVersion == null ? null : suiteVersion.getMinimumPassRate(),
                suiteVersion == null ? null : suiteVersion.getMinimumAverageScore(),
                suiteVersion == null ? null : suiteVersion.getRequireAllSafety(),
                suiteVersion == null ? null : suiteVersion.getRequireNoDegraded(),
                requestedBy,
                cases.size(),
                analyzer.promptId(),
                analyzer.schemaId()
        ));
        List<SreRcaEvaluationResult> persistedResults = new ArrayList<>();
        int passedCount = 0;
        int unsafeCount = 0;
        int degradedCount = 0;
        BigDecimal scoreTotal = BigDecimal.ZERO;
        String provider = null;
        String configuredModel = null;

        try {
            for (SreRcaEvaluationCase evaluationCase : cases) {
                SreRcaEvaluationResultCapture capture = evaluateCase(run.getId(), evaluationCase);
                SreRcaEvaluationResult persisted = runService.record(capture);
                persistedResults.add(persisted);
                if (Boolean.TRUE.equals(persisted.getPassed())) {
                    passedCount++;
                }
                if (!Boolean.TRUE.equals(persisted.getSafetyCompliant())) {
                    unsafeCount++;
                }
                scoreTotal = scoreTotal.add(valueOrZero(persisted.getTotalScore()));
                if (!"SUCCEEDED".equals(persisted.getStatus())) {
                    degradedCount++;
                }
                if (!StringUtils.hasText(provider)) {
                    provider = persisted.getProvider();
                    configuredModel = persisted.getConfiguredModel();
                }
            }

            int completedCount = persistedResults.size();
            int failedCount = completedCount - passedCount;
            BigDecimal averageScore = scoreTotal
                    .divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP);
            String terminalStatus = degradedCount > 0 ? "DEGRADED" : "SUCCEEDED";
            String resolvedProvider = StringUtils.hasText(provider) ? provider : "unknown";
            SreRcaEvaluationGateDecision gate = gateEvaluator.evaluate(new SreRcaEvaluationGateInput(
                    suiteVersion != null,
                    cases.size(),
                    completedCount,
                    passedCount,
                    averageScore,
                    unsafeCount,
                    degradedCount,
                    suiteVersion == null ? null : suiteVersion.getMinimumPassRate(),
                    suiteVersion == null ? null : suiteVersion.getMinimumAverageScore(),
                    suiteVersion != null && Boolean.TRUE.equals(suiteVersion.getRequireAllSafety()),
                    suiteVersion != null && Boolean.TRUE.equals(suiteVersion.getRequireNoDegraded())
            ));
            runService.complete(new SreRcaEvaluationRunCompletion(
                    run.getId(),
                    terminalStatus,
                    completedCount,
                    passedCount,
                    failedCount,
                    averageScore,
                    gate.passRate(),
                    unsafeCount,
                    degradedCount,
                    gate.status(),
                    writeJson(gate.failureCodes()),
                    resolvedProvider,
                    configuredModel
            ));
            completeInMemory(
                    run, terminalStatus, completedCount, passedCount, failedCount,
                    averageScore, gate, unsafeCount, degradedCount,
                    resolvedProvider, configuredModel);
            return new SreRcaEvaluationRunDetail(
                    toRunSummary(run),
                    persistedResults.stream().map(this::toCaseResult).toList()
            );
        } catch (RuntimeException exception) {
            markRunFailed(run.getId(), exception);
            throw exception;
        }
    }

    @Override
    public List<SreRcaEvaluationRunSummary> listRuns(int limit) {
        return runService.listRecent(limit).stream().map(this::toRunSummary).toList();
    }

    @Override
    public Optional<SreRcaEvaluationRunDetail> getRun(Long runId) {
        return runService.findById(runId)
                .map(run -> new SreRcaEvaluationRunDetail(
                        toRunSummary(run),
                        runService.listResults(run.getId()).stream().map(this::toCaseResult).toList()
                ));
    }

    @Override
    public Optional<SreRcaEvaluationGateSummary> getGate(Long runId) {
        return runService.findById(runId).map(run -> {
            SreRcaEvaluationRunSummary summary = toRunSummary(run);
            return new SreRcaEvaluationGateSummary(
                    summary.id(),
                    summary.status(),
                    summary.gateStatus(),
                    summary.suiteVersionId(),
                    summary.suiteKey(),
                    summary.suiteVersion(),
                    summary.suiteManifestSha256(),
                    summary.triggerSource(),
                    summary.caseCount(),
                    summary.completedCount(),
                    summary.passedCount(),
                    summary.failedCount(),
                    summary.averageScore(),
                    summary.passRate(),
                    summary.unsafeCount(),
                    summary.degradedCount(),
                    summary.gateMinimumPassRate(),
                    summary.gateMinimumAverageScore(),
                    summary.gateRequireAllSafety(),
                    summary.gateRequireNoDegraded(),
                    summary.gateFailureCodes(),
                    summary.promptId(),
                    summary.schemaId(),
                    summary.provider(),
                    summary.configuredModel(),
                    summary.startedAt(),
                    summary.completedAt()
            );
        });
    }

    private EvaluationSelection loadSelection(Long caseId, Long suiteVersionId) {
        if (caseId != null && suiteVersionId != null) {
            throw new IllegalArgumentException("评测用例与套件版本不能同时指定");
        }
        if (suiteVersionId != null) {
            if (suiteVersionId <= 0) {
                throw new IllegalArgumentException("评测套件版本 ID 不合法");
            }
            try {
                SreRcaEvaluationSuiteVersionSnapshot snapshot = suiteService
                        .findVersionById(suiteVersionId)
                        .orElseThrow(() -> new IllegalArgumentException("RCA 评测套件版本不存在"));
                return new EvaluationSelection(
                        snapshot.cases(), snapshot.suite(), snapshot.version());
            } catch (SreValidationException exception) {
                throw new IllegalArgumentException(exception.getMessage(), exception);
            }
        }
        if (caseId != null) {
            if (caseId <= 0) {
                throw new IllegalArgumentException("评测用例 ID 不合法");
            }
            return new EvaluationSelection(
                    caseService.findById(caseId).map(List::of).orElseGet(List::of),
                    null,
                    null
            );
        }
        List<SreRcaEvaluationCase> cases = caseService.listRecent(MAX_CASES_PER_RUN).stream()
                .map(item -> caseService.findById(item.getId())
                        .orElseThrow(() -> new IllegalStateException("RCA 评测用例读取失败")))
                .toList();
        return new EvaluationSelection(cases, null, null);
    }

    private SreRcaEvaluationResultCapture evaluateCase(Long runId, SreRcaEvaluationCase evaluationCase) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            SreRcaReport baseline = readReport(evaluationCase.getBaselineReportJson(), "基准");
            validateCaseIntegrity(evaluationCase, baseline);
            SreRcaAnalysisInput input = new SreRcaAnalysisInput(
                    evaluationCase.getContextJson(),
                    baseline.incidentId(),
                    baseline.incidentNo(),
                    baseline.severityAssessment(),
                    baseline.evidenceReferences(),
                    Boolean.TRUE.equals(evaluationCase.getContextTruncated())
            );
            AiExecutionResult<SreRcaReport> execution = analyzer.analyze(input);
            SreRcaReport candidate = execution == null ? null : execution.value();
            if (candidate == null) {
                throw new IllegalStateException("RCA 评测未返回候选报告");
            }
            SreRcaEvaluationScore score = scorer.score(
                    baseline,
                    candidate,
                    evaluationCase.getExpectedConclusion(),
                    execution.outcome()
            );
            String status = "SUCCESS".equals(execution.outcome())
                    && "AI".equals(candidate.generationMode()) ? "SUCCEEDED" : "DEGRADED";
            return new SreRcaEvaluationResultCapture(
                    runId,
                    evaluationCase.getId(),
                    status,
                    writeJson(candidate),
                    analyzer.promptId(),
                    analyzer.schemaId(),
                    safeProvider(execution.provider(), evaluationCase.getSourceProvider()),
                    execution.configuredModel(),
                    execution.actualModel(),
                    execution.outcome(),
                    BigDecimal.valueOf(score.conclusionSimilarity()),
                    BigDecimal.valueOf(score.evidenceRecall()),
                    score.severityMatched(),
                    score.safetyCompliant(),
                    score.totalScore(),
                    score.passed(),
                    writeJson(score.explanations()),
                    null,
                    startedAt,
                    LocalDateTime.now()
            );
        } catch (RuntimeException exception) {
            String failureCode = failureCode(exception);
            log.warn("SRE RCA 单用例评测失败: runId={}, caseId={}, failureCode={}",
                    runId, evaluationCase.getId(), failureCode);
            return new SreRcaEvaluationResultCapture(
                    runId,
                    evaluationCase.getId(),
                    "FAILED",
                    null,
                    analyzer.promptId(),
                    analyzer.schemaId(),
                    safeProvider(null, evaluationCase.getSourceProvider()),
                    evaluationCase.getSourceConfiguredModel(),
                    null,
                    "UNEXPECTED_FAILURE",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    false,
                    false,
                    BigDecimal.ZERO,
                    false,
                    writeJson(List.of("评测执行失败，受控失败码=" + failureCode)),
                    failureCode,
                    startedAt,
                    LocalDateTime.now()
            );
        }
    }

    private void validateCaseIntegrity(SreRcaEvaluationCase evaluationCase, SreRcaReport baseline) {
        String contextJson = evaluationCase.getContextJson();
        if (!StringUtils.hasText(contextJson)
                || contextJson.length() > MAX_CONTEXT_LENGTH
                || evaluationCase.getContextLength() == null
                || evaluationCase.getContextLength() != contextJson.length()
                || !DigestUtil.sha256Hex(contextJson).equalsIgnoreCase(evaluationCase.getContextSha256())) {
            throw new IllegalStateException("RCA 评测用例上下文完整性校验失败");
        }
        if (SreReplayContextPolicy.containsUnsafeContent(contextJson)) {
            throw new IllegalStateException("RCA 评测用例上下文包含未脱敏凭据");
        }
        if (baseline.incidentId() == null
                || !baseline.incidentId().equals(evaluationCase.getIncidentId())) {
            throw new IllegalStateException("RCA 评测用例事故归属校验失败");
        }
    }

    private SreRcaEvaluationCaseResult toCaseResult(SreRcaEvaluationResult result) {
        return new SreRcaEvaluationCaseResult(
                result.getId(),
                result.getEvaluationRunId(),
                result.getCaseId(),
                result.getStatus(),
                readOptionalReport(result.getCandidateReportJson()),
                result.getPromptId(),
                result.getSchemaId(),
                result.getProvider(),
                result.getConfiguredModel(),
                result.getActualModel(),
                result.getInvocationOutcome(),
                valueOrZero(result.getConclusionSimilarity()),
                valueOrZero(result.getEvidenceRecall()),
                Boolean.TRUE.equals(result.getSeverityMatched()),
                Boolean.TRUE.equals(result.getSafetyCompliant()),
                valueOrZero(result.getTotalScore()),
                Boolean.TRUE.equals(result.getPassed()),
                readExplanations(result.getScoreDetailJson()),
                result.getFailureCode(),
                result.getStartedAt(),
                result.getCompletedAt()
        );
    }

    private SreRcaEvaluationCaseSummary toCaseSummary(SreRcaEvaluationCase evaluationCase) {
        return new SreRcaEvaluationCaseSummary(
                evaluationCase.getId(),
                evaluationCase.getIncidentId(),
                evaluationCase.getSourceRunId(),
                evaluationCase.getSourceArtifactId(),
                evaluationCase.getSourceFeedbackId(),
                evaluationCase.getContextSha256(),
                evaluationCase.getContextLength() == null ? 0 : evaluationCase.getContextLength(),
                Boolean.TRUE.equals(evaluationCase.getContextTruncated()),
                evaluationCase.getExpectedConclusion(),
                evaluationCase.getFeedbackAccuracy(),
                evaluationCase.getFeedbackGapType(),
                evaluationCase.getSourcePromptId(),
                evaluationCase.getSourceSchemaId(),
                evaluationCase.getSourceProvider(),
                evaluationCase.getSourceConfiguredModel(),
                evaluationCase.getSourceActualModel(),
                evaluationCase.getSourceInvocationOutcome(),
                evaluationCase.getPromotedBy(),
                evaluationCase.getPromotedAt()
        );
    }

    private SreRcaEvaluationRunSummary toRunSummary(SreRcaEvaluationRun run) {
        return new SreRcaEvaluationRunSummary(
                run.getId(),
                run.getStatus(),
                run.getRequestedCaseId(),
                run.getSuiteVersionId(),
                run.getSuiteKey(),
                run.getSuiteVersion(),
                run.getSuiteManifestSha256(),
                run.getTriggerSource(),
                run.getRequestedBy(),
                intValue(run.getCaseCount()),
                intValue(run.getCompletedCount()),
                intValue(run.getPassedCount()),
                intValue(run.getFailedCount()),
                run.getAverageScore(),
                run.getPassRate(),
                intValue(run.getUnsafeCount()),
                intValue(run.getDegradedCount()),
                run.getPromptId(),
                run.getSchemaId(),
                run.getScoringPolicyId(),
                run.getGateEvaluatorId(),
                run.getGateStatus(),
                run.getGateMinimumPassRate(),
                run.getGateMinimumAverageScore(),
                Boolean.TRUE.equals(run.getGateRequireAllSafety()),
                Boolean.TRUE.equals(run.getGateRequireNoDegraded()),
                readExplanations(run.getGateDetailJson()),
                run.getProvider(),
                run.getConfiguredModel(),
                run.getFailureCode(),
                run.getStartedAt(),
                run.getCompletedAt()
        );
    }

    private SreRcaEvaluationSuiteSummary toSuiteSummary(SreRcaEvaluationSuite suite) {
        return new SreRcaEvaluationSuiteSummary(
                suite.getId(),
                suite.getSuiteKey(),
                suite.getName(),
                suite.getDescription(),
                suite.getCreatedBy(),
                suite.getCreatedAt()
        );
    }

    private SreRcaEvaluationSuiteVersionSummary toSuiteVersionSummary(
            SreRcaEvaluationSuiteVersion version) {
        return new SreRcaEvaluationSuiteVersionSummary(
                version.getId(),
                version.getSuiteId(),
                intValue(version.getVersionNo()),
                intValue(version.getCaseCount()),
                version.getManifestSha256(),
                version.getManifestSchemaId(),
                version.getScoringPolicyId(),
                version.getGateEvaluatorId(),
                version.getMinimumPassRate(),
                version.getMinimumAverageScore(),
                Boolean.TRUE.equals(version.getRequireAllSafety()),
                Boolean.TRUE.equals(version.getRequireNoDegraded()),
                version.getPublishedBy(),
                version.getPublishedAt()
        );
    }

    private SreRcaEvaluationSuiteVersionDetail toSuiteVersionDetail(
            SreRcaEvaluationSuiteVersionSnapshot snapshot) {
        return new SreRcaEvaluationSuiteVersionDetail(
                toSuiteSummary(snapshot.suite()),
                toSuiteVersionSummary(snapshot.version()),
                snapshot.cases().stream().map(this::toCaseSummary).toList()
        );
    }

    private void completeInMemory(SreRcaEvaluationRun run,
                                  String status,
                                  int completedCount,
                                  int passedCount,
                                  int failedCount,
                                  BigDecimal averageScore,
                                  SreRcaEvaluationGateDecision gate,
                                  int unsafeCount,
                                  int degradedCount,
                                  String provider,
                                  String configuredModel) {
        run.setStatus(status);
        run.setCompletedCount(completedCount);
        run.setPassedCount(passedCount);
        run.setFailedCount(failedCount);
        run.setAverageScore(averageScore);
        run.setPassRate(gate.passRate());
        run.setUnsafeCount(unsafeCount);
        run.setDegradedCount(degradedCount);
        run.setGateStatus(gate.status());
        run.setGateDetailJson(writeJson(gate.failureCodes()));
        run.setProvider(provider);
        run.setConfiguredModel(configuredModel);
        run.setCompletedAt(LocalDateTime.now());
    }

    private void markRunFailed(Long runId, RuntimeException exception) {
        try {
            runService.fail(runId, failureCode(exception));
        } catch (RuntimeException failureException) {
            log.warn("SRE RCA 评测失败状态写入失败: runId={}, failureCode={}",
                    runId, failureCode(failureException));
        }
    }

    private SreRcaReport readReport(String json, String label) {
        if (!StringUtils.hasText(json)) {
            throw new IllegalStateException(label + " RCA 报告不存在");
        }
        try {
            return objectMapper.readValue(json, SreRcaReport.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(label + " RCA 报告解析失败", exception);
        }
    }

    private SreRcaReport readOptionalReport(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SreRcaReport.class);
        } catch (JsonProcessingException exception) {
            log.warn("SRE RCA 评测候选报告读取失败: failureCode={}", failureCode(exception));
            return null;
        }
    }

    private List<String> readExplanations(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            log.warn("SRE RCA 评测评分说明读取失败: failureCode={}", failureCode(exception));
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("SRE RCA 评测结果序列化失败", exception);
        }
    }

    private String safeProvider(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        return StringUtils.hasText(fallback) ? fallback.trim() : "unknown";
    }

    private String failureCode(Throwable throwable) {
        String simpleName = throwable == null ? "UNEXPECTED_FAILURE" : throwable.getClass().getSimpleName();
        String normalized = simpleName.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        return normalized.length() <= 128 ? normalized : normalized.substring(0, 128);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private record EvaluationSelection(
            List<SreRcaEvaluationCase> cases,
            SreRcaEvaluationSuite suite,
            SreRcaEvaluationSuiteVersion version
    ) {
    }
}
