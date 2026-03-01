package com.xiaou.plan.mapper;

import com.xiaou.plan.domain.GrowthAutopilotGoal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 自动驾驶周目标Mapper
 *
 * @author xiaou
 */
@Mapper
public interface GrowthAutopilotGoalMapper {

    GrowthAutopilotGoal selectByUserAndWeek(@Param("userId") Long userId, @Param("weekStart") LocalDate weekStart);

    GrowthAutopilotGoal selectLatestByUser(@Param("userId") Long userId);

    GrowthAutopilotGoal selectById(@Param("id") Long id);

    int insert(GrowthAutopilotGoal goal);

    int updateById(GrowthAutopilotGoal goal);
}

