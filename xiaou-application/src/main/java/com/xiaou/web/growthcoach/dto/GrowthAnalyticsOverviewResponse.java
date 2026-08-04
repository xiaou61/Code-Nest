package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端成长业务漏斗总览。
 */
@Data
public class GrowthAnalyticsOverviewResponse {

    private Integer days;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Long primaryActionShownUsers;
    private Long primaryActionStartedUsers;
    private Double primaryActionStartRate;
    private Long verifiedGrowthUsers;
    private Long planPreviewCount;
    private Long planExecutedCount;
    private Double planAdoptionRate;
    private Long careerApplicationProgressUsers;
}
