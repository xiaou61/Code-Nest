package com.xiaou.sre.service.impl;

import com.xiaou.sre.domain.SreInvestigationFeedback;
import com.xiaou.sre.mapper.SreInvestigationFeedbackMapper;
import com.xiaou.sre.mapper.SreInvestigationRunMapper;
import com.xiaou.sre.service.SreInvestigationFeedbackService;
import com.xiaou.sre.service.SreValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SRE 调查反馈实现。每次修改均追加新修订，不覆盖历史评价。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreInvestigationFeedbackServiceImpl implements SreInvestigationFeedbackService {

    private static final int MAX_NOTE_LENGTH = 1_000;
    private static final int MAX_EXPECTED_CONCLUSION_LENGTH = 2_000;
    private static final Set<String> ACCURACIES = Set.of("ACCURATE", "PARTIAL", "INACCURATE");
    private static final Set<String> GAP_TYPES = Set.of(
            "RETRIEVAL_GAP", "REASONING_GAP", "TOOL_FAILURE", "ROUTING_GAP", "UNKNOWN");
    private static final Pattern CONTROL_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]{6,}");
    private static final Pattern INLINE_SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(authorization|password|passwd|token|secret|api[-_ ]?key|cookie|credential)"
                    + "\\b\\s*[:=]\\s*([^\\s,;]+)");
    private static final Pattern STANDALONE_CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)\\b(?:sk-[A-Za-z0-9_-]{16,}|gh[pousr]_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16})\\b");

    private final SreInvestigationRunMapper runMapper;
    private final SreInvestigationFeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public Optional<SreInvestigationFeedback> save(Long incidentId,
                                                   Long runId,
                                                   String accuracy,
                                                   String gapType,
                                                   String note,
                                                   String expectedConclusion,
                                                   Long reviewedBy) {
        requirePositive(incidentId, "事故 ID 不合法");
        requirePositive(runId, "调查运行 ID 不合法");
        requirePositive(reviewedBy, "评价人 ID 不合法");
        if (runMapper.selectByIncidentIdAndId(incidentId, runId) == null) {
            return Optional.empty();
        }

        String normalizedAccuracy = normalizeRequiredCode(accuracy, ACCURACIES, "RCA 准确度评价不合法");
        String normalizedGapType = normalizeOptionalCode(gapType, GAP_TYPES, "RCA 缺口类型不合法");
        String sanitizedNote = sanitizeText(note, MAX_NOTE_LENGTH, "管理员备注不能超过1000个字符");
        String sanitizedExpected = sanitizeText(
                expectedConclusion,
                MAX_EXPECTED_CONCLUSION_LENGTH,
                "期望结论不能超过2000个字符"
        );

        if ("ACCURATE".equals(normalizedAccuracy) && normalizedGapType != null) {
            throw new SreValidationException("准确评价不能设置缺口类型");
        }
        if (!"ACCURATE".equals(normalizedAccuracy) && normalizedGapType == null) {
            throw new SreValidationException("部分准确或不准确评价必须设置缺口类型");
        }
        if (!"ACCURATE".equals(normalizedAccuracy) && !StringUtils.hasText(sanitizedExpected)) {
            throw new SreValidationException("部分准确或不准确评价必须填写期望结论");
        }

        SreInvestigationFeedback feedback = new SreInvestigationFeedback();
        feedback.setRunId(runId);
        feedback.setAccuracy(normalizedAccuracy);
        feedback.setGapType(normalizedGapType);
        feedback.setNote(sanitizedNote);
        feedback.setExpectedConclusion(sanitizedExpected);
        feedback.setReviewedBy(reviewedBy);
        feedback.setReviewedAt(LocalDateTime.now());
        if (feedbackMapper.insert(feedback) != 1 || feedback.getId() == null) {
            throw new IllegalStateException("SRE 调查反馈写入失败");
        }
        return Optional.of(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SreInvestigationFeedback> findLatest(Long incidentId, Long runId) {
        if (incidentId == null || incidentId <= 0 || runId == null || runId <= 0) {
            return Optional.empty();
        }
        if (runMapper.selectByIncidentIdAndId(incidentId, runId) == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(feedbackMapper.selectLatestByRunId(runId));
    }

    private String normalizeRequiredCode(String value, Set<String> allowed, String message) {
        if (!StringUtils.hasText(value)) {
            throw new SreValidationException(message);
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new SreValidationException(message);
        }
        return normalized;
    }

    private String normalizeOptionalCode(String value, Set<String> allowed, String message) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new SreValidationException(message);
        }
        return normalized;
    }

    private String sanitizeText(String value, int maxLength, String message) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String sanitized = CONTROL_PATTERN.matcher(value).replaceAll("").trim();
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer [REDACTED]");
        sanitized = INLINE_SECRET_PATTERN.matcher(sanitized).replaceAll("$1=[REDACTED]");
        sanitized = STANDALONE_CREDENTIAL_PATTERN.matcher(sanitized).replaceAll("[REDACTED]");
        if (sanitized.length() > maxLength) {
            throw new SreValidationException(message);
        }
        return StringUtils.hasText(sanitized) ? sanitized : null;
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new SreValidationException(message);
        }
    }
}
