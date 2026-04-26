package com.xiaou.codepen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论发表请求
 * 
 * @author xiaou
 */
@Data
public class CommentCreateRequest {
    
    /**
     * 作品ID（必填）
     */
    @NotNull(message = "作品ID不能为空")
    @Positive(message = "作品ID必须为正数")
    private Long penId;
    
    /**
     * 评论内容（必填，1-500字符）
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500个字符")
    private String content;
    
    /**
     * 父评论ID（回复时传入）
     */
    @Positive(message = "父评论ID必须为正数")
    private Long parentId;
    
    /**
     * 回复目标用户ID（回复时传入）
     */
    @Positive(message = "回复目标用户ID必须为正数")
    private Long replyToUserId;
}

