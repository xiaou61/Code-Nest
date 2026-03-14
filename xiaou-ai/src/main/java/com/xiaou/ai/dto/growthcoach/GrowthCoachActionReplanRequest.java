package com.xiaou.ai.dto.growthcoach;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * AI成长教练行动重排请求
 */
@Data
@Accessors(chain = true)
public class GrowthCoachActionReplanRequest {

    private String scene;

    private Integer availableMinutes;

    private String headline;

    private String summaryJson;

    private String sourceDigestJson;

    private String selectedActionJson;

    private String deferredActionJson;
}
