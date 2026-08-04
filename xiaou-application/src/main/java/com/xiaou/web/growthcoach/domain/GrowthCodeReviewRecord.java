package com.xiaou.web.growthcoach.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户自有 CodePen 已保存版本的结构化 AI 审查记录。
 *
 * <p>只保存源码哈希、分数和受限审查结果，不复制 HTML/CSS/JavaScript 源码。</p>
 */
@Data
public class GrowthCodeReviewRecord {

    private Long id;
    private Long userId;
    private Long codePenId;
    private Long previousReviewId;
    private String sourceHash;
    private Integer score;
    private Integer criticalFindingCount;
    private Integer highFindingCount;
    private Integer mediumFindingCount;
    private String summary;
    private String resultJson;
    private String status;
    private LocalDateTime sourceObservedAt;
    private LocalDateTime reviewedAt;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
