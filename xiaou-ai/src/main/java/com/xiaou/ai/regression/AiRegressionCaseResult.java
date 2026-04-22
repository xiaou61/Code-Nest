package com.xiaou.ai.regression;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 回归单用例执行结果。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRegressionCaseResult {

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
     * 本用例耗时。
     */
    private Long durationMs;

    /**
     * 失败原因列表。
     */
    private List<String> failureReasons = new ArrayList<>();
}
