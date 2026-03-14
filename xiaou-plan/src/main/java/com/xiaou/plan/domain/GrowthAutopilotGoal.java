package com.xiaou.plan.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成长闭环自动驾驶-周目标
 *
 * @author xiaou
 */
@Data
public class GrowthAutopilotGoal {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 周起始日期（周一）
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate weekStart;

    /**
     * 周结束日期（周日）
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate weekEnd;

    /**
     * 目标岗位
     */
    private String targetRole;

    /**
     * 每周投入时长（小时）
     */
    private Integer weeklyHours;

    /**
     * 目标总分
     */
    private Integer totalScoreTarget;

    /**
     * 已完成总分
     */
    private Integer totalScoreCompleted;

    /**
     * 任务总数
     */
    private Integer totalTasks;

    /**
     * 已完成任务数
     */
    private Integer completedTasks;

    /**
     * 完成率（0-100）
     */
    private Integer completionRate;

    /**
     * 风险等级：low / medium / high
     */
    private String riskLevel;

    /**
     * 状态：active / archived
     */
    private String status;

    /**
     * 计划生成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generatedAt;

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

