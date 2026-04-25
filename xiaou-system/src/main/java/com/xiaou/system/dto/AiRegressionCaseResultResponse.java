package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 回归单用例结果响应。
 *
 * @author xiaou
 */
@Data
public class AiRegressionCaseResultResponse {

    /**
     * 用例 ID。
     */
    private String caseId;

    /**
     * 场景键。
     */
    private String scenario;

    /**
     * 用例说明。
     */
    private String description;

    /**
     * 是否通过。
     */
    private boolean passed;

    /**
     * 期望降级标记。
     */
    private Boolean expectedFallback;

    /**
     * 实际降级标记。
     */
    private Boolean actualFallback;

    /**
     * 单用例耗时。
     */
    private Long durationMs;

    /**
     * 本次回归使用的模型名称。
     */
    private String modelName;

    /**
     * 当前用例命中的图编排名称。
     */
    private String graphName;

    /**
     * 当前用例涉及的 Prompt 标识列表。
     */
    private List<String> promptIds = new ArrayList<>();

    /**
     * 失败原因。
     */
    private List<String> failureReasons = new ArrayList<>();
}
