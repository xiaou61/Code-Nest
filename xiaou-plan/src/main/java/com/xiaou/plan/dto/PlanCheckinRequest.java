package com.xiaou.plan.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 计划打卡请求DTO
 * 
 * @author xiaou
 */
@Data
public class PlanCheckinRequest {
    
    /**
     * 计划ID
     */
    @NotNull(message = "计划ID不能为空")
    @Positive(message = "计划ID必须为正数")
    private Long planId;
    
    /**
     * 完成数量
     */
    @JsonAlias("actualValue")
    @NotNull(message = "完成数量不能为空")
    @Min(value = 1, message = "完成数量必须大于等于1")
    private Integer completeValue;
    
    /**
     * 完成内容描述
     */
    @Size(max = 500, message = "完成内容描述不能超过500个字符")
    private String completeContent;
    
    /**
     * 心得备注
     */
    @Size(max = 500, message = "心得备注不能超过500个字符")
    private String remark;
}
