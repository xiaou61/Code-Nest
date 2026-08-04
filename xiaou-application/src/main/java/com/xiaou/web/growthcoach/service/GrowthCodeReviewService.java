package com.xiaou.web.growthcoach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.dto.codereview.CodePenReviewResult;
import com.xiaou.ai.service.AiCodeReviewService;
import com.xiaou.codepen.domain.CodePen;
import com.xiaou.codepen.mapper.CodePenMapper;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import com.xiaou.web.growthcoach.domain.GrowthCodeReviewRecord;
import com.xiaou.web.growthcoach.dto.GrowthCodeReviewResponse;
import com.xiaou.web.growthcoach.mapper.GrowthCodeReviewRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 用户自有 CodePen 的审查编排。
 *
 * <p>模型只返回结构化建议；源码所有权、大小限制、哈希、持久化和证据投影均由服务端控制。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthCodeReviewService {

    private static final String STATUS_COMPLETED = "COMPLETED";

    private final CodePenMapper codePenMapper;
    private final GrowthCodeReviewRecordMapper reviewRecordMapper;
    private final AiCodeReviewService aiCodeReviewService;
    private final GrowthCoachProperties properties;
    private final GrowthCoachRateLimiter rateLimiter;
    private final GrowthEvidenceProjectorService projectorService;
    private final ObjectMapper objectMapper;

    public GrowthCodeReviewResponse review(Long userId, Long codePenId) {
        requireEnabled();
        rateLimiter.checkCodeReview(userId);
        CodePen pen = requireOwnedPen(userId, codePenId);
        String source = sourceContent(pen);
        ensureSourceSize(source);
        String sourceHash = sha256(source);
        GrowthCodeReviewRecord latest = reviewRecordMapper.selectLatestByUserAndPen(userId, codePenId);
        if (latest != null && sourceHash.equals(latest.getSourceHash())) {
            GrowthCodeReviewResponse reused = toResponse(latest, sourceHash);
            reused.setReused(true);
            return reused;
        }

        CodePenReviewResult result = aiCodeReviewService.reviewCodePen(
                safeText(pen.getTitle(), "未命名 CodePen"), pen.getHtmlCode(), pen.getCssCode(), pen.getJsCode());
        if (result == null || result.isFallback()) {
            throw new BusinessException("AI 代码审查暂不可用，请稍后重试");
        }

        LocalDateTime reviewedAt = LocalDateTime.now();
        GrowthCodeReviewRecord record = new GrowthCodeReviewRecord();
        record.setUserId(userId);
        record.setCodePenId(codePenId);
        record.setPreviousReviewId(latest == null ? null : latest.getId());
        record.setSourceHash(sourceHash);
        record.setScore(clampScore(result.getScore()));
        record.setCriticalFindingCount(countSeverity(result, "CRITICAL"));
        record.setHighFindingCount(countSeverity(result, "HIGH"));
        record.setMediumFindingCount(countSeverity(result, "MEDIUM"));
        record.setSummary(limitText(result.getSummary(), 200));
        record.setResultJson(writeResult(result));
        record.setStatus(STATUS_COMPLETED);
        record.setSourceObservedAt(sourceObservedAt(pen, reviewedAt));
        record.setReviewedAt(reviewedAt);
        reviewRecordMapper.insert(record);
        refreshEvidence(userId);
        return toResponse(record, sourceHash);
    }

    public GrowthCodeReviewResponse getLatest(Long userId, Long codePenId) {
        CodePen pen = requireOwnedPen(userId, codePenId);
        GrowthCodeReviewRecord record = reviewRecordMapper.selectLatestByUserAndPen(userId, codePenId);
        return record == null ? null : toResponse(record, sha256(sourceContent(pen)));
    }

    public void delete(Long userId, Long reviewId) {
        if (userId == null || userId <= 0 || reviewId == null || reviewId <= 0
                || reviewRecordMapper.logicalDelete(reviewId, userId) != 1) {
            throw new BusinessException("代码审查记录不存在");
        }
        refreshEvidence(userId);
    }

    private CodePen requireOwnedPen(Long userId, Long codePenId) {
        if (userId == null || userId <= 0 || codePenId == null || codePenId <= 0) {
            throw new BusinessException("代码作品不存在");
        }
        CodePen pen = codePenMapper.selectById(codePenId);
        if (pen == null || !Objects.equals(userId, pen.getUserId()) || Integer.valueOf(3).equals(pen.getStatus())) {
            throw new BusinessException("代码作品不存在");
        }
        if (!StringUtils.hasText(pen.getHtmlCode())
                && !StringUtils.hasText(pen.getCssCode())
                && !StringUtils.hasText(pen.getJsCode())) {
            throw new BusinessException("请先保存至少一种代码后再发起审查");
        }
        return pen;
    }

    private void requireEnabled() {
        GrowthCoachProperties.CodeReview config = properties.getCodeReview();
        if (config == null || !config.isEnabled()) {
            throw new BusinessException("CodePen AI 审查暂未开放");
        }
    }

    private String sourceContent(CodePen pen) {
        return "HTML\n" + defaultText(pen.getHtmlCode())
                + "\nCSS\n" + defaultText(pen.getCssCode())
                + "\nJAVASCRIPT\n" + defaultText(pen.getJsCode());
    }

    private void ensureSourceSize(String source) {
        GrowthCoachProperties.CodeReview config = properties.getCodeReview();
        int configured = config == null ? 24000 : config.getMaxSourceChars();
        int max = Math.max(1000, Math.min(configured, 60000));
        if (source.length() > max) {
            throw new BusinessException("当前 CodePen 源码过长，请拆分后再发起审查");
        }
    }

    private GrowthCodeReviewResponse toResponse(GrowthCodeReviewRecord record, String currentSourceHash) {
        CodePenReviewResult result = readResult(record);
        GrowthCodeReviewResponse response = new GrowthCodeReviewResponse();
        response.setId(record.getId());
        response.setPenId(record.getCodePenId());
        response.setPreviousReviewId(record.getPreviousReviewId());
        response.setScore(record.getScore());
        response.setCriticalFindingCount(nvl(record.getCriticalFindingCount()));
        response.setHighFindingCount(nvl(record.getHighFindingCount()));
        response.setMediumFindingCount(nvl(record.getMediumFindingCount()));
        response.setSummary(safeText(record.getSummary(), "已完成当前已保存版本的代码审查。"));
        response.setStatus(record.getStatus());
        response.setSourceObservedAt(record.getSourceObservedAt());
        response.setReviewedAt(record.getReviewedAt());
        response.setSourceChanged(!Objects.equals(record.getSourceHash(), currentSourceHash));
        response.setFindings(toFindings(result));
        response.setActionItems(toActionItems(result));
        response.setScoreDelta(scoreDelta(record));
        return response;
    }

    private Integer scoreDelta(GrowthCodeReviewRecord record) {
        if (record.getPreviousReviewId() == null) {
            return null;
        }
        GrowthCodeReviewRecord previous = reviewRecordMapper.selectByIdAndUser(record.getPreviousReviewId(), record.getUserId());
        if (previous == null || previous.getScore() == null || record.getScore() == null) {
            return null;
        }
        return record.getScore() - previous.getScore();
    }

    private List<GrowthCodeReviewResponse.Finding> toFindings(CodePenReviewResult result) {
        List<GrowthCodeReviewResponse.Finding> values = new ArrayList<>();
        for (CodePenReviewResult.Finding source : safeFindings(result)) {
            GrowthCodeReviewResponse.Finding target = new GrowthCodeReviewResponse.Finding();
            target.setSeverity(source.getSeverity());
            target.setArea(source.getArea());
            target.setTitle(source.getTitle());
            target.setDescription(source.getDescription());
            target.setRecommendedAction(source.getRecommendedAction());
            values.add(target);
        }
        return values;
    }

    private List<GrowthCodeReviewResponse.ActionItem> toActionItems(CodePenReviewResult result) {
        List<GrowthCodeReviewResponse.ActionItem> values = new ArrayList<>();
        if (result == null || result.getActionItems() == null) {
            return values;
        }
        for (CodePenReviewResult.ActionItem source : result.getActionItems()) {
            if (source == null) {
                continue;
            }
            GrowthCodeReviewResponse.ActionItem target = new GrowthCodeReviewResponse.ActionItem();
            target.setTitle(source.getTitle());
            target.setDescription(source.getDescription());
            target.setVerification(source.getVerification());
            values.add(target);
        }
        return values;
    }

    private CodePenReviewResult readResult(GrowthCodeReviewRecord record) {
        if (record == null || !StringUtils.hasText(record.getResultJson())) {
            return new CodePenReviewResult();
        }
        try {
            return objectMapper.readValue(record.getResultJson(), CodePenReviewResult.class);
        } catch (JsonProcessingException exception) {
            log.warn("解析 CodePen 审查记录失败，reviewId={}", record.getId());
            return new CodePenReviewResult();
        }
    }

    private String writeResult(CodePenReviewResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("代码审查结果序列化失败", exception);
        }
    }

    private int countSeverity(CodePenReviewResult result, String severity) {
        return (int) safeFindings(result).stream()
                .filter(item -> severity.equalsIgnoreCase(item.getSeverity()))
                .count();
    }

    private List<CodePenReviewResult.Finding> safeFindings(CodePenReviewResult result) {
        return result == null || result.getFindings() == null ? List.of() : result.getFindings().stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private LocalDateTime sourceObservedAt(CodePen pen, LocalDateTime fallback) {
        if (pen.getUpdateTime() == null) {
            return fallback;
        }
        return LocalDateTime.ofInstant(pen.getUpdateTime().toInstant(), ZoneId.systemDefault());
    }

    private int clampScore(Integer value) {
        return Math.max(0, Math.min(value == null ? 0 : value, 100));
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String limitText(String value, int maxLength) {
        String normalized = safeText(value, "已完成当前已保存版本的代码审查。");
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private void refreshEvidence(Long userId) {
        try {
            projectorService.refreshForUser(userId);
        } catch (RuntimeException exception) {
            log.warn("CodePen 审查证据投影延后处理: {}", exception.getClass().getSimpleName());
        }
    }
}
