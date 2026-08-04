package com.xiaou.web.growthcoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 计划调整确认请求。
 */
@Data
public class GrowthCoachConfirmRequest {

    @NotBlank(message = "预览摘要不能为空")
    @Size(max = 80, message = "预览摘要格式不正确")
    private String previewHash;
}
