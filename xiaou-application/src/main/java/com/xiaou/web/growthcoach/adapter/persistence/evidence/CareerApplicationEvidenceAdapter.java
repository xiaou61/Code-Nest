package com.xiaou.web.growthcoach.adapter.persistence.evidence;

import com.xiaou.mockinterview.domain.CareerApplicationRecord;
import com.xiaou.mockinterview.mapper.CareerApplicationRecordMapper;
import com.xiaou.web.growthcoach.evidence.GrowthEvidenceAdapter;
import com.xiaou.web.growthcoach.evidence.GrowthEvidenceProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 将用户主动维护的投递状态投影为成长档案中的自报事实。
 *
 * 投影不携带公司、岗位或备注，也不把用户自报结果归类为已验证能力证据。
 */
@Component
@RequiredArgsConstructor
public class CareerApplicationEvidenceAdapter implements GrowthEvidenceAdapter {

    private static final String SOURCE_KEY = "career_application_record";
    private static final String SOURCE_MODULE = "career_loop";
    private static final String EVIDENCE_TYPE = "CAREER_APPLICATION_STATUS";

    private final CareerApplicationRecordMapper applicationRecordMapper;

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public List<GrowthEvidenceProjection> loadChanges(Long userId,
                                                       LocalDateTime afterUpdateTime,
                                                       Long afterSourceId,
                                                       int limit) {
        return applicationRecordMapper.selectChangedForEvidence(userId, afterUpdateTime, afterSourceId, limit)
                .stream()
                .map(this::toProjection)
                .toList();
    }

    private GrowthEvidenceProjection toProjection(CareerApplicationRecord record) {
        GrowthEvidenceProjection projection = new GrowthEvidenceProjection();
        projection.setUserId(record.getUserId());
        projection.setSourceModule(SOURCE_MODULE);
        projection.setSourceType(SOURCE_KEY);
        projection.setSourceId(String.valueOf(record.getId()));
        projection.setSourceRecordId(record.getId());
        projection.setEvidenceType(EVIDENCE_TYPE);
        projection.setSkillKey("career_application:" + safeStatus(record.getStatus()));
        projection.setQualityLevel("SELF_REPORTED");
        projection.setObservedAt(record.getUpdateTime());
        projection.setSourceUpdatedAt(record.getUpdateTime());
        projection.setActive(record.getDeleted() == null || record.getDeleted() == 0);

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", safeStatus(record.getStatus()));
        summary.put("hasAppliedDate", record.getAppliedDate() != null);
        summary.put("hasFollowUp", record.getNextFollowUpDate() != null);
        projection.setSummary(summary);
        return projection;
    }

    private String safeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : "UNKNOWN";
    }
}
