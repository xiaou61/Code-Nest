package com.xiaou.oj.dto;

import lombok.Data;

/**
 * 个人做题统计
 *
 * @author xiaou
 */
@Data
public class OjStatisticsVO {

    /**
     * 总提交数
     */
    private Integer totalSubmissions;

    /**
     * 通过的题目数
     */
    private Integer acceptedProblems;

    /**
     * 总尝试题目数
     */
    private Integer attemptedProblems;

    /**
     * 简单题通过数
     */
    private Integer easyAccepted;

    /**
     * 中等题通过数
     */
    private Integer mediumAccepted;

    /**
     * 困难题通过数
     */
    private Integer hardAccepted;
}
