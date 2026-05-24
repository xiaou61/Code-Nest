package com.xiaou.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 创建计划请求DTO
 * 
 * @author xiaou
 */
@Data
public class PlanCreateRequest {
    
    /**
     * 计划名称
     */
    @NotBlank(message = "计划名称不能为空")
    @Size(max = 100, message = "计划名称不能超过100个字符")
    private String planName;
    
    /**
     * 计划描述
     */
    @Size(max = 500, message = "计划描述不能超过500个字符")
    private String planDesc;
    
    /**
     * 计划类型：1-刷题 2-学习 3-阅读 4-运动 5-自定义
     */
    @NotNull(message = "计划类型不能为空")
    @Min(value = 1, message = "计划类型不合法")
    @Max(value = 5, message = "计划类型不合法")
    private Integer planType;
    
    /**
     * 目标类型：1-数量 2-时长 3-次数
     */
    @Min(value = 1, message = "目标类型不合法")
    @Max(value = 3, message = "目标类型不合法")
    private Integer targetType;
    
    /**
     * 目标值
     */
    @Min(value = 1, message = "目标值必须大于等于1")
    private Integer targetValue;
    
    /**
     * 目标单位（道/小时/次）
     */
    @Size(max = 20, message = "目标单位不能超过20个字符")
    private String targetUnit;
    
    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate startDate;
    
    /**
     * 结束日期（NULL表示长期）
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate endDate;
    
    /**
     * 每日开始时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private LocalTime dailyStartTime;
    
    /**
     * 每日截止时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private LocalTime dailyEndTime;
    
    /**
     * 重复类型：1-每日 2-工作日 3-周末 4-自定义
     */
    @Min(value = 1, message = "重复类型不合法")
    @Max(value = 4, message = "重复类型不合法")
    private Integer repeatType;
    
    /**
     * 自定义重复日（如：1,2,3,4,5 表示周一到周五）
     */
    @Pattern(regexp = "^$|^[1-7](,[1-7])*$", message = "自定义重复日格式不正确")
    private String repeatDays;
    
    /**
     * 提前提醒分钟数
     */
    @Min(value = 0, message = "提前提醒分钟数不能小于0")
    private Integer remindBefore;
    
    /**
     * 截止提醒分钟数
     */
    @Min(value = 0, message = "截止提醒分钟数不能小于0")
    private Integer remindDeadline;
    
    /**
     * 是否启用提醒：0-否 1-是
     */
    @Min(value = 0, message = "提醒开关不合法")
    @Max(value = 1, message = "提醒开关不合法")
    private Integer remindEnabled;
}
