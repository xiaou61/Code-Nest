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
     * 最后一次 AC 时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastAcTime;
}
