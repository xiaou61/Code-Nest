package com.xiaou.learningasset.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.learningasset.dto.request.LearningAssetAdminCandidateUpdateRequest;
import com.xiaou.learningasset.dto.request.LearningAssetApproveRequest;
import com.xiaou.learningasset.dto.request.LearningAssetMergeRequest;
import com.xiaou.learningasset.dto.request.LearningAssetReviewQueryRequest;
import com.xiaou.learningasset.dto.response.LearningAssetPublishResponse;
import com.xiaou.learningasset.dto.response.LearningAssetReviewCandidateResponse;
import com.xiaou.learningasset.dto.response.LearningAssetStatisticsResponse;

import java.util.List;

/**
 * 学习资产发布与审核服务
 */
public interface LearningAssetPublishService {

    LearningAssetPublishResponse publish(Long userId, Long recordId, List<Long> candidateIds);

    PageResult<LearningAssetReviewCandidateResponse> getReviewList(LearningAssetReviewQueryRequest request);

    LearningAssetReviewCandidateResponse getReviewDetail(Long candidateId);

    void updateReviewCandidate(Long adminId, Long candidateId, LearningAssetAdminCandidateUpdateRequest request);

    Long approve(Long adminId, Long candidateId, LearningAssetApproveRequest request);

    Long merge(Long adminId, Long candidateId, LearningAssetMergeRequest request);

    void reject(Long adminId, Long candidateId, String note);

    LearningAssetStatisticsResponse getStatistics();
}
