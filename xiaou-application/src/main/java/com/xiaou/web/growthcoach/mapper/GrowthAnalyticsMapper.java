package com.xiaou.web.growthcoach.mapper;

import com.xiaou.web.growthcoach.domain.GrowthAnalyticsSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 管理端成长业务漏斗聚合 Mapper。
 */
@Mapper
public interface GrowthAnalyticsMapper {

    GrowthAnalyticsSnapshot selectOverview(@Param("periodStart") LocalDateTime periodStart,
                                             @Param("periodEnd") LocalDateTime periodEnd);
}
