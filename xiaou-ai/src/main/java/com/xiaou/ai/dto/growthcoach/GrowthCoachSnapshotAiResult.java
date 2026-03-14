package com.xiaou.ai.dto.growthcoach;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * AI成长教练快照结果
 */
@Data
@Accessors(chain = true)
public class GrowthCoachSnapshotAiResult {

    private String headline;

    private String summary;

    private String riskLevel;

    private List<String> riskFlags;

    private List<String> focusAreas;

    private List<String> recommendedQuestions;
}
