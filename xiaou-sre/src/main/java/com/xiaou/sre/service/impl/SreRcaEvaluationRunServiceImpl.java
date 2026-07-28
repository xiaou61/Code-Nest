package com.xiaou.sre.service.impl;

import com.xiaou.sre.domain.SreRcaEvaluationResult;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.dto.request.SreRcaEvaluationResultCapture;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunCompletion;
import com.xiaou.sre.dto.request.SreRcaEvaluationRunStart;
import com.xiaou.sre.mapper.SreRcaEvaluationCaseMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationResultMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationRunMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationSuiteCaseMapper;
import com.xiaou.sre.service.SreRcaEvaluationRunService;
import com.xiaou.sre.service.SreValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * RCA 评测运行持久化实现。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreRcaEvaluationRunServiceImpl implements SreRcaEvaluationRunService {

    private static final int MAX_CASES_PER_RUN = 100;
    private static final int MAX_HISTORY_LIMIT = 50;
    private static final int MAX_REPORT_LENGTH = 1_000_000;
    private static final int MAX_SCORE_DETAIL_LENGTH = 10_000;
    private static final int MAX_GATE_DETAIL_LENGTH = 5_000;
    private static final Set<String> RESULT_STATUSES = Set.of("SUCCEEDED", "DEGRADED", "FAILED");
    private static final Set<String> RUN_TERMINAL_STATUSES = Set.of("SUCCEEDED", "DEGRADED");
    private static final Set<String> TRIGGER_SOURCES = Set.of("MANUAL", "CI");
    private static final Set<String> GATE_TERMINAL_STATUSES = Set.of(
            "NOT_APPLICABLE", "PASSED", "FAILED");
    private static final Set<String> INVOCATION_OUTCOMES = Set.of(
            "SUCCESS", "MODEL_UNAVAILABLE", "EMPTY_RESPONSE", "INVOCATION_EXCEPTION",
            "PARSER_FAILURE", "UNEXPECTED_FAILURE");
    private static final Pattern FAILURE_CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");
    private static final Pattern SHA256_PATTERN = Pattern.compile("[a-fA-F0-9]{64}");

    private final SreRcaEvaluationRunMapper runMapper;
    private final SreRcaEvaluationResultMapper resultMapper;
    private final SreRcaEvaluationCaseMapper caseMapper;
    private final SreRcaEvaluationSuiteCaseMapper suiteCaseMapper;

    @Override
    @Transactional
    public SreRcaEvaluationRun start(SreRcaEvaluationRunStart start) {
        if (start == null) {
            throw new SreValidationException("评测运行启动参数不能为空");
        }
        if (start.requestedCaseId() != null && start.requestedCaseId() <= 0) {
            throw new SreValidationException("指定评测用例 ID 不合法");
        }
        if (start.requestedCaseId() != null && start.suiteVersionId() != null) {
            throw new SreValidationException("单用例与套件版本不能同时指定");
        }
        requirePositive(start.requestedBy(), "评测发起管理员 ID 不合法");
        if (start.caseCount() <= 0 || start.caseCount() > MAX_CASES_PER_RUN) {
            throw new SreValidationException("评测用例数量不合法");
        }

        SreRcaEvaluationRun run = new SreRcaEvaluationRun();
        run.setStatus("RUNNING");
        run.setRequestedCaseId(start.requestedCaseId());
        run.setSuiteVersionId(start.suiteVersionId());
        run.setTriggerSource(normalizedIn(start.triggerSource(), TRIGGER_SOURCES, "评测触发来源不合法"));
        run.setRequestedBy(start.requestedBy());
        run.setCaseCount(start.caseCount());
        run.setCompletedCount(0);
        run.setPassedCount(0);
        run.setFailedCount(0);
        run.setUnsafeCount(0);
        run.setDegradedCount(0);
        run.setPromptId(requiredText(start.promptId(), 128, "评测 Prompt ID 不合法"));
        run.setSchemaId(requiredText(start.schemaId(), 255, "评测 Schema ID 不合法"));
        if (start.suiteVersionId() == null) {
            run.setGateStatus("NOT_APPLICABLE");
        } else {
            initializeSuiteSnapshot(run, start);
        }
        run.setStartedAt(LocalDateTime.now());
        if (runMapper.insert(run) != 1 || run.getId() == null) {
            throw new IllegalStateException("SRE RCA 评测运行创建失败");
        }
        return run;
    }

    @Override
    @Transactional
    public SreRcaEvaluationResult record(SreRcaEvaluationResultCapture capture) {
        if (capture == null) {
            throw new SreValidationException("评测结果不能为空");
        }
        requirePositive(capture.runId(), "评测运行 ID 不合法");
        requirePositive(capture.caseId(), "评测用例 ID 不合法");
        SreRcaEvaluationRun run = runMapper.selectById(capture.runId());
        if (run == null || !"RUNNING".equals(run.getStatus())) {
            throw new SreValidationException("评测运行不存在或已结束");
        }
        if (caseMapper.selectById(capture.caseId()) == null) {
            throw new SreValidationException("评测用例不存在");
        }
        validateRunMembership(run, capture.caseId());

        String status = normalizedIn(capture.status(), RESULT_STATUSES, "评测结果状态不合法");
        String reportJson = optionalText(capture.candidateReportJson(), MAX_REPORT_LENGTH, "候选报告大小不合法");
        if (!"FAILED".equals(status) && !StringUtils.hasText(reportJson)) {
            throw new SreValidationException("完成的评测结果必须保存候选报告");
        }
        String invocationOutcome = normalizedIn(
                capture.invocationOutcome(), INVOCATION_OUTCOMES, "模型调用结果不合法");
        BigDecimal conclusionSimilarity = ratio(capture.conclusionSimilarity(), "结论相似度不合法");
        BigDecimal evidenceRecall = ratio(capture.evidenceRecall(), "证据召回率不合法");
        BigDecimal totalScore = score(capture.totalScore());
        boolean passed = "SUCCEEDED".equals(status) && capture.passed();
        String failureCode = optionalFailureCode(capture.failureCode());
        if ("FAILED".equals(status) && failureCode == null) {
            throw new SreValidationException("失败评测结果必须提供受控失败码");
        }

        LocalDateTime startedAt = capture.startedAt() == null ? LocalDateTime.now() : capture.startedAt();
        LocalDateTime completedAt = capture.completedAt() == null ? LocalDateTime.now() : capture.completedAt();
        if (completedAt.isBefore(startedAt)) {
            throw new SreValidationException("评测结果时间范围不合法");
        }

        SreRcaEvaluationResult result = new SreRcaEvaluationResult();
        result.setEvaluationRunId(capture.runId());
        result.setCaseId(capture.caseId());
        result.setStatus(status);
        result.setCandidateReportJson(reportJson);
        result.setPromptId(requiredText(capture.promptId(), 128, "评测 Prompt ID 不合法"));
        result.setSchemaId(requiredText(capture.schemaId(), 255, "评测 Schema ID 不合法"));
        result.setProvider(requiredText(capture.provider(), 64, "AI provider 不合法"));
        result.setConfiguredModel(optionalText(capture.configuredModel(), 128, "配置模型名称不合法"));
        result.setActualModel(optionalText(capture.actualModel(), 128, "实际模型名称不合法"));
        result.setInvocationOutcome(invocationOutcome);
        result.setConclusionSimilarity(conclusionSimilarity);
        result.setEvidenceRecall(evidenceRecall);
        result.setSeverityMatched(capture.severityMatched());
        result.setSafetyCompliant(capture.safetyCompliant());
        result.setTotalScore(totalScore);
        result.setPassed(passed);
        result.setScoreDetailJson(optionalText(
                capture.scoreDetailJson(), MAX_SCORE_DETAIL_LENGTH, "评分说明大小不合法"));
        result.setFailureCode(failureCode);
        result.setStartedAt(startedAt);
        result.setCompletedAt(completedAt);
        if (resultMapper.insert(result) != 1 || result.getId() == null) {
            throw new IllegalStateException("SRE RCA 单用例评测结果写入失败");
        }
        return result;
    }

    @Override
    @Transactional
    public void complete(SreRcaEvaluationRunCompletion completion) {
        if (completion == null) {
            throw new SreValidationException("评测完成参数不能为空");
        }
        requirePositive(completion.runId(), "评测运行 ID 不合法");
        SreRcaEvaluationRun run = runMapper.selectById(completion.runId());
        if (run == null || !"RUNNING".equals(run.getStatus())) {
            throw new SreValidationException("评测运行不存在或已结束");
        }
        int persistedCount = resultMapper.countByRunId(run.getId());
        if (completion.completedCount() != run.getCaseCount()
                || persistedCount != completion.completedCount()
                || completion.passedCount() < 0
                || completion.failedCount() < 0
                || completion.passedCount() + completion.failedCount() != completion.completedCount()
                || completion.unsafeCount() < 0
                || completion.degradedCount() < 0
                || completion.unsafeCount() > completion.completedCount()
                || completion.degradedCount() > completion.completedCount()) {
            throw new SreValidationException("评测运行计数不一致");
        }
        String gateStatus = normalizedIn(
                completion.gateStatus(), GATE_TERMINAL_STATUSES, "质量门禁状态不合法");
        if ((run.getSuiteVersionId() == null && !"NOT_APPLICABLE".equals(gateStatus))
                || (run.getSuiteVersionId() != null && "NOT_APPLICABLE".equals(gateStatus))) {
            throw new SreValidationException("质量门禁状态与运行类型不一致");
        }

        run.setStatus(normalizedIn(
                completion.status(), RUN_TERMINAL_STATUSES, "评测完成状态不合法"));
        run.setCompletedCount(completion.completedCount());
        run.setPassedCount(completion.passedCount());
        run.setFailedCount(completion.failedCount());
        run.setAverageScore(score(completion.averageScore()));
        run.setPassRate(percentage(completion.passRate(), "评测通过率不合法"));
        run.setUnsafeCount(completion.unsafeCount());
        run.setDegradedCount(completion.degradedCount());
        run.setGateStatus(gateStatus);
        run.setGateDetailJson(optionalText(
                completion.gateDetailJson(), MAX_GATE_DETAIL_LENGTH, "质量门禁说明大小不合法"));
        run.setProvider(requiredText(completion.provider(), 64, "AI provider 不合法"));
        run.setConfiguredModel(optionalText(
                completion.configuredModel(), 128, "配置模型名称不合法"));
        run.setCompletedAt(LocalDateTime.now());
        if (runMapper.updateCompletion(run) != 1) {
            throw new SreValidationException("评测运行不存在或已结束");
        }
    }

    @Override
    @Transactional
    public void fail(Long runId, String failureCode) {
        requirePositive(runId, "评测运行 ID 不合法");
        String normalizedFailure = requiredFailureCode(failureCode);
        if (runMapper.updateFailed(runId, normalizedFailure, LocalDateTime.now()) != 1) {
            throw new SreValidationException("评测运行不存在或已结束");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SreRcaEvaluationRun> findById(Long runId) {
        if (runId == null || runId <= 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(runMapper.selectById(runId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SreRcaEvaluationRun> listRecent(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, MAX_HISTORY_LIMIT));
        List<SreRcaEvaluationRun> runs = runMapper.selectRecent(boundedLimit);
        return runs == null ? List.of() : List.copyOf(runs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SreRcaEvaluationResult> listResults(Long runId) {
        if (runId == null || runId <= 0 || runMapper.selectById(runId) == null) {
            return List.of();
        }
        List<SreRcaEvaluationResult> results = resultMapper.selectByRunId(runId);
        return results == null ? List.of() : List.copyOf(results);
    }

    private void initializeSuiteSnapshot(SreRcaEvaluationRun run, SreRcaEvaluationRunStart start) {
        requirePositive(start.suiteVersionId(), "评测套件版本 ID 不合法");
        if (start.suiteVersion() == null || start.suiteVersion() <= 0) {
            throw new SreValidationException("评测套件版本号不合法");
        }
        String manifest = requiredText(
                start.suiteManifestSha256(), 64, "评测套件 manifest 不合法");
        if (!SHA256_PATTERN.matcher(manifest).matches()) {
            throw new SreValidationException("评测套件 manifest 不合法");
        }
        if (suiteCaseMapper.countBySuiteVersionId(start.suiteVersionId()) != start.caseCount()) {
            throw new SreValidationException("评测套件版本成员数量不一致");
        }
        if (start.gateRequireAllSafety() == null || start.gateRequireNoDegraded() == null) {
            throw new SreValidationException("质量门禁策略不完整");
        }

        run.setSuiteKey(requiredText(start.suiteKey(), 64, "评测套件 Key 不合法"));
        run.setSuiteVersion(start.suiteVersion());
        run.setSuiteManifestSha256(manifest.toLowerCase(Locale.ROOT));
        run.setScoringPolicyId(requiredText(
                start.scoringPolicyId(), 128, "评分策略 ID 不合法"));
        run.setGateEvaluatorId(requiredText(
                start.gateEvaluatorId(), 128, "质量门禁评估器 ID 不合法"));
        run.setGateMinimumPassRate(percentage(
                start.gateMinimumPassRate(), "质量门禁最低通过率不合法"));
        run.setGateMinimumAverageScore(percentage(
                start.gateMinimumAverageScore(), "质量门禁最低平均分不合法"));
        run.setGateRequireAllSafety(start.gateRequireAllSafety());
        run.setGateRequireNoDegraded(start.gateRequireNoDegraded());
        run.setGateStatus("PENDING");
    }

    private void validateRunMembership(SreRcaEvaluationRun run, Long caseId) {
        if (run.getSuiteVersionId() != null
                && suiteCaseMapper.countBySuiteVersionIdAndCaseId(run.getSuiteVersionId(), caseId) != 1) {
            throw new SreValidationException("评测用例不属于本次套件版本");
        }
        if (run.getRequestedCaseId() != null && !run.getRequestedCaseId().equals(caseId)) {
            throw new SreValidationException("评测用例不属于本次单用例运行");
        }
    }

    private BigDecimal ratio(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new SreValidationException(message);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal score(BigDecimal value) {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new SreValidationException("评测总分不合法");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal value, String message) {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new SreValidationException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizedIn(String value, Set<String> allowed, String message) {
        String normalized = requiredText(value, 32, message).toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new SreValidationException(message);
        }
        return normalized;
    }

    private String requiredFailureCode(String value) {
        String normalized = optionalFailureCode(value);
        if (normalized == null) {
            throw new SreValidationException("评测失败码不合法");
        }
        return normalized;
    }

    private String optionalFailureCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        if (!FAILURE_CODE_PATTERN.matcher(normalized).matches()) {
            throw new SreValidationException("评测失败码不合法");
        }
        return normalized;
    }

    private String requiredText(String value, int maxLength, String message) {
        String normalized = optionalText(value, maxLength, message);
        if (!StringUtils.hasText(normalized)) {
            throw new SreValidationException(message);
        }
        return normalized;
    }

    private String optionalText(String value, int maxLength, String message) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
        if (!StringUtils.hasText(normalized) || normalized.length() > maxLength) {
            throw new SreValidationException(message);
        }
        return normalized;
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new SreValidationException(message);
        }
    }
}
