package com.xiaou.sre.service.impl;

import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.domain.SreInvestigationStep;
import com.xiaou.sre.mapper.SreInvestigationRunMapper;
import com.xiaou.sre.mapper.SreInvestigationStepMapper;
import com.xiaou.sre.service.SreInvestigationRunService;
import com.xiaou.sre.service.SreValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SRE 调查运行持久化实现。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreInvestigationRunServiceImpl implements SreInvestigationRunService {

    private static final int MAX_HISTORY_LIMIT = 50;
    private static final int MAX_REPORT_LENGTH = 1_000_000;
    private static final int MAX_DETAIL_LENGTH = 500;
    private static final int MAX_FAILURE_CODE_LENGTH = 128;
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private static final Set<String> TERMINAL_STATUSES = Set.of("SUCCEEDED", "DEGRADED");
    private static final Set<String> STEP_STATUSES = Set.of("SUCCEEDED", "DEGRADED", "FAILED", "SKIPPED");

    private final SreInvestigationRunMapper runMapper;
    private final SreInvestigationStepMapper stepMapper;

    @Override
    @Transactional
    public SreInvestigationRun start(Long incidentId,
                                     String triggerSource,
                                     Long requestedBy,
                                     int alertCount,
                                     int evidenceCount,
                                     boolean contextTruncated) {
        requirePositive(incidentId, "事故 ID 不合法");
        SreInvestigationRun run = new SreInvestigationRun();
        run.setIncidentId(incidentId);
        run.setStatus("RUNNING");
        run.setTriggerSource(normalizeCode(triggerSource, "SYSTEM"));
        run.setRequestedBy(requestedBy != null && requestedBy > 0 ? requestedBy : null);
        run.setAlertCount(Math.max(0, alertCount));
        run.setEvidenceCount(Math.max(0, evidenceCount));
        run.setContextTruncated(contextTruncated);
        run.setStartedAt(LocalDateTime.now());
        if (runMapper.insert(run) != 1 || run.getId() == null) {
            throw new IllegalStateException("SRE 调查运行创建失败");
        }
        return run;
    }

    @Override
    @Transactional
    public void recordStep(Long runId, int stepOrder, String stepCode, String status, String detail) {
        requirePositive(runId, "调查运行 ID 不合法");
        if (stepOrder <= 0 || stepOrder > 100) {
            throw new SreValidationException("调查步骤序号不合法");
        }
        String normalizedStatus = normalizeCode(status, "SUCCEEDED");
        if (!STEP_STATUSES.contains(normalizedStatus)) {
            throw new SreValidationException("调查步骤状态不合法");
        }
        SreInvestigationStep step = new SreInvestigationStep();
        step.setRunId(runId);
        step.setStepOrder(stepOrder);
        step.setStepCode(normalizeCode(stepCode, "UNKNOWN"));
        step.setStatus(normalizedStatus);
        step.setDetail(boundedText(detail, MAX_DETAIL_LENGTH));
        step.setRecordedAt(LocalDateTime.now());
        if (stepMapper.insert(step) != 1) {
            throw new IllegalStateException("SRE 调查步骤写入失败");
        }
    }

    @Override
    @Transactional
    public void complete(Long runId,
                         String status,
                         String generationMode,
                         String conclusionStatus,
                         boolean contextTruncated,
                         String reportJson) {
        requirePositive(runId, "调查运行 ID 不合法");
        String normalizedStatus = normalizeCode(status, "DEGRADED");
        if (!TERMINAL_STATUSES.contains(normalizedStatus)) {
            throw new SreValidationException("调查完成状态不合法");
        }
        if (!StringUtils.hasText(reportJson) || reportJson.length() > MAX_REPORT_LENGTH) {
            throw new SreValidationException("调查报告大小不合法");
        }
        int updated = runMapper.updateCompletion(
                runId,
                normalizedStatus,
                normalizeCode(generationMode, "FALLBACK"),
                normalizeCode(conclusionStatus, "INSUFFICIENT_EVIDENCE"),
                contextTruncated,
                reportJson,
                LocalDateTime.now()
        );
        if (updated != 1) {
            throw new SreValidationException("调查运行不存在或已结束");
        }
    }

    @Override
    @Transactional
    public void fail(Long runId, String failureCode) {
        requirePositive(runId, "调查运行 ID 不合法");
        String normalizedFailure = boundedText(
                normalizeCode(failureCode, "UNEXPECTED_FAILURE"), MAX_FAILURE_CODE_LENGTH);
        if (runMapper.updateFailed(runId, normalizedFailure, LocalDateTime.now()) != 1) {
            throw new SreValidationException("调查运行不存在或已结束");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SreInvestigationRun> listByIncidentId(Long incidentId, int limit) {
        if (incidentId == null || incidentId <= 0) {
            return List.of();
        }
        int boundedLimit = Math.max(1, Math.min(limit, MAX_HISTORY_LIMIT));
        List<SreInvestigationRun> runs = runMapper.selectByIncidentId(incidentId, boundedLimit);
        return runs == null ? List.of() : List.copyOf(runs);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SreInvestigationRun> findByIncidentIdAndRunId(Long incidentId, Long runId) {
        if (incidentId == null || incidentId <= 0 || runId == null || runId <= 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(runMapper.selectByIncidentIdAndId(incidentId, runId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SreInvestigationStep> listSteps(Long runId) {
        if (runId == null || runId <= 0) {
            return List.of();
        }
        List<SreInvestigationStep> steps = stepMapper.selectByRunId(runId);
        return steps == null ? List.of() : List.copyOf(steps);
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new SreValidationException(message);
        }
    }

    private String normalizeCode(String value, String fallback) {
        String normalized = StringUtils.hasText(value)
                ? value.trim().toUpperCase(Locale.ROOT)
                : fallback;
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new SreValidationException("调查元数据编码不合法");
        }
        return normalized;
    }

    private String boundedText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
