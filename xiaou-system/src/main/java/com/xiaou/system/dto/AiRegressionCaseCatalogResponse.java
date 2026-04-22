package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 回归用例目录响应。
 *
 * @author xiaou
 */
@Data
public class AiRegressionCaseCatalogResponse {

    /**
     * 用例总数。
     */
    private Integer totalCount;

    /**
     * 场景列表。
     */
    private List<String> scenarios = new ArrayList<>();

    /**
     * 用例列表。
     */
    private List<AiRegressionCaseCatalogItemResponse> cases = new ArrayList<>();
}
