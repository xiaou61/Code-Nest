package com.xiaou.ai.eval;

import lombok.Data;

/**
 * 整数区间断言。
 *
 * @author xiaou
 */
@Data
public class AiEvalIntegerRange {

    /**
     * 最小值。
     */
    private Integer min;

    /**
     * 最大值。
     */
    private Integer max;
}
