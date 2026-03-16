package com.xiaou.techbriefing.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 科技热点文章内容
 */
@Data
@Accessors(chain = true)
public class TechBriefingArticleContent {

    private Long id;

    private Long articleId;

    private String rawContent;

    private String translatedContentZh;

    private String contentExtractStatus;

    private String translationStatus;

    private Integer tokenUsage;

    private String copyrightMode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
