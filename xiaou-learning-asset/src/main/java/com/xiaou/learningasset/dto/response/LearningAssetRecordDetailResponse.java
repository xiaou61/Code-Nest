package com.xiaou.learningasset.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 转化记录详情
 */
@Data
public class LearningAssetRecordDetailResponse {

    private Long recordId;
    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private String sourceSnapshot;
    private String transformMode;
    private String status;
    private String statusText;
    private String summaryText;
    private Integer totalCandidates;
    private Integer publishedCandidates;
    private String failReason;
    private List<LearningAssetCandidateResponse> candidates;
}
