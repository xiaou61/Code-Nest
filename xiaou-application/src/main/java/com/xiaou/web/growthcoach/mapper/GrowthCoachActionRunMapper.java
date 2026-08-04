package com.xiaou.web.growthcoach.mapper;

import com.xiaou.web.growthcoach.domain.GrowthCoachActionRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Growth Coach 用户动作运行记录 Mapper。
 */
@Mapper
public interface GrowthCoachActionRunMapper {

    GrowthCoachActionRun selectByUserAndClientRequest(@Param("userId") Long userId,
                                                       @Param("clientRequestId") String clientRequestId);

    GrowthCoachActionRun selectByRunIdAndUser(@Param("runId") String runId, @Param("userId") Long userId);

    GrowthCoachActionRun selectByRunIdAndUserForUpdate(@Param("runId") String runId, @Param("userId") Long userId);

    int insert(GrowthCoachActionRun run);

    int updateStatus(@Param("runId") String runId,
                     @Param("userId") Long userId,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("targetStatus") String targetStatus,
                     @Param("errorCode") String errorCode,
                     @Param("errorMessage") String errorMessage);

    int markExecuted(@Param("runId") String runId,
                     @Param("userId") Long userId,
                     @Param("targetPlanVersion") Integer targetPlanVersion,
                     @Param("resultJson") String resultJson);
}
