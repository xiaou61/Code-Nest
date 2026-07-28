package com.xiaou.system.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.domain.SreRcaEvaluationResult;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.dto.request.SreRcaEvaluationResultCapture;
import com.xiaou.sre.service.SreRcaEvaluationCaseService;
import com.xiaou.sre.service.SreRcaEvaluationRunService;
import com.xiaou.sre.service.SreReplayContextPolicy;
import com.xiaou.sre.service.SreValidationException;
import com.xiaou.system.dto.SreRcaEvaluationCaseResult;
import com.xiaou.system.dto.SreRcaEvaluationCaseSummary;
import com.xiaou.system.dto.SreRcaEvaluationRunDetail;
import com.xiaou.system.dto.SreRcaEvaluationRunSummary;
import com.xiaou.system.dto.SreRcaEvaluationScore;
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
    private final SreRcaAnalyzer analyzer;
    private final SreRcaEvaluationScorer scorer;
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
    public SreRcaEvaluationRunDetail run(Long caseId, Long requestedBy) {
        if (requestedBy == null || requestedBy <= 0) {
            throw new IllegalArgumentException("评测发起管理员 ID 不合法");
        }
        List<SreRcaEvaluationCase> cases = loadCases(caseId);
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("当前没有可回放的 RCA 评测用例");
        }

        SreRcaEvaluationRun run = runService.start(
                caseId,
                requestedBy,
                cases.size(),
                analyzer.promptId(),
                analyzer.schemaId()
        );
        List<SreRcaEvaluationResult> persistedResults = new ArrayList<>();
        int passedCount = 0;
        BigDecimal scoreTotal = BigDecimal.ZERO;
        boolean degraded = false;
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
                scoreTotal = scoreTotal.add(valueOrZero(persisted.getTotalScore()));
                degraded = degraded || !"SUCCEEDED".equals(persisted.getStatus());
                if (!StringUtils.hasText(provider)) {
                    provider = persisted.getProvider();
                    configuredModel = persisted.getConfiguredModel();
                }
            }

            int completedCount = persistedResults.size();
            int failedCount = completedCount - passedCount;
            BigDecimal averageScore = scoreTotal
                    .divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP);
            String terminalStatus = degraded ? "DEGRADED" : "SUCCEEDED";
            String resolvedProvider = StringUtils.hasText(provider) ? provider : "unknown";
            runService.complete(
                    run.getId(),
                    terminalStatus,
                    completedCount,
                    passedCount,
                    failedCount,
                    averageScore,
                    resolvedProvider,
                    configuredModel
            );
            completeInMemory(
                    run, terminalStatus, completedCount, passedCount, failedCount,
                    averageScore, resolvedProvider, configuredModel);
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

    private List<SreRcaEvaluationCase> loadCases(Long caseId) {
        if (caseId != null) {
            if (caseId <= 0) {
                throw new IllegalArgumentException("评测用例 ID 不合法");
            }
            return caseService.findById(caseId).map(List::of).orElseGet(List::of);
        }
        return caseService.listRecent(MAX_CASES_PER_RUN).stream()
                .map(item -> caseService.findById(item.getId())
                        .orElseThrow(() -> new IllegalStateException("RCA 评测用例读取失败")))
                .toList();
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
                run.getRequestedBy(),
                intValue(run.getCaseCount()),
                intValue(run.getCompletedCount()),
                intValue(run.getPassedCount()),
                intValue(run.getFailedCount()),
                run.getAverageScore(),
                run.getPromptId(),
                run.getSchemaId(),
                run.getProvider(),
                run.getConfiguredModel(),
                run.getFailureCode(),
                run.getStartedAt(),
                run.getCompletedAt()
        );
    }

    private void completeInMemory(SreRcaEvaluationRun run,
                                  String status,
                                  int completedCount,
                                  int passedCount,
                                  int failedCount,
                                  BigDecimal averageScore,
                                  String provider,
                                  String configuredModel) {
        run.setStatus(status);
        run.setCompletedCount(completedCount);
        run.setPassedCount(passedCount);
        run.setFailedCount(failedCount);
        run.setAverageScore(averageScore);
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
}
