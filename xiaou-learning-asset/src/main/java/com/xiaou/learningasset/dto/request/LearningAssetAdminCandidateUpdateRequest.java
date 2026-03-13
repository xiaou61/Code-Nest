package com.xiaou.learningasset.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端审核候选编辑请求
 */
@Data
public class LearningAssetAdminCandidateUpdateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String contentJson;

    private String tags;

    private String difficulty;
}
