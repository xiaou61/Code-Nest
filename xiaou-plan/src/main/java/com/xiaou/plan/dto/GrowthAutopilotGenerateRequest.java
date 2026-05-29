package com.xiaou.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 自动驾驶计划生成请求
 *
 * @author xiaou
 */
@Data
public class GrowthAutopilotGenerateRequest {

    /**
     * 目标岗位
     */
    @Size(max = 50, message = "目标岗位不能超过50个字符")
    private String targetRole;

    /**
     * 每周投入时长（小时）
     */
    @Min(value = 1, message = "每周投入时长必须大于等于1小时")
    @Max(value = 40, message = "每周投入时长不能超过40小时")
    private Integer weeklyHours;

    /**
     * 周起始日期（周一），为空时使用当前周
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate weekStart;
}

