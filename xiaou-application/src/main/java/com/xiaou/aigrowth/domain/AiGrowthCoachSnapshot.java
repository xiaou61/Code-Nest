package com.xiaou.aigrowth.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI成长教练快照
 */
@Data
@Accessors(chain = true)
public class AiGrowthCoachSnapshot {

    private Long id;

    private Long userId;

    private String sceneScope;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate snapshotDate;

    private Integer learningScore;

    private Integer careerScore;

    private Integer executionScore;

    private Integer overallScore;

    private String riskLevel;

    private String headline;

    private String summaryJson;

    private String sourceDigestJson;

    private Boolean fallbackOnly;

    private String status;

    private String failReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
