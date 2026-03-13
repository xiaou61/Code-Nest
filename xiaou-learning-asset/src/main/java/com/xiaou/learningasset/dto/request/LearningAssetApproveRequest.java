package com.xiaou.learningasset.dto.request;

import lombok.Data;

/**
 * 审核通过请求
 */
@Data
public class LearningAssetApproveRequest {

    private Long mapId;
    private Long parentId = 0L;
    private Long questionSetId;
    private String note;
}
