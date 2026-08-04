package com.xiaou.web.growthcoach.intent;

import lombok.Data;

/**
 * 自然语言计划调整的规范化意图。
 */
@Data
public class GrowthCoachIntent {

    private Integer availableMinutes;

    private String targetRole;

    private boolean prioritizeInterview;

    private String summary;

    private String resolutionMode;
}
