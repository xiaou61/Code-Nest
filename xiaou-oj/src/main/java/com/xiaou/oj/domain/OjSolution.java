package com.xiaou.oj.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * OJ标准答案 (题解)
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class OjSolution {

    private Long id;

    /**
     * 所属题目ID
     */
    private Long problemId;

    /**
     * 编程语言 (java/cpp/python 等)
     */
    private String language;

    /**
     * 题解标题 (如 "哈希表解法")
     */
    private String title;

    /**
     * 标准答案代码
     */
    private String code;

    /**
     * 题解说明 (Markdown, 思路讲解)
     */
    private String description;

    /**
     * 排序号
     */
    private Integer sortOrder;
}
