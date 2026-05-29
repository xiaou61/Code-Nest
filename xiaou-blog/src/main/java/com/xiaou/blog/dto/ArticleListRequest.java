package com.xiaou.blog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章列表查询请求DTO
 * 
 * @author xiaou
 */
@Data
public class ArticleListRequest {
    
    /**
     * 用户ID（查询指定用户的文章）
     */
    @Positive(message = "用户ID必须为正数")
    private Long userId;
    
    /**
     * 分类ID
     */
    @Positive(message = "分类ID必须为正数")
    private Long categoryId;
    
    /**
     * 标签（支持多标签组合筛选）
     */
    @Size(max = 50, message = "标签长度不能超过50个字符")
    private String tags;
    
    /**
     * 关键词搜索（标题、内容）
     */
    @Size(max = 100, message = "关键词长度不能超过100个字符")
    private String keyword;
    
    /**
     * 文章状态：0-草稿 1-已发布 2-已下架 3-已删除
     */
    private Integer status;
    
    /**
     * 页码
     */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页数量必须大于等于1")
    private Integer pageSize = 10;
}


