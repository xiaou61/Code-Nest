package com.xiaou.codepen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 作品列表查询请求
 * 
 * @author xiaou
 */
@Data
public class CodePenListRequest {
    
    /**
     * 作者ID
     */
    private Long userId;
    
    /**
     * 分类
     */
    private String category;
    
    /**
     * 标签筛选
     */
    @Size(max = 5, message = "标签最多选择5个")
    private List<String> tags;
    
    /**
     * 关键词搜索
     */
    private String keyword;
    
    /**
     * 是否免费筛选：1-免费 0-付费
     */
    @Min(value = 0, message = "是否免费参数错误")
    @Max(value = 1, message = "是否免费参数错误")
    private Integer isFree;
    
    /**
     * 排序方式：latest/hot/most_liked/most_viewed，默认latest
     */
    @Pattern(regexp = "^(latest|hot|most_liked|most_viewed)?$", message = "排序方式参数错误")
    private String sortBy;
    
    /**
     * 状态筛选
     */
    @Min(value = 0, message = "状态参数错误")
    @Max(value = 3, message = "状态参数错误")
    private Integer status;
    
    /**
     * 页码，默认1
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum;
    
    /**
     * 每页大小，默认20
     */
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize;
}

