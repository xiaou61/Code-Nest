package com.xiaou.web.growthcoach.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户成长证据的结构化投影，不保存源业务正文。
 */
@Data
public class GrowthEvidence {

    private Long id;
    private String evidenceId;
    private Long userId;
    private String evidenceType;
    private String sourceModule;
    private String sourceType;
    private String sourceId;
    private String skillKey;
    private String summaryJson;
    private String qualityLevel;
    private LocalDateTime observedAt;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String contentHash;
    private String projectorVersion;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
