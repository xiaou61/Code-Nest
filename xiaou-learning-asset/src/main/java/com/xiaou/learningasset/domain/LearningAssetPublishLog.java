package com.xiaou.learningasset.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 学习资产发布日志
 */
@Data
@Accessors(chain = true)
public class LearningAssetPublishLog {

    private Long id;
    private Long candidateId;
    private String publishType;
    private String targetModule;
    private Long targetId;
    private Long operatorId;
    private String publishResult;
    private String message;
    private LocalDateTime createTime;
}
