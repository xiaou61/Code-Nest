package com.xiaou.codepen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建标签请求
 *
 * @author xiaou
 */
@Data
public class CodePenTagCreateRequest {

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称不能超过50个字符")
    private String tagName;

    /**
     * 标签描述
     */
    @Size(max = 200, message = "标签描述不能超过200个字符")
    private String tagDescription;
}
