package com.xiaou.aigrowth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI成长教练动作状态更新请求
 */
@Data
public class AiGrowthCoachActionStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}
