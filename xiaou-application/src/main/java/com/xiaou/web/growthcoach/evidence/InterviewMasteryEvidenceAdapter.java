package com.xiaou.web.growthcoach.evidence;

import com.xiaou.interview.domain.InterviewMasteryRecord;
import com.xiaou.interview.mapper.InterviewMasteryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 将面试题掌握度投影为可追溯的成长证据，不复制题目和答案正文。
 */
@Component
@RequiredArgsConstructor
public class InterviewMasteryEvidenceAdapter implements GrowthEvidenceAdapter {

    private static final String SOURCE_KEY = "interview_mastery_record";
    private static final String SOURCE_MODULE = "interview";
    private static final String EVIDENCE_TYPE = "QUESTION_MASTERY";

    private final InterviewMasteryMapper masteryMapper;

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public List<GrowthEvidenceProjection> loadChanges(Long userId,
                                                       LocalDateTime afterUpdateTime,
                                                       Long afterSourceId,
                                                       int limit) {
        return masteryMapper.selectChangedForEvidence(userId, afterUpdateTime, afterSourceId, limit).stream()
                .map(this::toProjection)
                .toList();
    }

    private GrowthEvidenceProjection toProjection(InterviewMasteryRecord record) {
        LocalDateTime sourceUpdatedAt = firstPresent(
                record.getUpdateTime(), record.getLastReviewTime(), record.getCreateTime()
        );
        GrowthEvidenceProjection projection = new GrowthEvidenceProjection();
        projection.setUserId(record.getUserId());
        projection.setSourceModule(SOURCE_MODULE);
        projection.setSourceType(SOURCE_KEY);
        projection.setSourceId(String.valueOf(record.getId()));
        projection.setSourceRecordId(record.getId());
        projection.setEvidenceType(EVIDENCE_TYPE);
        projection.setSkillKey(skillKey(record.getQuestionSetId()));
        projection.setQualityLevel("VERIFIED");
        projection.setObservedAt(sourceUpdatedAt);
        projection.setSourceUpdatedAt(sourceUpdatedAt);
        projection.setActive(true);

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("questionId", record.getQuestionId());
        summary.put("questionSetId", record.getQuestionSetId());
        summary.put("masteryLevel", record.getMasteryLevel());
        summary.put("reviewCount", record.getReviewCount());
        summary.put("nextReviewTime", record.getNextReviewTime());
        projection.setSummary(summary);
        return projection;
    }

    private String skillKey(Long questionSetId) {
        return questionSetId == null ? "interview" : "question_set:" + questionSetId;
    }

    private LocalDateTime firstPresent(LocalDateTime... candidates) {
        for (LocalDateTime candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return LocalDateTime.of(1970, 1, 1, 0, 0);
    }
}
