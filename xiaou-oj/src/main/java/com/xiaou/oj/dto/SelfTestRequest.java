package com.xiaou.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 自测请求
 *
 * @author xiaou
 */
@Data
public class SelfTestRequest {

    @NotNull(message = "题目ID不能为空")
    private Long problemId;

    @NotBlank(message = "语言不能为空")
    private String language;

    @NotBlank(message = "代码不能为空")
    private String code;
}
