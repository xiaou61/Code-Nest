package com.xiaou.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员提交的 RCA 调查反馈。
 *
 * @author xiaou
 */
public record SreRcaFeedbackRequest(
        @NotBlank(message = "RCA 准确度评价不能为空")
        @Pattern(regexp = "ACCURATE|PARTIAL|INACCURATE", message = "RCA 准确度评价不合法")
        String accuracy,

        @Pattern(
                regexp = "RETRIEVAL_GAP|REASONING_GAP|TOOL_FAILURE|ROUTING_GAP|UNKNOWN",
                message = "RCA 缺口类型不合法"
        )
        String gapType,

        @Size(max = 1000, message = "管理员备注不能超过1000个字符")
        String note,

        @Size(max = 2000, message = "期望结论不能超过2000个字符")
        String expectedConclusion
) {
}
