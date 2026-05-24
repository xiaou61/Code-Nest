package com.xiaou.points.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户积分列表查询请求DTO
 * 
 * @author xiaou
 */
@Data
public class UserPointsListRequest {
    
    /**
     * 页码
     */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页数量必须大于等于1")
    private Integer pageSize = 20;
    
    /**
     * 用户名模糊搜索
     */
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String userName;
    
    /**
     * 最小积分
     */
    @Min(value = 0, message = "最小积分不能小于0")
    private Integer minPoints;
    
    /**
     * 最大积分
     */
    @Min(value = 0, message = "最大积分不能小于0")
    private Integer maxPoints;
    
    /**
     * 排序字段：points-积分, create_time-创建时间
     */
    @Pattern(regexp = "^(points|create_time)$", message = "排序字段不合法")
    private String orderBy = "points";
    
    /**
     * 排序方向：asc-升序, desc-降序
     */
    @Pattern(regexp = "^(asc|desc)$", message = "排序方向不合法")
    private String orderDirection = "desc";
}
