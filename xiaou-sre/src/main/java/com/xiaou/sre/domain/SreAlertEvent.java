package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Alertmanager 告警事件快照。
 *
 * @author xiaou
 */
@Data
public class SreAlertEvent {

    private Long id;
    private String source;
    private String fingerprint;
    private String alertName;
    private String status;
    private String severity;
    private String service;
    private String labelsJson;
    private String annotationsJson;
    private String startsAt;
    private String endsAt;
    private String generatorUrl;
    private String rawPayload;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
