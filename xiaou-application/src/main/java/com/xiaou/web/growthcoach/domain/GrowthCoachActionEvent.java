package com.xiaou.web.growthcoach.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成长教练 Action Run 状态事件。
 */
@Data
public class GrowthCoachActionEvent {

    private Long id;
    private String runId;
    private Integer sequenceNo;
    private String fromStatus;
    private String toStatus;
    private String eventType;
    private String detailJson;
    private LocalDateTime createTime;
}
