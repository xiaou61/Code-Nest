package com.xiaou.web.growthcoach.adapter.persistence.evidence;

import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import com.xiaou.web.growthcoach.evidence.GrowthEvidenceAdapter;
import com.xiaou.web.growthcoach.evidence.GrowthEvidenceProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 将最终 OJ 判题结果投影为成长证据，不复制用户提交代码或错误信息正文。
 */
@Component
@RequiredArgsConstructor
public class OjSubmissionEvidenceAdapter implements GrowthEvidenceAdapter {

    private static final String SOURCE_KEY = "oj_submission";
    private static final String SOURCE_MODULE = "oj";
    private static final String EVIDENCE_TYPE = "OJ_SUBMISSION_RESULT";
    private static final Set<String> FINAL_STATUSES = Set.of(
            "accepted", "wrong_answer", "time_limit_exceeded", "memory_limit_exceeded",
            "runtime_error", "compile_error"
    );

    private final OjSubmissionMapper submissionMapper;

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public List<GrowthEvidenceProjection> loadChanges(Long userId,
                                                       LocalDateTime afterUpdateTime,
                                                       Long afterSourceId,
                                                       int limit) {
        return submissionMapper.selectChangedForEvidence(userId, afterUpdateTime, afterSourceId, limit).stream()
                .map(this::toProjection)
                .toList();
    }

    private GrowthEvidenceProjection toProjection(OjSubmission submission) {
        LocalDateTime sourceUpdatedAt = firstPresent(submission.getUpdateTime(), submission.getCreateTime());
        String status = normalizeStatus(submission.getStatus());
        GrowthEvidenceProjection projection = new GrowthEvidenceProjection();
        projection.setUserId(submission.getUserId());
        projection.setSourceModule(SOURCE_MODULE);
        projection.setSourceType(SOURCE_KEY);
        projection.setSourceId(String.valueOf(submission.getId()));
        projection.setSourceRecordId(submission.getId());
        projection.setEvidenceType(EVIDENCE_TYPE);
        projection.setSkillKey(skillKey(submission.getProblemId()));
        projection.setQualityLevel("VERIFIED");
        projection.setObservedAt(sourceUpdatedAt);
        projection.setSourceUpdatedAt(sourceUpdatedAt);
        projection.setActive(FINAL_STATUSES.contains(status));

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("problemId", submission.getProblemId());
        summary.put("status", status);
        summary.put("language", submission.getLanguage());
        summary.put("passCount", submission.getPassCount());
        summary.put("totalCount", submission.getTotalCount());
        projection.setSummary(summary);
        return projection;
    }

    private String skillKey(Long problemId) {
        return problemId == null ? "oj" : "oj_problem:" + problemId;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
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
