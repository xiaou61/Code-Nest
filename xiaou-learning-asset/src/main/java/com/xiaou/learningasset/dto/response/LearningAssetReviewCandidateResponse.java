package com.xiaou.learningasset.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核候选项响应
 */
@Data
public class LearningAssetReviewCandidateResponse {

    private Long candidateId;
    private Long recordId;
    private Long userId;
    private String assetType;
    private String assetTypeText;
    private String title;
    private String contentJson;
    private String tags;
    private String difficulty;
    private Double confidenceScore;
    private String status;
    private String statusText;
    private String reviewNote;
    private String targetModule;
    private Long targetId;
    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private String sourceSnapshot;
    private LocalDateTime createTime;
}
