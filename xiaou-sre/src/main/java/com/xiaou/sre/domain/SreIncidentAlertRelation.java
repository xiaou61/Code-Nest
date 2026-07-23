package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事故与原始告警事件的关联。
 *
 * @author xiaou
 */
@Data
public class SreIncidentAlertRelation {

    private Long id;
    private Long incidentId;
    private Long alertEventId;
    private LocalDateTime createTime;

    public SreIncidentAlertRelation() {
    }

    public SreIncidentAlertRelation(Long incidentId, Long alertEventId) {
        this.incidentId = incidentId;
        this.alertEventId = alertEventId;
    }
}
