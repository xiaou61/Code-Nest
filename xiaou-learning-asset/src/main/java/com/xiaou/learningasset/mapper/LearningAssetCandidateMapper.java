package com.xiaou.learningasset.mapper;

import com.xiaou.learningasset.domain.LearningAssetCandidate;
import com.xiaou.learningasset.dto.response.LearningAssetStatisticsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 候选项 Mapper
 */
@Mapper
public interface LearningAssetCandidateMapper {

    int insert(LearningAssetCandidate candidate);

    int updateEditable(LearningAssetCandidate candidate);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("targetId") Long targetId,
                     @Param("reviewNote") String reviewNote);

    LearningAssetCandidate selectById(@Param("id") Long id);

    List<LearningAssetCandidate> selectByRecordId(@Param("recordId") Long recordId);

    List<LearningAssetCandidate> selectByRecordIdAndStatuses(@Param("recordId") Long recordId,
                                                             @Param("statuses") List<String> statuses);

    List<LearningAssetCandidate> selectReviewingCandidates(@Param("assetType") String assetType);

    Long countAll();

    Long countEdited();

    Long countByStatuses(@Param("statuses") List<String> statuses);

    List<LearningAssetStatisticsResponse.AssetStat> selectAssetStatistics();
}
