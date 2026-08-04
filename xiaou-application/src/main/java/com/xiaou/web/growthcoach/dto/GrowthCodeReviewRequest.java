package com.xiaou.web.growthcoach.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 对已保存 CodePen 版本发起审查的请求。
 */
@Data
public class GrowthCodeReviewRequest {

    @NotNull(message = "penId不能为空")
    @Positive(message = "penId必须为正数")
    private Long penId;
}
