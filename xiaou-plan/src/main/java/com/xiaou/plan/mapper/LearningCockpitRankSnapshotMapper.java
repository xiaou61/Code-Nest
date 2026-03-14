package com.xiaou.plan.mapper;

import com.xiaou.plan.domain.LearningCockpitRankSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习驾驶舱排名快照 Mapper
 */
@Mapper
public interface LearningCockpitRankSnapshotMapper {

    LearningCockpitRankSnapshot selectByUserAndWeekStart(@Param("userId") Long userId, @Param("weekStart") LocalDate weekStart);

    LearningCockpitRankSnapshot selectLatestBeforeWeek(@Param("userId") Long userId, @Param("weekStart") LocalDate weekStart);

    List<LearningCockpitRankSnapshot> selectRecentByUser(@Param("userId") Long userId, @Param("limit") Integer limit);

    int upsert(LearningCockpitRankSnapshot snapshot);
}

