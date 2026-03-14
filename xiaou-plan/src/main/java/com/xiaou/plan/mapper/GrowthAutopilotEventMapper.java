package com.xiaou.plan.mapper;

import com.xiaou.plan.domain.GrowthAutopilotEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自动驾驶事件Mapper
 *
 * @author xiaou
 */
@Mapper
public interface GrowthAutopilotEventMapper {

    int insert(GrowthAutopilotEvent event);

    List<GrowthAutopilotEvent> selectLatestByGoalId(@Param("goalId") Long goalId, @Param("limit") Integer limit);
}

