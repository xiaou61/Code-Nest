package com.xiaou.ai.regression;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI 黄金样例回归用例。
 *
 * @author xiaou
 */
@Data
public class AiRegressionCase {

    /**
     * 用例唯一标识。
     */
    private String id;

    /**
     * 场景键。
     */
    private String scenario;

    /**
     * 用例说明。
     */
    private String description;

    /**
     * 测试输入。
     */
    private Map<String, Object> input;

    /**
     * 单步场景模型返回。
     */
    private String response;

    /**
     * 多步场景模型返回序列。
     */
    private List<String> responses;

    /**
     * 是否直接走降级。
     */
    private boolean useFallback;

    /**
     * 多步场景降级序列。
     */
    private List<Boolean> fallbackSequence;

    /**
     * 断言预期。
     */
    private AiRegressionExpectation expect;
}
