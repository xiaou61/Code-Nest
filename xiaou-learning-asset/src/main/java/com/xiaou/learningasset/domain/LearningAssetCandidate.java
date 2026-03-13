package com.xiaou.learningasset.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 学习资产候选项
 */
@Data
@Accessors(chain = true)
public class LearningAssetCandidate {

    private Long id;
    private Long recordId;
    private Long userId;
    private String assetType;
    private String title;
    private String contentJson;
    private String tags;
    private String difficulty;
    private Double confidenceScore;
    private String dedupeKey;
    private String targetModule;
    private Long targetId;
    private String status;
    private Integer sortOrder;
    private String reviewNote;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
