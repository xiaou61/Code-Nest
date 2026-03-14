package com.xiaou.learningasset.mapper;

import com.xiaou.learningasset.domain.LearningAssetRecord;
import com.xiaou.learningasset.dto.request.LearningAssetRecordQueryRequest;
import com.xiaou.learningasset.dto.response.LearningAssetStatisticsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 转化记录 Mapper
 */
@Mapper
public interface LearningAssetRecordMapper {

    int insert(LearningAssetRecord record);

    int update(LearningAssetRecord record);

    LearningAssetRecord selectById(@Param("id") Long id);

    LearningAssetRecord selectByUserIdAndId(@Param("userId") Long userId, @Param("id") Long id);

    List<LearningAssetRecord> selectByUserId(LearningAssetRecordQueryRequest request);

    Long countAll();

    Long countSuccess();

    Long countStatus(@Param("status") String status);

    List<LearningAssetStatisticsResponse.SourceStat> selectSourceStatistics();

    List<LearningAssetStatisticsResponse.FailReasonStat> selectFailReasonStatistics();

    List<LearningAssetStatisticsResponse.TopSourceStat> selectTopSourceStatistics();
}
