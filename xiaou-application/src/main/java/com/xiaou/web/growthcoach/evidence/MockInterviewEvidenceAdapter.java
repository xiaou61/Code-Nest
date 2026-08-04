package com.xiaou.web.growthcoach.evidence;

import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 将模拟面试结果投影为分数证据，不复制 AI 评语正文。
 */
@Component
@RequiredArgsConstructor
public class MockInterviewEvidenceAdapter implements GrowthEvidenceAdapter {

    private static final String SOURCE_KEY = "mock_interview_session";
    private static final String SOURCE_MODULE = "mock_interview";
    private static final String EVIDENCE_TYPE = "INTERVIEW_SCORE";

    private final MockInterviewSessionMapper sessionMapper;

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public List<GrowthEvidenceProjection> loadChanges(Long userId,
                                                       LocalDateTime afterUpdateTime,
                                                       Long afterSourceId,
                                                       int limit) {
        return sessionMapper.selectChangedForEvidence(userId, afterUpdateTime, afterSourceId, limit).stream()
                .map(this::toProjection)
                .toList();
    }

    private GrowthEvidenceProjection toProjection(MockInterviewSession session) {
        LocalDateTime sourceUpdatedAt = firstPresent(session.getUpdateTime(), session.getEndTime(), session.getCreateTime());
        GrowthEvidenceProjection projection = new GrowthEvidenceProjection();
        projection.setUserId(session.getUserId());
        projection.setSourceModule(SOURCE_MODULE);
        projection.setSourceType(SOURCE_KEY);
        projection.setSourceId(String.valueOf(session.getId()));
        projection.setSourceRecordId(session.getId());
        projection.setEvidenceType(EVIDENCE_TYPE);
        projection.setSkillKey(session.getDirection());
        projection.setQualityLevel("VERIFIED");
        projection.setObservedAt(firstPresent(session.getEndTime(), sourceUpdatedAt));
        projection.setSourceUpdatedAt(sourceUpdatedAt);
        projection.setActive(Integer.valueOf(1).equals(session.getStatus()));

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("direction", session.getDirection());
        summary.put("totalScore", session.getTotalScore());
        summary.put("questionCount", session.getQuestionCount());
        summary.put("level", session.getLevel());
        summary.put("interviewType", session.getInterviewType());
        projection.setSummary(summary);
        return projection;
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
