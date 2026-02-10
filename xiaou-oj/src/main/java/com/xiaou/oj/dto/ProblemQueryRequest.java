package com.xiaou.oj.dto;

import lombok.Data;

/**
 * 题目查询请求
 *
 * @author xiaou
 */
@Data
public class ProblemQueryRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**
     * 关键词 (标题搜索)
     */
    private String keyword;

    /**
     * 难度筛选
     */
    private String difficulty;

    /**
     * 标签ID筛选
     */
    private Long tagId;

    /**
     * 状态筛选 (管理端使用)
     */
    private Integer status;
}
