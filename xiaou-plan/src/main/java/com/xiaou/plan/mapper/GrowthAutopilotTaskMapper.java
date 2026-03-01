package com.xiaou.plan.mapper;

import com.xiaou.plan.domain.GrowthAutopilotTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动驾驶任务Mapper
 *
 * @author xiaou
 */
@Mapper
public interface GrowthAutopilotTaskMapper {

    int batchInsert(@Param("list") List<GrowthAutopilotTask> list);

    List<GrowthAutopilotTask> selectByGoalId(@Param("goalId") Long goalId);

    GrowthAutopilotTask selectById(@Param("id") Long id);

    int markDone(@Param("id") Long id,
                 @Param("goalId") Long goalId,
                 @Param("userId") Long userId,
                 @Param("completeTime") LocalDateTime completeTime);

    int markDoneByDate(@Param("goalId") Long goalId,
                       @Param("userId") Long userId,
                       @Param("date") LocalDate date,
                       @Param("completeTime") LocalDateTime completeTime);

    int postponeTask(@Param("id") Long id,
                     @Param("goalId") Long goalId,
                     @Param("userId") Long userId,
                     @Param("newDate") LocalDate newDate);

    int markMissedBeforeDate(@Param("goalId") Long goalId, @Param("userId") Long userId, @Param("date") LocalDate date);

    int deleteTodoFromDate(@Param("goalId") Long goalId, @Param("userId") Long userId, @Param("date") LocalDate date);

    int deleteByGoalId(@Param("goalId") Long goalId);
}
