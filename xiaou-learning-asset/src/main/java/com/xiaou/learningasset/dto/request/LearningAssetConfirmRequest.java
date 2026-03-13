package com.xiaou.learningasset.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 候选确认请求
 */
@Data
public class LearningAssetConfirmRequest {

    @NotEmpty(message = "至少选择一个候选项")
    private List<Long> candidateIds;
}
