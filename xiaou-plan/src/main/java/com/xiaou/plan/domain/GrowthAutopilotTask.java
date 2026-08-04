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
     * 跨版本关联任务键。
     */
    private String taskKey;

    /**
     * 所属计划版本。
     */
    private Integer planVersion;

    /**
     * 可执行资源类型。
     */
    private String resourceType;

    /**
     * 可执行资源业务标识。
     */
    private String resourceId;

    /**
     * 资源版本或快照标识。
     */
    private String resourceVersion;

    /**
     * 任务被选入当前计划的原因。
     */
    private String selectionReason;

    /**
     * 完成校验规则 JSON。
     */
    private String completionRuleJson;

    /**
     * 替代当前任务的新任务 ID，P0 预留。
     */
    private Long supersededByTaskId;

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

