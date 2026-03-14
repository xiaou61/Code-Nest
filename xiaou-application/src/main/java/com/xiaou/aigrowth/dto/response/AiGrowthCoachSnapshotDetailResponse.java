package com.xiaou.aigrowth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI成长教练快照详情响应
 */
@Data
public class AiGrowthCoachSnapshotDetailResponse {

    private Long snapshotId;

    private String scene;

    private String status;

    private Integer learningScore;

    private Integer careerScore;

    private Integer executionScore;

    private Integer overallScore;

    private String riskLevel;

    private String headline;

    private String summary;

    private List<String> focusAreas;

    private List<String> riskFlags;

    private List<String> suggestedQuestions;

    private Map<String, Object> sourceDigest;

    private Boolean fallbackOnly;

    private List<AiGrowthCoachResourceResponse> resources;

    private List<AiGrowthCoachActionResponse> actions;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireAt;
}
