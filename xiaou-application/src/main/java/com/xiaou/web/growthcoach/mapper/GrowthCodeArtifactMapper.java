package com.xiaou.web.growthcoach.mapper;

import com.xiaou.web.growthcoach.domain.GrowthCodeArtifact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户公开代码来源的持久化访问。
 */
@Mapper
public interface GrowthCodeArtifactMapper {

    int upsert(GrowthCodeArtifact artifact);

    GrowthCodeArtifact selectByUserAndExternalKey(@Param("userId") Long userId,
                                                  @Param("provider") String provider,
                                                  @Param("artifactType") String artifactType,
                                                  @Param("repository") String repository,
                                                  @Param("externalId") String externalId);

    List<GrowthCodeArtifact> selectActiveByUserId(@Param("userId") Long userId,
                                                  @Param("limit") int limit);

    int logicalDelete(@Param("id") Long id, @Param("userId") Long userId);

    List<GrowthCodeArtifact> selectChangedForEvidence(@Param("userId") Long userId,
                                                      @Param("afterUpdateTime") LocalDateTime afterUpdateTime,
                                                      @Param("afterSourceId") Long afterSourceId,
                                                      @Param("limit") int limit);

    List<Long> selectUserIdsChangedForEvidence(@Param("updatedAfter") LocalDateTime updatedAfter,
                                               @Param("limit") int limit);
}
