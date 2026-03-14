package com.xiaou.ai.dto.growthcoach;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * AI成长教练对话结果
 */
@Data
@Accessors(chain = true)
public class GrowthCoachChatResult {

    private String reply;

    private List<String> suggestedQuestions;

    private List<String> references;
}
