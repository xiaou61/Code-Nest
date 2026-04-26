package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 回归执行历史响应。
 *
 * @author xiaou
 */
@Data
public class AiRegressionRunHistoryResponse {

    /**
     * 本次返回上限。
     */
    private Integer limit;

    /**
     * 当前已保存的历史条数。
     */
    private Integer totalCount;

    /**
     * 历史执行记录。
     */
    private List<AiRegressionRunResponse> runs = new ArrayList<>();
}
