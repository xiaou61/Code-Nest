package com.xiaou.codepen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新标签请求
 *
 * @author xiaou
 */
@Data
public class CodePenTagUpdateRequest {

    /**
     * 标签ID
     */
    @NotNull(message = "标签ID不能为空")
    @Positive(message = "标签ID必须为正数")
    private Long id;

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
