package com.xiaou.mockinterview.dto.response.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端面试统计总览
 *
 * @author xiaou
 */
@Data
public class AdminInterviewStatsOverviewResponse {

    /**
     * 总会话数
     */
    private Long totalSessions;

    /**
     * 已完成会话数
     */
    private Long completedSessions;

    /**
     * 进行中会话数
     */
    private Long ongoingSessions;

    /**
     * 已中断会话数
     */
    private Long interruptedSessions;

    /**
     * 完成率（百分比）
     */
    private BigDecimal completionRate;

    /**
     * 平均分
     */
    private BigDecimal avgScore;

    /**
     * 平均面试时长（分钟）
     */
    private Integer avgDurationMinutes;

    /**
     * 活跃用户数（去重）
     */
    private Long activeUsers;

    /**
     * 方向分布
     */
    private List<DirectionDistribution> directionDistributions;

    @Data
    public static class DirectionDistribution {
        /**
         * 方向代码
         */
        private String direction;

        /**
         * 方向名称
         */
        private String directionName;

        /**
         * 会话数
         */
        private Long sessionCount;

        /**
         * 方向平均分
         */
        private BigDecimal avgScore;

        /**
         * 完成率（百分比）
         */
        private BigDecimal completionRate;
    }
}

