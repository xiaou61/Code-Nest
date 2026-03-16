package com.xiaou.techbriefing.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 科技热点 AI 结果
 */
@Data
@Accessors(chain = true)
public class TechBriefingArticleAi {

    private Long id;

    private Long articleId;

    private String summaryJson;

    private String keywordsJson;

    private String whyImportant;

    private String impactScope;

    private String modelName;

    private String status;

    private String failReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
