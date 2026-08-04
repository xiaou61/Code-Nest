package com.xiaou.web.growthcoach.mapper;

import com.xiaou.web.growthcoach.domain.GrowthCoachNudge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Growth Coach 主动提醒的幂等存储。
 */
@Mapper
public interface GrowthCoachNudgeMapper {

    int insertIgnore(GrowthCoachNudge nudge);

    Long selectIdByUserAndNudgeKey(@Param("userId") Long userId, @Param("nudgeKey") String nudgeKey);

    int markSent(@Param("id") Long id,
                 @Param("userId") Long userId,
                 @Param("notificationId") Long notificationId);

    int markFailed(@Param("id") Long id, @Param("userId") Long userId);
}
