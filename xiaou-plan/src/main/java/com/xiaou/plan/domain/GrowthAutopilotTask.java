package com.xiaou.plan.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成长闭环自动驾驶-任务
 *
 * @author xiaou
 */
@Data
public class GrowthAutopilotTask {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 周目标ID
     */
    private Long goalId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 模块标识
     */
    private String moduleKey;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 任务日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate taskDate;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 计划投入分钟数
     */
    private Integer plannedMinutes;

    /**
     * 任务分值
     */
    private Integer taskScore;

    /**
     * 优先级：P1 / P2 / P3
     */
    private String priority;

    /**
     * 状态：todo / done / missed
     */
    private String status;

    /**
     * 来源：auto / replan
     */
    private String source;

    /**
     * 前端跳转路径
     */
    private String routePath;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completeTime;

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

