package com.xiaou.web.growthcoach.evidence;

import com.xiaou.web.growthcoach.domain.GrowthCodeReviewRecord;
import com.xiaou.web.growthcoach.mapper.GrowthCodeReviewRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 投影 CodePen AI 审查完成和重新审查事实。
 *
 * <p>模型评估不等同于已验证能力；证据不包含任何代码正文或审查长文本。</p>
 */
@Component
@RequiredArgsConstructor
public class GrowthCodeReviewEvidenceAdapter implements GrowthEvidenceAdapter {

    private static final String SOURCE_KEY = "growth_code_review_record";
    private static final String SOURCE_MODULE = "growth_coach";
    private static final String EVIDENCE_TYPE = "CODE_REVIEW_RESULT";

    private final GrowthCodeReviewRecordMapper reviewRecordMapper;

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public List<GrowthEvidenceProjection> loadChanges(Long userId,
                                                       LocalDateTime afterUpdateTime,
                                                       Long afterSourceId,
                                                       int limit) {
        return reviewRecordMapper.selectChangedForEvidence(userId, afterUpdateTime, afterSourceId, limit).stream()
                .map(this::toProjection)
                .toList();
    }

    private GrowthEvidenceProjection toProjection(GrowthCodeReviewRecord record) {
        LocalDateTime observedAt = firstPresent(record.getReviewedAt(), record.getSourceObservedAt(), record.getCreateTime());
        LocalDateTime updatedAt = firstPresent(record.getUpdateTime(), record.getReviewedAt(), record.getCreateTime());
        GrowthEvidenceProjection projection = new GrowthEvidenceProjection();
        projection.setUserId(record.getUserId());
        projection.setSourceModule(SOURCE_MODULE);
        projection.setSourceType(SOURCE_KEY);
        projection.setSourceId(String.valueOf(record.getId()));
        projection.setSourceRecordId(record.getId());
        projection.setEvidenceType(EVIDENCE_TYPE);
        projection.setSkillKey("codepen_review");
        projection.setQualityLevel("MODEL_ASSESSED");
        projection.setObservedAt(observedAt);
        projection.setSourceUpdatedAt(updatedAt);
        projection.setActive(!Integer.valueOf(1).equals(record.getDeleted())
                && "COMPLETED".equalsIgnoreCase(record.getStatus()));

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("score", record.getScore());
        summary.put("criticalFindingCount", record.getCriticalFindingCount());
        summary.put("highFindingCount", record.getHighFindingCount());
        summary.put("mediumFindingCount", record.getMediumFindingCount());
        summary.put("reReviewed", record.getPreviousReviewId() != null);
        projection.setSummary(summary);
        return projection;
    }

    private LocalDateTime firstPresent(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return LocalDateTime.of(1970, 1, 1, 0, 0);
    }
}
