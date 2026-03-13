package com.xiaou.learningasset.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 学习资产转化记录
 */
@Data
@Accessors(chain = true)
public class LearningAssetRecord {

    private Long id;
    private Long userId;
    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private Long sourceAuthorId;
    private String sourceSnapshot;
    private String transformMode;
    private String targetTypes;
    private String status;
    private String sourceHash;
    private String summaryText;
    private String failReason;
    private Integer totalCandidates;
    private Integer publishedCandidates;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
