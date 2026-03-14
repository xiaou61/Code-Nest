package com.xiaou.aigrowth.dto.response;

import lombok.Data;

import java.util.List;

/**
 * AI成长教练对话响应
 */
@Data
public class AiGrowthCoachChatResponse {

    private Long sessionId;

    private Long snapshotId;

    private String scene;

    private String title;

    private String reply;

    private List<String> suggestedQuestions;

    private List<AiGrowthCoachChatMessageResponse> messages;
}
