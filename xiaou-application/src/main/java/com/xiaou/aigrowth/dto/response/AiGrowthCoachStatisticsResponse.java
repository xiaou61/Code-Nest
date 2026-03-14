package com.xiaou.aigrowth.dto.response;

import lombok.Data;

import java.util.Map;

/**
 * AI成长教练统计响应
 */
@Data
public class AiGrowthCoachStatisticsResponse {

    private Long totalSnapshots;

    private Long todaySnapshots;

    private Long failedSnapshots;

    private Long fallbackSnapshots;

    private Long totalChatSessions;

    private Long totalMessages;

    private Double avgMessagesPerSession;

    private Double actionDoneRate;

    private Long totalReplans;

    private Long todayReplans;

    private Double replanFallbackRate;

    private Double avgCompressionRate;

    private Map<String, Long> sceneDistribution;
}
