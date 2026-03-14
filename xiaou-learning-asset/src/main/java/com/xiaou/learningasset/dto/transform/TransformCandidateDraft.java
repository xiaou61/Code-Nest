package com.xiaou.learningasset.dto.transform;

import com.xiaou.learningasset.enums.TargetAssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转化候选草稿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformCandidateDraft {

    private TargetAssetType assetType;

    private String title;

    private String contentJson;

    private String tags;

    private Double confidenceScore;

    private String targetModule;
}
