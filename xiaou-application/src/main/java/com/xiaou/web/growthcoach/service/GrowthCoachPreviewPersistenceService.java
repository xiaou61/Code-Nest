package com.xiaou.web.growthcoach.service;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.domain.GrowthCoachActionEvent;
import com.xiaou.web.growthcoach.domain.GrowthCoachActionRun;
import com.xiaou.web.growthcoach.mapper.GrowthCoachActionEventMapper;
import com.xiaou.web.growthcoach.mapper.GrowthCoachActionRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在短事务中固定 Preview 与其首条审计事件。
 */
@Service
@RequiredArgsConstructor
public class GrowthCoachPreviewPersistenceService {

    private static final String STATUS_PREVIEW = "PREVIEW";

    private final GrowthCoachActionRunMapper actionRunMapper;
    private final GrowthCoachActionEventMapper actionEventMapper;

    @Transactional(rollbackFor = Exception.class)
    public void persistPreview(GrowthCoachActionRun run) {
        if (run == null || run.getRunId() == null || !STATUS_PREVIEW.equals(run.getStatus())) {
            throw new BusinessException("计划调整预览数据无效");
        }
        if (actionRunMapper.insert(run) != 1) {
            throw new BusinessException("计划调整预览保存失败");
        }
        GrowthCoachActionEvent event = new GrowthCoachActionEvent();
        event.setRunId(run.getRunId());
        event.setSequenceNo(actionEventMapper.selectNextSequence(run.getRunId()));
        event.setFromStatus(null);
        event.setToStatus(STATUS_PREVIEW);
        event.setEventType("preview_created");
        event.setDetailJson("{}");
        if (actionEventMapper.insert(event) != 1) {
            throw new BusinessException("计划调整审计事件保存失败");
        }
    }
}
