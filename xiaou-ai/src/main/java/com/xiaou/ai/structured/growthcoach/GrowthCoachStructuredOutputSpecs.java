package com.xiaou.ai.structured.growthcoach;

import com.xiaou.ai.prompt.growthcoach.GrowthCoachPromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputSpec;

/**
 * AI 成长教练结构化输出契约。
 */
public final class GrowthCoachStructuredOutputSpecs {

    public static final AiStructuredOutputSpec PLAN_ADJUSTMENT_INTENT = AiStructuredOutputSpec.object(
            GrowthCoachPromptSpecs.PLAN_ADJUSTMENT_INTENT,
            validator -> validator
                    .requireIntRange("availableMinutes", 0, 2400)
                    .requireString("targetRole")
                    .requireIntRange("prioritizeInterview", 0, 1)
                    .requireString("summary")
    );

    private GrowthCoachStructuredOutputSpecs() {
    }
}
