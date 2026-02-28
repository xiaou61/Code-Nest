package com.xiaou.oj.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * OJ题目标签
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class OjProblemTag {

    private Long id;

    /**
     * 标签名称
     */
    private String name;
}
