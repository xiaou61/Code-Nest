package com.xiaou.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交代码请求
 *
 * @author xiaou
 */
@Data
public class SubmitCodeRequest {

    /**
     * 题目ID
     */
    @NotNull(message = "题目ID不能为空")
    private Long problemId;

    /**
     * 编程语言
     */
    @NotBlank(message = "编程语言不能为空")
    private String language;

    /**
     * 源代码
     */
    @NotBlank(message = "代码不能为空")
    private String code;
}
