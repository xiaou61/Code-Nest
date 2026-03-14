package com.xiaou.aigrowth.mapper;

import com.xiaou.aigrowth.domain.AiGrowthCoachChatSession;
import org.apache.ibatis.annotations.Param;

/**
 * AI成长教练会话Mapper
 */
public interface AiGrowthCoachChatSessionMapper {

    AiGrowthCoachChatSession selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    AiGrowthCoachChatSession selectLatestBySnapshotId(@Param("userId") Long userId, @Param("snapshotId") Long snapshotId);

    int insert(AiGrowthCoachChatSession session);

    int updateTitle(@Param("id") Long id, @Param("title") String title);

    int updateLastMessageAt(@Param("id") Long id, @Param("lastMessageAt") java.time.LocalDateTime lastMessageAt);

    long countAll();
}
