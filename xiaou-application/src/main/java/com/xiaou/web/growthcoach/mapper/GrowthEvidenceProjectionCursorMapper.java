package com.xiaou.web.growthcoach.mapper;

import com.xiaou.web.growthcoach.domain.GrowthEvidenceProjectionCursor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 成长证据投影游标 Mapper。
 */
@Mapper
public interface GrowthEvidenceProjectionCursorMapper {

    GrowthEvidenceProjectionCursor selectByUserAndSourceKey(@Param("userId") Long userId,
                                                              @Param("sourceKey") String sourceKey);

    int upsert(GrowthEvidenceProjectionCursor cursor);
}
