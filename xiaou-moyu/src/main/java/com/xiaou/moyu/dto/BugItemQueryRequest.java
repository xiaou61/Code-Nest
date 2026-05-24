package com.xiaou.moyu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Bug条目查询请求DTO
 *
 * @author xiaou
 */
@Data
public class BugItemQueryRequest {
    
    /**
     * 当前页
     */
    @Min(value = 1, message = "当前页最小为1")
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer size = 10;
    
    /**
     * Bug标题（模糊查询）
     */
    private String title;
    
    /**
     * 难度等级
     */
    @Min(value = 1, message = "难度等级最小为1")
    @Max(value = 4, message = "难度等级最大为4")
    private Integer difficultyLevel;
    
    /**
     * 状态：0-禁用，1-启用
     */
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;
    
    /**
     * 技术标签（模糊查询）
     */
    private String techTag;
}
