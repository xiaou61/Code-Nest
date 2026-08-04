package com.xiaou.web.growthcoach.dto;

import lombok.Data;

/**
 * 成长主行动漏斗事件写入结果。
 */
@Data
public class GrowthJourneyEventResponse {

    private boolean recorded;
    private String eventType;
    private String trackingId;
}
