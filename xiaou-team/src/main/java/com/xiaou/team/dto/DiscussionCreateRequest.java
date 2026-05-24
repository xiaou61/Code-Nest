package com.xiaou.team.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建讨论请求DTO
 * 
 * @author xiaou
 */
@Data
public class DiscussionCreateRequest {
    
    /**
     * 分类：1公告 2问答 3笔记 4经验 5闲聊
     */
    @Min(value = 1, message = "讨论分类不合法")
    @Max(value = 5, message = "讨论分类不合法")
    private Integer category;
    
    /**
     * 标题
     */
    @NotBlank(message = "讨论标题不能为空")
    @Size(max = 100, message = "讨论标题不能超过100个字符")
    private String title;
    
    /**
     * 内容
     */
    @NotBlank(message = "讨论内容不能为空")
    @Size(max = 5000, message = "讨论内容不能超过5000个字符")
    private String content;
    
    /**
     * 图片列表
     */
    private List<String> images;
    
    /**
     * 是否置顶
     */
    @Min(value = 0, message = "置顶状态不合法")
    @Max(value = 1, message = "置顶状态不合法")
    private Integer isTop;
    
    /**
     * 是否精华
     */
    @Min(value = 0, message = "精华状态不合法")
    @Max(value = 1, message = "精华状态不合法")
    private Integer isEssence;
}
