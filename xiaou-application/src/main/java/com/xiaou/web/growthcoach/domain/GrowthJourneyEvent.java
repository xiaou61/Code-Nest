package com.xiaou.web.growthcoach.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户成长主行动漏斗事件。
 */
@Data
public class GrowthJourneyEvent {

    private Long id;
    private Long userId;
    private String eventType;
    private String trackingId;
    private String actionType;
    private String source;
    private String schemaVersion;
    private String clientVersion;
    private String entryPage;
    private LocalDateTime createTime;
}
