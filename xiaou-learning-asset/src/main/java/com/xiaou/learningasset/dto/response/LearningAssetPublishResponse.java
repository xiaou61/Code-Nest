package com.xiaou.learningasset.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 发布结果响应
 */
@Data
public class LearningAssetPublishResponse {

    private Long recordId;
    private Integer publishedCount = 0;
    private Integer reviewingCount = 0;
    private Integer failedCount = 0;
    private Long flashcardDeckId;
    private List<Long> planIds = new ArrayList<>();
    private List<Long> publishedCandidateIds = new ArrayList<>();
    private List<Long> reviewingCandidateIds = new ArrayList<>();
}
