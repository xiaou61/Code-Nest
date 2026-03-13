package com.xiaou.learningasset.dto.request;

import lombok.Data;

/**
 * 转化记录查询请求
 */
@Data
public class LearningAssetRecordQueryRequest {

    private Long userId;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String sourceType;
    private String status;
}
