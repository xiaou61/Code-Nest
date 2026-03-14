package com.xiaou.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private String targetRole;

    /**
     * 每周投入时长（小时）
     */
    private Integer weeklyHours;

    /**
     * 周起始日期（周一），为空时使用当前周
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate weekStart;
}

