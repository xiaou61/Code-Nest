package com.xiaou.aigrowth.dto.response;

import lombok.Data;

/**
 * AI成长教练推荐资源响应
 */
@Data
public class AiGrowthCoachResourceResponse {

    private String resourceType;

    private String sourceLabel;

    private String title;

    private String summary;

    private String route;

    private String reason;
}
