package com.xiaou.learningasset.dto.response;

import lombok.Data;

/**
 * 候选项响应
 */
@Data
public class LearningAssetCandidateResponse {

    private Long id;
    private String assetType;
    private String assetTypeText;
    private String title;
    private String contentJson;
    private String tags;
    private String difficulty;
    private Double confidenceScore;
    private String status;
    private String statusText;
    private String targetModule;
    private Long targetId;
    private String reviewNote;
}
