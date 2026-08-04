package com.xiaou.web.growthcoach.mapper;

import com.xiaou.web.growthcoach.domain.GrowthCoachActionEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Growth Coach 用户动作事件 Mapper。
 */
@Mapper
public interface GrowthCoachActionEventMapper {

    Integer selectNextSequence(@Param("runId") String runId);

    int insert(GrowthCoachActionEvent event);
}
