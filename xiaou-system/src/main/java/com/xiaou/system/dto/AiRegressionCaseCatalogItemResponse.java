package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 回归用例目录项响应。
 *
 * @author xiaou
 */
@Data
public class AiRegressionCaseCatalogItemResponse {

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
     * 输入字段列表。
     */
    private List<String> inputKeys = new ArrayList<>();

    /**
     * 是否预期为降级结果。
     */
    private Boolean expectedFallback;
}
