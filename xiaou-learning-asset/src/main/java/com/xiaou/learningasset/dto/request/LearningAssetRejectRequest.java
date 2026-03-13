package com.xiaou.learningasset.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审核驳回请求
 */
@Data
public class LearningAssetRejectRequest {

    @NotBlank(message = "驳回原因不能为空")
    private String note;
}
