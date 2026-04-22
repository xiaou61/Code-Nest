package com.xiaou.ai.eval;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI 回归评测断言定义。
 *
 * @author xiaou
 */
@Data
public class AiEvalExpectation {

    /**
     * 是否预期为降级结果。
     */
    private Boolean fallback;

    /**
     * 精确字符串断言。
     */
    private Map<String, String> exactStrings;

    /**
     * 精确整数断言。
     */
    private Map<String, Integer> exactIntegers;

    /**
     * 整数区间断言。
     */
    private Map<String, AiEvalIntegerRange> intRanges;

    /**
     * 文本包含断言。
     */
    private Map<String, List<String>> textContains;

    /**
     * 列表包含断言。
     */
    private Map<String, List<String>> listContains;

    /**
     * 最小列表长度断言。
     */
    private Map<String, Integer> minListSize;
}
