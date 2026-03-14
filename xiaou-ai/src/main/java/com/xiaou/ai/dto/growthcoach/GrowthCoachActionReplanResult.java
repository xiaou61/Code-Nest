package com.xiaou.ai.dto.growthcoach;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * AI成长教练行动重排结果
 */
@Data
@Accessors(chain = true)
public class GrowthCoachActionReplanResult {

    private String summary;

    private List<String> suggestedQuestions;
}
