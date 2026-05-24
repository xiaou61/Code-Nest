package com.xiaou.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建打卡任务请求DTO
 * 
 * @author xiaou
 */
@Data
public class TaskCreateRequest {
    
    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称不能超过100个字符")
    private String taskName;
    
    /**
     * 任务描述
     */
    @Size(max = 500, message = "任务描述不能超过500个字符")
    private String taskDesc;
    
    /**
     * 类型：1刷题 2学习时长 3阅读 4自定义
     */
    @NotNull(message = "任务类型不能为空")
    @Min(value = 1, message = "任务类型不合法")
    @Max(value = 4, message = "任务类型不合法")
    private Integer taskType;
    
    /**
     * 目标数量
     */
    @Min(value = 1, message = "目标数量必须大于0")
    private Integer targetValue;
    
    /**
     * 目标单位
     */
    @Size(max = 20, message = "目标单位不能超过20个字符")
    private String targetUnit;
    
    /**
     * 重复：1每日 2工作日 3自定义
     */
    @Min(value = 1, message = "重复类型不合法")
    @Max(value = 3, message = "重复类型不合法")
    private Integer repeatType;
    
    /**
     * 自定义重复日（如：1,2,3,4,5）
     */
    @Size(max = 32, message = "自定义重复日长度不合法")
    private String repeatDays;
    
    /**
     * 是否必须附带内容
     */
    @Min(value = 0, message = "内容要求值不合法")
    @Max(value = 1, message = "内容要求值不合法")
    private Integer requireContent;
    
    /**
     * 是否必须附带图片
     */
    @Min(value = 0, message = "图片要求值不合法")
    @Max(value = 1, message = "图片要求值不合法")
    private Integer requireImage;
    
    /**
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate endDate;
}
