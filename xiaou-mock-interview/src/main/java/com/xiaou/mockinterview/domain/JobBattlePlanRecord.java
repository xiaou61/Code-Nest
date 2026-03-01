package com.xiaou.mockinterview.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 求职作战台计划记录
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class JobBattlePlanRecord {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 计划总天数（AI返回）
     */
    private Integer totalDays;

    /**
     * 用户输入目标天数
     */
    private Integer targetDays;

    /**
     * 每周投入小时
     */
    private Integer weeklyHours;

    /**
     * 学习偏好
     */
    private String preferredLearningMode;

    /**
     * 下一场面试日期
     */
    private String nextInterviewDate;

    /**
     * 差距项JSON
     */
    private String gapsJson;

    /**
     * 计划结果JSON
     */
    private String planResultJson;

    /**
     * 是否降级结果
     */
    private Boolean fallback;

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

