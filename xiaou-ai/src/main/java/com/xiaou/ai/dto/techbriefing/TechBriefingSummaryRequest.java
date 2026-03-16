package com.xiaou.ai.dto.techbriefing;

import lombok.Data;

/**
 * 科技热点摘要请求
 */
@Data
public class TechBriefingSummaryRequest {

    private String title;

    private String titleZh;

    private String summary;

    private String summaryZh;

    private String contentZh;

    private String sourceName;
}
