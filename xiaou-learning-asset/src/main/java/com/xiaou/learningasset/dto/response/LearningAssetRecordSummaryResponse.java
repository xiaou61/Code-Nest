package com.xiaou.learningasset.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 转化记录摘要
 */
@Data
public class LearningAssetRecordSummaryResponse {

    private Long recordId;
    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private String status;
    private String statusText;
    private Integer totalCandidates;
    private Integer publishedCandidates;
    private LocalDateTime createTime;
}
