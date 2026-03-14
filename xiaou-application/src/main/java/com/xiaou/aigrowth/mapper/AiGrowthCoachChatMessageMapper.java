package com.xiaou.aigrowth.mapper;

import com.xiaou.aigrowth.domain.AiGrowthCoachChatMessage;

import java.util.List;

/**
 * AI成长教练消息Mapper
 */
public interface AiGrowthCoachChatMessageMapper {

    List<AiGrowthCoachChatMessage> selectBySessionId(Long sessionId);

    int insert(AiGrowthCoachChatMessage message);

    long countAll();
}
