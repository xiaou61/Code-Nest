package com.xiaou.techbriefing.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 科技热点卡片响应
 */
@Data
public class TechBriefingArticleCardResponse {

    private Long id;

    private String title;

    private String summary;

    private String aiSummary;

    private String sourceName;

    private String regionType;

    private String category;

    private String sourceUrl;

    private String coverImage;

    private String translationStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;
}
