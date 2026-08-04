package com.xiaou.web.growthcoach.mapper;

import com.xiaou.web.growthcoach.domain.GrowthEvidence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成长证据索引 Mapper。
 */
@Mapper
public interface GrowthEvidenceMapper {

    int upsert(GrowthEvidence evidence);

    int invalidateBySource(@Param("userId") Long userId,
                           @Param("sourceModule") String sourceModule,
                           @Param("sourceType") String sourceType,
                           @Param("sourceId") String sourceId,
                           @Param("evidenceType") String evidenceType,
                           @Param("projectorVersion") String projectorVersion,
                           @Param("invalidatedAt") LocalDateTime invalidatedAt);

    List<GrowthEvidence> selectRecentValidByUser(@Param("userId") Long userId, @Param("limit") int limit);
}
