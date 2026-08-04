package com.xiaou.web.growthcoach.evidence;

import com.xiaou.plan.domain.GrowthAutopilotTask;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 将成长计划任务的完成状态投影为证据。
 */
@Component
@RequiredArgsConstructor
public class GrowthAutopilotTaskEvidenceAdapter implements GrowthEvidenceAdapter {

    private static final String SOURCE_KEY = "growth_autopilot_task";
    private static final String SOURCE_MODULE = "plan";
    private static final String EVIDENCE_TYPE = "TASK_COMPLETED";

    private final GrowthAutopilotTaskMapper taskMapper;

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public List<GrowthEvidenceProjection> loadChanges(Long userId,
                                                       LocalDateTime afterUpdateTime,
                                                       Long afterSourceId,
                                                       int limit) {
        return taskMapper.selectChangedForEvidence(userId, afterUpdateTime, afterSourceId, limit).stream()
                .map(this::toProjection)
                .toList();
    }

    private GrowthEvidenceProjection toProjection(GrowthAutopilotTask task) {
        LocalDateTime sourceUpdatedAt = firstPresent(task.getUpdateTime(), task.getCompleteTime(), task.getCreateTime());
        GrowthEvidenceProjection projection = new GrowthEvidenceProjection();
        projection.setUserId(task.getUserId());
        projection.setSourceModule(SOURCE_MODULE);
        projection.setSourceType(SOURCE_KEY);
        projection.setSourceId(String.valueOf(task.getId()));
        projection.setSourceRecordId(task.getId());
        projection.setEvidenceType(EVIDENCE_TYPE);
        projection.setSkillKey(task.getModuleKey());
        projection.setQualityLevel("VERIFIED");
        projection.setObservedAt(firstPresent(task.getCompleteTime(), sourceUpdatedAt));
        projection.setSourceUpdatedAt(sourceUpdatedAt);
        projection.setActive("done".equalsIgnoreCase(task.getStatus()));

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleKey", task.getModuleKey());
        summary.put("plannedMinutes", task.getPlannedMinutes());
        summary.put("planVersion", task.getPlanVersion());
        summary.put("resourceType", task.getResourceType());
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
