package com.xiaou.techbriefing.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 科技热点详情响应
 */
@Data
public class TechBriefingArticleDetailResponse {

    private Long id;

    private String title;

    private String originalTitle;

    private String summary;

    private String aiSummary;

    private String whyImportant;

    private String impactScope;

    private String translatedContentZh;

    private String rawContent;

    private String sourceUrl;

    private String sourceName;

    private String translationStatus;

    private List<String> tags;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;
}
