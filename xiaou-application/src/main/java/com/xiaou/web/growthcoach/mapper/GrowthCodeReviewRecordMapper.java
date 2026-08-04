package com.xiaou.web.growthcoach.mapper;

import com.xiaou.web.growthcoach.domain.GrowthCodeReviewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CodePen 审查记录存储。
 */
@Mapper
public interface GrowthCodeReviewRecordMapper {

    int insert(GrowthCodeReviewRecord record);

    GrowthCodeReviewRecord selectLatestByUserAndPen(@Param("userId") Long userId,
                                                    @Param("codePenId") Long codePenId);

    GrowthCodeReviewRecord selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int logicalDelete(@Param("id") Long id, @Param("userId") Long userId);

    List<GrowthCodeReviewRecord> selectChangedForEvidence(@Param("userId") Long userId,
                                                          @Param("afterUpdateTime") LocalDateTime afterUpdateTime,
                                                          @Param("afterSourceId") Long afterSourceId,
                                                          @Param("limit") int limit);

    List<Long> selectUserIdsChangedForEvidence(@Param("updatedAfter") LocalDateTime updatedAfter,
                                               @Param("limit") int limit);
}
