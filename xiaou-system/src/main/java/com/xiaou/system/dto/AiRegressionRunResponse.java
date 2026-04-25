package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 回归执行结果响应。
 *
 * @author xiaou
 */
@Data
public class AiRegressionRunResponse {

    /**
     * 场景过滤。
     */
    private String scenario;

    /**
     * 单用例过滤。
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
    private List<AiRegressionCaseResultResponse> caseResults = new ArrayList<>();
}
