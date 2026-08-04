package com.xiaou.web.growthcoach.domain;

import lombok.Data;

/**
 * 数据库一次聚合返回的成长业务漏斗快照。
 */
@Data
public class GrowthAnalyticsSnapshot {

    private Long primaryActionShownUsers;
    private Long primaryActionStartedUsers;
    private Long verifiedGrowthUsers;
    private Long planPreviewCount;
    private Long planExecutedCount;
    private Long careerApplicationProgressUsers;
}
