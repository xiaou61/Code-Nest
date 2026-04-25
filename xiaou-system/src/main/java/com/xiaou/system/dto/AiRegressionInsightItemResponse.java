package com.xiaou.system.dto;

import lombok.Data;

/**
 * AI 回归洞察计数项。
 *
 * @author xiaou
 */
@Data
public class AiRegressionInsightItemResponse {

    /**
     * 洞察标签，例如失败用例 ID 或失败原因。
     */
    private String label;

    /**
     * 最近窗口内出现次数。
     */
    private Integer count;
}
