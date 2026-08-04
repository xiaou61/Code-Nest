package com.xiaou.web.growthcoach.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 自然语言计划调整预览请求。
 */
@Data
public class GrowthCoachPreviewRequest {

    @NotBlank(message = "请描述本周计划需要如何调整")
    @Size(max = 1000, message = "计划调整描述不能超过1000个字符")
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate weekStart;

    @NotBlank(message = "clientRequestId不能为空")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-]{7,79}$", message = "clientRequestId格式不正确")
    private String clientRequestId;
}
