package com.xiaou.sre.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.xiaou.sre.domain.SreInvestigationArtifact;
import com.xiaou.sre.domain.SreInvestigationFeedback;
import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.domain.SreRcaEvaluationCase;
import com.xiaou.sre.mapper.SreInvestigationArtifactMapper;
import com.xiaou.sre.mapper.SreInvestigationFeedbackMapper;
import com.xiaou.sre.mapper.SreInvestigationRunMapper;
import com.xiaou.sre.mapper.SreRcaEvaluationCaseMapper;
import com.xiaou.sre.service.SreRcaEvaluationCaseService;
import com.xiaou.sre.service.SreReplayContextPolicy;
import com.xiaou.sre.service.SreValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 从已完成运行、指定反馈修订和已脱敏 artifact 构建不可变评测用例。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreRcaEvaluationCaseServiceImpl implements SreRcaEvaluationCaseService {

    private static final int MAX_CONTEXT_LENGTH = 60_000;
    private static final int MAX_REPORT_LENGTH = 1_000_000;
    private static final int MAX_HISTORY_LIMIT = 100;
    private static final Set<String> PROMOTABLE_RUN_STATUSES = Set.of("SUCCEEDED", "DEGRADED");

    private final SreInvestigationRunMapper runMapper;
    private final SreInvestigationArtifactMapper artifactMapper;
    private final SreInvestigationFeedbackMapper feedbackMapper;
    private final SreRcaEvaluationCaseMapper caseMapper;

    @Override
    @Transactional
    public Optional<SreRcaEvaluationCase> promote(Long incidentId,
                                                  Long runId,
                                                  Long feedbackId,
                                                  Long promotedBy) {
        requirePositive(incidentId, "事故 ID 不合法");
        requirePositive(runId, "调查运行 ID 不合法");
        requirePositive(feedbackId, "反馈修订 ID 不合法");
        requirePositive(promotedBy, "提升管理员 ID 不合法");

        SreInvestigationRun run = runMapper.selectByIncidentIdAndId(incidentId, runId);
        if (run == null) {
            return Optional.empty();
        }
        validateRun(run);

        SreInvestigationArtifact artifact = artifactMapper.selectByIncidentIdAndRunId(incidentId, runId);
        if (artifact == null) {
            throw new SreValidationException("RCA 运行缺少可回放的脱敏 artifact");
        }
        validateArtifact(artifact);

        SreInvestigationFeedback feedback = feedbackMapper.selectByIdAndRunId(feedbackId, runId);
        if (feedback == null) {
            return Optional.empty();
        }

        SreRcaEvaluationCase existing = caseMapper.selectBySourceFeedbackId(feedbackId);
        if (existing != null) {
            return Optional.of(existing);
        }

        SreRcaEvaluationCase evaluationCase = buildCase(
                incidentId, run, artifact, feedback, promotedBy, LocalDateTime.now());
        try {
            if (caseMapper.insert(evaluationCase) != 1 || evaluationCase.getId() == null) {
                throw new IllegalStateException("SRE RCA 评测用例写入失败");
            }
            return Optional.of(evaluationCase);
        } catch (DuplicateKeyException duplicateKeyException) {
            SreRcaEvaluationCase raced = caseMapper.selectBySourceFeedbackId(feedbackId);
            if (raced == null) {
                throw duplicateKeyException;
            }
            return Optional.of(raced);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SreRcaEvaluationCase> findById(Long caseId) {
        if (caseId == null || caseId <= 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(caseMapper.selectById(caseId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SreRcaEvaluationCase> listRecent(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, MAX_HISTORY_LIMIT));
        List<SreRcaEvaluationCase> cases = caseMapper.selectRecent(boundedLimit);
        return cases == null ? List.of() : List.copyOf(cases);
    }

    private SreRcaEvaluationCase buildCase(Long incidentId,
                                           SreInvestigationRun run,
                                           SreInvestigationArtifact artifact,
                                           SreInvestigationFeedback feedback,
                                           Long promotedBy,
                                           LocalDateTime promotedAt) {
        SreRcaEvaluationCase evaluationCase = new SreRcaEvaluationCase();
        evaluationCase.setIncidentId(incidentId);
        evaluationCase.setSourceRunId(run.getId());
        evaluationCase.setSourceArtifactId(artifact.getId());
        evaluationCase.setSourceFeedbackId(feedback.getId());
        evaluationCase.setContextJson(artifact.getContextJson());
        evaluationCase.setContextSha256(artifact.getContextSha256());
        evaluationCase.setContextLength(artifact.getContextLength());
        evaluationCase.setContextTruncated(Boolean.TRUE.equals(artifact.getContextTruncated()));
        evaluationCase.setBaselineReportJson(run.getReportJson());
        evaluationCase.setExpectedConclusion(feedback.getExpectedConclusion());
        evaluationCase.setFeedbackAccuracy(feedback.getAccuracy());
        evaluationCase.setFeedbackGapType(feedback.getGapType());
        evaluationCase.setSourcePromptId(artifact.getPromptId());
        evaluationCase.setSourceSchemaId(artifact.getSchemaId());
        evaluationCase.setSourceProvider(artifact.getProvider());
        evaluationCase.setSourceConfiguredModel(artifact.getConfiguredModel());
        evaluationCase.setSourceActualModel(artifact.getActualModel());
        evaluationCase.setSourceInvocationOutcome(artifact.getInvocationOutcome());
        evaluationCase.setPromotedBy(promotedBy);
        evaluationCase.setPromotedAt(promotedAt);
        return evaluationCase;
    }

    private void validateRun(SreInvestigationRun run) {
        if (!PROMOTABLE_RUN_STATUSES.contains(run.getStatus())
                || !StringUtils.hasText(run.getReportJson())
                || run.getReportJson().length() > MAX_REPORT_LENGTH) {
            throw new SreValidationException("RCA 运行尚未形成可评测报告");
        }
    }

    private void validateArtifact(SreInvestigationArtifact artifact) {
        String contextJson = artifact.getContextJson();
        if (!StringUtils.hasText(contextJson)
                || contextJson.length() > MAX_CONTEXT_LENGTH
                || artifact.getContextLength() == null
                || artifact.getContextLength() != contextJson.length()
                || !StringUtils.hasText(artifact.getContextSha256())
                || !DigestUtil.sha256Hex(contextJson).equalsIgnoreCase(artifact.getContextSha256())) {
            throw new SreValidationException("RCA 回放 artifact 完整性校验失败");
        }
        if (SreReplayContextPolicy.containsUnsafeContent(contextJson)) {
            throw new SreValidationException("RCA 回放 artifact 包含未脱敏凭据");
        }
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new SreValidationException(message);
        }
    }
}
