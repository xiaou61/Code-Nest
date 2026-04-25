package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 回归场景健康聚合项。
 *
 * @author xiaou
 */
@Data
public class AiRegressionScenarioHealthItemResponse {

    /**
     * 场景标识。
     */
    private String scenario;

    /**
     * 最近窗口内参与统计的运行次数。
     */
    private Integer runCount;

    /**
     * 最近窗口内存在失败的运行次数。
     */
    private Integer failedRunCount;

    /**
     * 最近窗口内累计执行的用例数。
     */
    private Integer totalCaseCount;

    /**
     * 最近窗口内累计通过的用例数。
     */
    private Integer passedCaseCount;

    /**
     * 最近窗口内累计失败的用例数。
     */
    private Integer failedCaseCount;

    /**
     * 最近一次运行时间。
     */
    private Long latestExecutedAt;

    /**
     * 最近一次是否全通过。
     */
    private Boolean latestPassed;

    /**
     * 最近一次失败发生时间。
     */
    private Long lastFailedAt;

    /**
     * 最近一次运行中的失败用例数。
     */
    private Integer latestFailedCaseCount;

    /**
     * 最近一次运行中的失败用例 ID。
     */
    private List<String> latestFailedCaseIds = new ArrayList<>();

    /**
     * 最近窗口内高频失败用例。
     */
    private List<AiRegressionInsightItemResponse> topFailedCases = new ArrayList<>();

    /**
     * 最近窗口内高频失败原因。
     */
    private List<AiRegressionInsightItemResponse> topFailureReasons = new ArrayList<>();

    /**
     * 最近窗口内高频退化模型。
     */
    private List<AiRegressionInsightItemResponse> topModelNames = new ArrayList<>();

    /**
     * 最近窗口内高影响图编排。
     */
    private List<AiRegressionInsightItemResponse> topGraphNames = new ArrayList<>();

    /**
     * 最近窗口内高影响 Prompt。
     */
    private List<AiRegressionInsightItemResponse> topPromptIds = new ArrayList<>();
}
