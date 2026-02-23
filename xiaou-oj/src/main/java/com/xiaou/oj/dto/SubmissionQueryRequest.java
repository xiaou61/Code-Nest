package com.xiaou.oj.dto;

import lombok.Data;

/**
 * 提交记录查询请求
 *
 * @author xiaou
 */
@Data
public class SubmissionQueryRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**
     * 题目ID
     */
    private Long problemId;

    /**
     * 赛事ID
     */
    private Long contestId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 状态
     */
    private String status;
}
