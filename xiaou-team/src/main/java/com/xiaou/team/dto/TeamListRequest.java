package com.xiaou.team.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 小组列表查询请求DTO
 * 
 * @author xiaou
 */
@Data
public class TeamListRequest {
    
    /**
     * 当前页码
     */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;
    
    /**
     * 每页条数
     */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 10;
    
    /**
     * 小组类型筛选
     */
    @Min(value = 1, message = "小组类型不合法")
    @Max(value = 3, message = "小组类型不合法")
    private Integer teamType;
    
    /**
     * 标签筛选
     */
    @Size(max = 20, message = "标签长度不能超过20个字符")
    private String tag;
    
    /**
     * 关键字搜索（小组名称）
     */
    @Size(max = 50, message = "关键字长度不能超过50个字符")
    private String keyword;
    
    /**
     * 排序方式：hot-热门 new-最新 active-活跃
     */
    @Pattern(regexp = "hot|new|active", message = "排序方式不合法")
    private String sortBy = "hot";
    
    /**
     * 用户ID（用于查询我的小组）
     */
    private Long userId;
    
    /**
     * 只查询可加入的小组
     */
    private Boolean joinable;
}
