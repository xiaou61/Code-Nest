package com.xiaou.oj.dto.contest;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事榜单项
 *
 * @author xiaou
 */
@Data
public class ContestRankingItem {

    private Integer rank;

    private Long userId;

    private String nickname;

    private String avatar;

    /**
     * 解题数
     */
    private Integer solvedCount;

    /**
     * 罚时（分钟）
     */
    private Long penalty;

    /**
     * 赛后预估表现分
     */
    private Integer performanceScore;

    /**
     * 赛后预估评分变化
     */
    private Integer ratingChange;

    /**
     * 赛后预估评分
     */
    private Integer ratingAfter;

    /**
     * 最后一次 AC 时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastAcTime;
}
