package com.xiaou.aigrowth.dto.response;

import lombok.Data;

/**
 * AI成长教练动作响应
 */
@Data
public class AiGrowthCoachActionResponse {

    private Long actionId;

    private String title;

    private String description;

    private String priority;

    private String actionType;

    private String targetRoute;

    private String reason;

    private String expectedGain;

    private Integer estimatedMinutes;

    private String status;

    private Integer sortOrder;
}
