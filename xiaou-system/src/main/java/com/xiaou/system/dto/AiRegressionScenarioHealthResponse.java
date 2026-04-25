package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 回归场景健康聚合响应。
 *
 * @author xiaou
 */
@Data
public class AiRegressionScenarioHealthResponse {

    /**
     * 本次统计窗口上限。
     */
    private Integer limit;

    /**
     * 当前返回的场景条数。
     */
    private Integer totalCount;

    /**
     * 场景健康聚合列表。
     */
    private List<AiRegressionScenarioHealthItemResponse> scenarios = new ArrayList<>();
}
