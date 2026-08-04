package com.xiaou.web.growthcoach.mapper;

import com.xiaou.web.growthcoach.domain.GrowthJourneyEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成长主行动漏斗事件 Mapper。
 */
@Mapper
public interface GrowthJourneyEventMapper {

    int insert(GrowthJourneyEvent event);
}
