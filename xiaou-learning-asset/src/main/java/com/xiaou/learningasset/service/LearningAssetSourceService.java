package com.xiaou.learningasset.service;

import com.xiaou.learningasset.dto.request.LearningAssetConvertRequest;
import com.xiaou.learningasset.dto.source.LearningAssetSourceSnapshot;

/**
 * 来源快照服务
 */
public interface LearningAssetSourceService {

    LearningAssetSourceSnapshot loadSnapshot(Long userId, LearningAssetConvertRequest request);
}
