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

    /**
     * 确认计划调整前锁住该计划的任务快照，避免覆盖并发完成或资源变更。
     */
    List<GrowthAutopilotTask> selectByGoalIdForUpdate(@Param("goalId") Long goalId);

    GrowthAutopilotTask selectById(@Param("id") Long id);

    /**
     * 按更新时间增量读取用户任务，供 Growth Coach 证据投影使用。
     */
    List<GrowthAutopilotTask> selectChangedForEvidence(@Param("userId") Long userId,
                                                        @Param("afterUpdateTime") LocalDateTime afterUpdateTime,
                                                        @Param("afterSourceId") Long afterSourceId,
                                                        @Param("limit") int limit);

    List<Long> selectUserIdsChangedForEvidence(@Param("updatedAfter") LocalDateTime updatedAfter,
                                                @Param("limit") int limit);

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

    int supersedeTodoFromDate(@Param("goalId") Long goalId, @Param("userId") Long userId, @Param("date") LocalDate date);

    int supersedeTodoByGoalId(@Param("goalId") Long goalId, @Param("userId") Long userId);

    int applyPlanAdjustmentTask(@Param("id") Long id,
                                @Param("goalId") Long goalId,
                                @Param("userId") Long userId,
                                @Param("taskDate") LocalDate taskDate,
                                @Param("planVersion") Integer planVersion,
                                @Param("resourceType") String resourceType,
                                @Param("resourceId") String resourceId,
                                @Param("resourceVersion") String resourceVersion,
                                @Param("selectionReason") String selectionReason);

    int supersedePlanAdjustmentTask(@Param("id") Long id,
                                    @Param("goalId") Long goalId,
                                    @Param("userId") Long userId,
                                    @Param("planVersion") Integer planVersion,
                                    @Param("selectionReason") String selectionReason);

    int deleteByGoalId(@Param("goalId") Long goalId);
}
