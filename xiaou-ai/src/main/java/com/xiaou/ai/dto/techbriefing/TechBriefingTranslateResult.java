package com.xiaou.ai.dto.techbriefing;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 科技热点翻译结果
 */
@Data
@Accessors(chain = true)
public class TechBriefingTranslateResult {

    private String titleZh;

    private String summaryZh;

    private String contentZh;

    private String modelName;
}
