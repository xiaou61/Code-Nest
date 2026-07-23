package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多条相关告警聚合后的事故。
 *
 * @author xiaou
 */
@Data
public class SreIncident {

    private Long id;
    private String incidentNo;
    private String incidentKey;
    private String service;
    private String alertName;
    private String severity;
    private String state;
    private String summary;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private Long acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
