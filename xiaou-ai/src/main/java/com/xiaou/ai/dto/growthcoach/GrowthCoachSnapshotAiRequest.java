package com.xiaou.ai.dto.growthcoach;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * AI成长教练快照请求
 */
@Data
@Accessors(chain = true)
public class GrowthCoachSnapshotAiRequest {

    private String scene;

    private String headline;

    private String summaryJson;

    private String sourceDigestJson;

    private String actionJson;
}
