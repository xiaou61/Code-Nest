package com.xiaou.oj.dto;

import lombok.Data;

/**
 * 排行榜项
 *
 * @author xiaou
 */
@Data
public class RankingItem {

    private Long userId;

    private String nickname;

    private String avatar;

    /**
     * AC 题目数（去重）
     */
    private int acceptedCount;

    /**
     * 总提交数
     */
    private int submissionCount;

    /**
     * 排名
     */
    private int rank;
}
