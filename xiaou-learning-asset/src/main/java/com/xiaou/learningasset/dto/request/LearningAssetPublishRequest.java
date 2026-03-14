package com.xiaou.learningasset.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 发布请求
 */
@Data
public class LearningAssetPublishRequest {

    /**
     * 可选，若为空则发布当前记录下所有已选中候选项
     */
    private List<Long> candidateIds;
}
