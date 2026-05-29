package com.xiaou.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端文章列表查询请求DTO
 * 
 * @author xiaou
 */
@Data
public class AdminArticleListRequest {
    
    /**
     * 用户ID
     */
    @Positive(message = "用户ID必须为正数")
    private Long userId;
    
    /**
     * 分类ID
     */
    @Positive(message = "分类ID必须为正数")
    private Long categoryId;
    
    /**
     * 文章状态
     */
    @Min(value = 0, message = "文章状态不合法")
    @Max(value = 3, message = "文章状态不合法")
    private Integer status;
    
    /**
     * 关键词搜索
     */
    @Size(max = 100, message = "关键词长度不能超过100个字符")
    private String keyword;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
    
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
}


