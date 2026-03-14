package com.xiaou.plan.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习成长驾驶舱-排名快照
 */
@Data
public class LearningCockpitRankSnapshot {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 周起始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate weekStart;

    /**
     * 周结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate weekEnd;

    /**
     * 周榜排名
     */
    private Integer weeklyRank;

    /**
     * 总榜排名
     */
    private Integer allRank;

    /**
     * 周榜参与人数
     */
    private Integer weeklyPopulation;

    /**
     * 总榜参与人数
     */
    private Integer allPopulation;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}

