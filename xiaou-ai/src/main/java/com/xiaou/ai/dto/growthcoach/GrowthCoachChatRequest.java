package com.xiaou.ai.dto.growthcoach;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * AI成长教练对话请求
 */
@Data
@Accessors(chain = true)
public class GrowthCoachChatRequest {

    private String scene;

    private String message;

    private String headline;

    private String summaryJson;

    private String sourceDigestJson;
}
