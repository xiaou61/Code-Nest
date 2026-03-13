package com.xiaou.learningasset.service;

import com.xiaou.learningasset.dto.source.LearningAssetSourceSnapshot;
import com.xiaou.learningasset.dto.transform.TransformResult;
import com.xiaou.learningasset.enums.TargetAssetType;
import com.xiaou.learningasset.enums.TransformMode;

import java.util.List;

/**
 * 学习资产转化引擎
 */
public interface LearningAssetTransformEngine {

    /**
     * 将来源内容转为候选资产
     */
    TransformResult transform(LearningAssetSourceSnapshot snapshot,
                              TransformMode mode,
                              List<TargetAssetType> targetTypes);
}
