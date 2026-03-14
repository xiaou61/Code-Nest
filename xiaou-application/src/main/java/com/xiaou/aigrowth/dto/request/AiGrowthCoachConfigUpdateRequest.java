package com.xiaou.aigrowth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI成长教练配置更新请求
 */
@Data
public class AiGrowthCoachConfigUpdateRequest {

    @NotBlank(message = "配置键不能为空")
    private String configKey;

    private String configValue;

    private String remark;

    private String status;
}
