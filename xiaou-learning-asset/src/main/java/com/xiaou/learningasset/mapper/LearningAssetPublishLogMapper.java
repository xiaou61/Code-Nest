package com.xiaou.learningasset.mapper;

import com.xiaou.learningasset.domain.LearningAssetPublishLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发布日志 Mapper
 */
@Mapper
public interface LearningAssetPublishLogMapper {

    int insert(LearningAssetPublishLog publishLog);
}
