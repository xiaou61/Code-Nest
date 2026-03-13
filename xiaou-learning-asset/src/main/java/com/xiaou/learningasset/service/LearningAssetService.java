package com.xiaou.learningasset.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.learningasset.dto.request.LearningAssetCandidateUpdateRequest;
import com.xiaou.learningasset.dto.request.LearningAssetConfirmRequest;
import com.xiaou.learningasset.dto.request.LearningAssetConvertRequest;
import com.xiaou.learningasset.dto.request.LearningAssetRecordQueryRequest;
import com.xiaou.learningasset.dto.response.LearningAssetRecordDetailResponse;
import com.xiaou.learningasset.dto.response.LearningAssetRecordSummaryResponse;

/**
 * 学习资产服务
 */
public interface LearningAssetService {

    LearningAssetRecordDetailResponse convert(Long userId, LearningAssetConvertRequest request);

    PageResult<LearningAssetRecordSummaryResponse> getRecordList(Long userId, LearningAssetRecordQueryRequest request);

    LearningAssetRecordDetailResponse getRecordDetail(Long userId, Long recordId);

    void updateCandidate(Long userId, Long candidateId, LearningAssetCandidateUpdateRequest request);

    LearningAssetRecordDetailResponse confirmCandidates(Long userId, Long recordId, LearningAssetConfirmRequest request);

    LearningAssetRecordDetailResponse discardCandidate(Long userId, Long candidateId);

    LearningAssetRecordDetailResponse retry(Long userId, Long recordId);
}
