package com.xiaou.learningasset.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 合并到已有内容请求
 */
@Data
public class LearningAssetMergeRequest {

    @NotNull(message = "目标内容ID不能为空")
    private Long existingTargetId;

    private String note;
}
