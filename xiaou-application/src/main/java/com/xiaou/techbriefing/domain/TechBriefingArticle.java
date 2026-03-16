package com.xiaou.techbriefing.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 科技热点文章
 */
@Data
@Accessors(chain = true)
public class TechBriefingArticle {

    private Long id;

    private Long sourceId;

    private String sourceName;

    private String title;

    private String titleZh;

    private String summary;

    private String summaryZh;

    private String sourceUrl;

    private String coverImage;

    private String language;

    private String regionType;

    private String category;

    private String tagsJson;

    private Integer hotScore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;

    private String status;

    private Boolean isPinned;

    private String translationStatus;

    private String aiSummaryStatus;

    private Boolean deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
