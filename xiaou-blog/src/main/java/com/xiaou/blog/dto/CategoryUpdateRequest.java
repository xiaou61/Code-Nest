package com.xiaou.blog.dto;

import lombok.Data;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 更新分类请求DTO
 * 
 * @author xiaou
 */
@Data
public class CategoryUpdateRequest {
    
    /**
     * 分类ID
     */
    @Positive(message = "分类ID必须为正数")
    private Long id;
    
    /**
     * 分类名称
     */
    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String categoryName;
    
    /**
     * 分类图标URL
     */
    private String categoryIcon;
    
    /**
     * 分类描述
     */
    @Size(max = 200, message = "分类描述不能超过200个字符")
    private String categoryDescription;
    
    /**
     * 排序权重
     */
    private Integer sortOrder;
    
    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;
}


