package com.xiaou.plan.mapper;

import com.xiaou.plan.domain.GrowthAutopilotGoal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

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

    /** 查询当前周仍在执行自动驾驶计划的用户，用于受限主动提醒。 */
    List<Long> selectActiveUserIdsByWeek(@Param("weekStart") LocalDate weekStart, @Param("limit") int limit);

    int insert(GrowthAutopilotGoal goal);

    int updateById(GrowthAutopilotGoal goal);

    int updateMetrics(GrowthAutopilotGoal goal);

    int advancePlanVersion(@Param("id") Long id,
                           @Param("userId") Long userId,
                           @Param("expectedPlanVersion") Integer expectedPlanVersion,
                           @Param("nextPlanVersion") Integer nextPlanVersion,
                           @Param("targetRole") String targetRole,
                           @Param("weeklyMinutes") Integer weeklyMinutes,
                           @Param("weeklyHours") Integer weeklyHours,
                           @Param("actionRunId") String actionRunId);
}

