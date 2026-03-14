package com.xiaou.aigrowth.dto.request;

import lombok.Data;

/**
 * AI成长教练刷新请求
 */
@Data
public class AiGrowthCoachRefreshRequest {

    private String scene;

    private Boolean force = false;
}
