package com.xiaou.ai.regression;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 回归执行汇总。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class AiRegressionRunSummary {

    /**
     * 过滤后的场景键。
     */
    private String scenario;

    /**
     * 过滤后的用例 ID。
     */
    private String caseId;

    /**
     * 本次执行用例数。
     */
    private Integer totalCount;

    /**
     * 通过数量。
     */
    private Integer passedCount;

    /**
     * 失败数量。
     */
    private Integer failedCount;

    /**
     * 总耗时。
     */
    private Long durationMs;

    /**
     * 执行时间戳。
     */
    private Long executedAt;

    /**
     * 单用例结果列表。
     */
    private List<AiRegressionCaseResult> caseResults = new ArrayList<>();
}
