package com.xiaou.aigrowth.dto.response;

import lombok.Data;

import java.util.List;

/**
 * AI成长教练时间压缩重排响应
 */
@Data
public class AiGrowthCoachReplanResponse {

    private Long snapshotId;

    private String scene;

    private Integer availableMinutes;

    private Integer originalTotalMinutes;

    private String summary;

    private Boolean fallbackOnly;

    private List<String> suggestedQuestions;

    private List<AiGrowthCoachActionResponse> actions;

    private List<AiGrowthCoachActionResponse> deferredActions;
}
