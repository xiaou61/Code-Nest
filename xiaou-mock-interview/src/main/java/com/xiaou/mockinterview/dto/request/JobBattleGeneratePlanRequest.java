package com.xiaou.mockinterview.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 求职作战台-计划生成请求
 *
 * @author xiaou
 */
@Data
public class JobBattleGeneratePlanRequest {

    /**
     * 差距项JSON
     */
    @NotBlank(message = "差距项不能为空")
    private String gapsJson;

    /**
     * 计划天数
     */
    @Min(value = 1, message = "计划天数必须大于0")
    private Integer targetDays = 30;

    /**
     * 每周投入时长（小时）
     */
    @Min(value = 1, message = "每周投入时长必须大于0")
    private Integer weeklyHours = 6;

    /**
     * 偏好学习方式（可选）
     */
    private String preferredLearningMode;

    /**
     * 下一场面试日期（可选，yyyy-MM-dd）
     */
    private String nextInterviewDate;
}

