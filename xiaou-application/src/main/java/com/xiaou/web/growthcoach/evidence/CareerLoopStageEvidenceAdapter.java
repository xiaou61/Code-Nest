package com.xiaou.web.growthcoach.evidence;

import com.xiaou.mockinterview.domain.CareerLoopStageLog;
import com.xiaou.mockinterview.mapper.CareerLoopStageLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 将求职闭环已发生的阶段推进投影为成长证据。
 *
 * 阶段推进仅说明用户完成了可追溯流程节点，不推断投递、Offer 或能力结果。
 */
@Component
@RequiredArgsConstructor
public class CareerLoopStageEvidenceAdapter implements GrowthEvidenceAdapter {

    private static final String SOURCE_KEY = "career_loop_stage_log";
    private static final String SOURCE_MODULE = "career_loop";
    private static final String EVIDENCE_TYPE = "CAREER_STAGE_PROGRESS";

    private final CareerLoopStageLogMapper stageLogMapper;

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public List<GrowthEvidenceProjection> loadChanges(Long userId,
                                                       LocalDateTime afterUpdateTime,
                                                       Long afterSourceId,
                                                       int limit) {
        return stageLogMapper.selectChangedForEvidence(userId, afterUpdateTime, afterSourceId, limit).stream()
                .map(this::toProjection)
                .toList();
    }

    private GrowthEvidenceProjection toProjection(CareerLoopStageLog stageLog) {
        GrowthEvidenceProjection projection = new GrowthEvidenceProjection();
        projection.setUserId(stageLog.getUserId());
        projection.setSourceModule(SOURCE_MODULE);
        projection.setSourceType(SOURCE_KEY);
        projection.setSourceId(String.valueOf(stageLog.getId()));
        projection.setSourceRecordId(stageLog.getId());
        projection.setEvidenceType(EVIDENCE_TYPE);
        projection.setSkillKey("career_stage:" + safeStage(stageLog.getToStage()));
        projection.setQualityLevel("VERIFIED");
        projection.setObservedAt(stageLog.getCreateTime());
        projection.setSourceUpdatedAt(stageLog.getCreateTime());
        projection.setActive(true);

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("fromStage", stageLog.getFromStage());
        summary.put("toStage", stageLog.getToStage());
        summary.put("triggerSource", stageLog.getTriggerSource());
        summary.put("triggerRefId", stageLog.getTriggerRefId());
        projection.setSummary(summary);
        return projection;
    }

    private String safeStage(String stage) {
        return stage == null || stage.isBlank() ? "unknown" : stage.trim().toLowerCase();
    }
}
