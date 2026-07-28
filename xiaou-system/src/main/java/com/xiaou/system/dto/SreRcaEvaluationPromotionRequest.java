package com.xiaou.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 将指定反馈修订提升为不可变 RCA 评测用例。
 *
 * @author xiaou
 */
public record SreRcaEvaluationPromotionRequest(
        @NotNull(message = "反馈修订 ID 不能为空")
        @Min(value = 1, message = "反馈修订 ID 不合法")
        Long feedbackId
) {
}
