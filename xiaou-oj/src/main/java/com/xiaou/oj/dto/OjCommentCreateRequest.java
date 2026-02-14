package com.xiaou.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * OJ评论创建请求
 *
 * @author xiaou
 */
@Data
public class OjCommentCreateRequest {

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 1000, message = "评论内容为1-1000个字符")
    private String content;
}
