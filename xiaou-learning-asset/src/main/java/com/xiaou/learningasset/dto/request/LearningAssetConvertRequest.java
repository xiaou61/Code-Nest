package com.xiaou.learningasset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 发起转化请求
 */
@Data
public class LearningAssetConvertRequest {

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @NotNull(message = "来源ID不能为空")
    private Long sourceId;

    private String transformMode = "study";

    @NotEmpty(message = "目标资产类型不能为空")
    private List<String> targetTypes;

    private Boolean useExistingSummary = true;

    private List<String> extraTags;
}
