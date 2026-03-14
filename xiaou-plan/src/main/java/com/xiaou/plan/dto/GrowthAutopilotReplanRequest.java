package com.xiaou.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 自动驾驶计划重排请求
 *
 * @author xiaou
 */
@Data
public class GrowthAutopilotReplanRequest {

    /**
     * 周起始日期（周一），为空时使用当前周
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate weekStart;
}

