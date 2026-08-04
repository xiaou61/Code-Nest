package com.xiaou.web.growthcoach.service;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.web.growthcoach.domain.GrowthJourneyEvent;
import com.xiaou.web.growthcoach.dto.GrowthJourneyEventRequest;
import com.xiaou.web.growthcoach.dto.GrowthJourneyEventResponse;
import com.xiaou.web.growthcoach.mapper.GrowthJourneyEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 持久化用户成长主行动的最小业务漏斗。
 */
@Service
@RequiredArgsConstructor
public class GrowthJourneyEventService {

    private final GrowthJourneyEventMapper eventMapper;

    public GrowthJourneyEventResponse record(Long userId, GrowthJourneyEventRequest request) {
        if (userId == null || userId <= 0 || request == null) {
            throw new BusinessException("成长行动事件无效");
        }
        String eventType = normalize(request.getEventType(), 64, "事件类型不能为空");
        if (!GrowthJourneyEventCatalog.EVENT_TYPES.contains(eventType)) {
            throw new BusinessException("事件类型不合法");
        }

        GrowthJourneyEvent event = new GrowthJourneyEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setTrackingId(normalize(request.getTrackingId(), 96, "行动追踪ID不能为空"));
        String actionType = normalize(request.getActionType(), 64, "行动类型不能为空");
        if (!GrowthJourneyEventCatalog.ACTION_TYPES.contains(actionType)) {
            throw new BusinessException("行动类型不合法");
        }
        String source = normalize(request.getSource(), 64, "行动来源不能为空");
        if (!GrowthJourneyEventCatalog.SOURCES.contains(source)) {
            throw new BusinessException("行动来源不合法");
        }
        event.setActionType(actionType);
        event.setSource(source);
        event.setSchemaVersion(defaultValue(request.getSchemaVersion(), GrowthJourneyEventCatalog.SCHEMA_VERSION));
        event.setClientVersion(normalizeOptional(request.getClientVersion(), 32));
        event.setEntryPage(normalizeOptional(request.getEntryPage(), 128));

        GrowthJourneyEventResponse response = new GrowthJourneyEventResponse();
        try {
            response.setRecorded(eventMapper.insert(event) == 1);
        } catch (DuplicateKeyException ignored) {
            response.setRecorded(false);
        }
        response.setEventType(eventType);
        response.setTrackingId(event.getTrackingId());
        return response;
    }

    private String normalize(String value, int maxLength, String emptyMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(emptyMessage);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException("成长行动事件字段过长");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException("成长行动事件字段过长");
        }
        return normalized;
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? normalize(value, 16, "事件契约版本不能为空") : fallback;
    }
}
