package com.xiaou.oj.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * OJ测试用例
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class OjTestCase {

    private Long id;

    /**
     * 所属题目ID
     */
    private Long problemId;

    /**
     * 输入数据
     */
    private String input;

    /**
     * 期望输出
     */
    private String expectedOutput;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 是否为示例用例
     */
    private Boolean isSample;
}
