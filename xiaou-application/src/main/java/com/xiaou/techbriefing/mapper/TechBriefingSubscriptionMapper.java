package com.xiaou.techbriefing.mapper;

import com.xiaou.techbriefing.domain.TechBriefingSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TechBriefingSubscriptionMapper {

    TechBriefingSubscription selectById(@Param("id") Long id);

    TechBriefingSubscription selectActiveByChannelAndWebhook(@Param("channelType") String channelType,
                                                             @Param("webhookUrl") String webhookUrl);

    List<TechBriefingSubscription> selectAll();

    long countAll();

    int insert(TechBriefingSubscription subscription);

    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    int updateTestResult(@Param("id") Long id,
                         @Param("lastTestAt") LocalDateTime lastTestAt,
                         @Param("lastPushStatus") String lastPushStatus);
}
