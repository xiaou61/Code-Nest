package com.xiaou.ai.dto.techbriefing;

import lombok.Data;

/**
 * 科技热点翻译请求
 */
@Data
public class TechBriefingTranslateRequest {

    private String title;

    private String summary;

    private String content;

    private String sourceName;
}
