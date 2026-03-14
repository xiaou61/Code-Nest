package com.xiaou.aigrowth.mapper;

import com.xiaou.aigrowth.domain.AiGrowthCoachSnapshot;
import org.apache.ibatis.annotations.Param;

/**
 * AI成长教练快照Mapper
 */
public interface AiGrowthCoachSnapshotMapper {

    AiGrowthCoachSnapshot selectLatestReady(@Param("userId") Long userId, @Param("sceneScope") String sceneScope);

    AiGrowthCoachSnapshot selectById(Long id);

    int insert(AiGrowthCoachSnapshot snapshot);

    long countAll();

    long countToday();

    long countFailed();

    long countFallback();

    long countBySceneScope(String sceneScope);

    java.util.List<AiGrowthCoachSnapshot> selectFailures(@Param("limit") int limit);
}
