package com.xiaou.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 计划列表查询请求DTO
 * 
 * @author xiaou
 */
@Data
public class PlanListRequest {
    
    /**
     * 用户ID（内部使用）
     */
    @Positive(message = "用户ID必须为正数")
    private Long userId;
    
    /**
     * 计划状态筛选
     */
    @Min(value = 0, message = "计划状态不合法")
    @Max(value = 4, message = "计划状态不合法")
    private Integer status;
    
    /**
     * 计划类型筛选
     */
    @Min(value = 1, message = "计划类型不合法")
    @Max(value = 5, message = "计划类型不合法")
    private Integer planType;
    
    /**
     * 搜索关键字
     */
    @Size(max = 100, message = "搜索关键字不能超过100个字符")
    private String keyword;
    
    /**
     * 当前页码
     */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;
    
    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量必须大于等于1")
    private Integer pageSize = 10;
}
