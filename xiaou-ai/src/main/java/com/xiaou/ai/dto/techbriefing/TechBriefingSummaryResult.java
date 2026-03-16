package com.xiaou.ai.dto.techbriefing;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 科技热点摘要结果
 */
@Data
@Accessors(chain = true)
public class TechBriefingSummaryResult {

    private String summary;

    private String whyImportant;

    private String impactScope;

    private List<String> keywords;

    private String modelName;
}
