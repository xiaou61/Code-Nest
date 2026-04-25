package com.xiaou.system.dto;

import lombok.Data;

/**
 * AI 回归执行请求。
 *
 * @author xiaou
 */
@Data
public class AiRegressionRunRequest {

    /**
     * 可选场景过滤。
     */
    private String scenario;

    /**
     * 可选单用例过滤。
     */
    private String caseId;
}
