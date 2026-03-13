package com.xiaou.learningasset.dto.request;

import com.xiaou.common.core.domain.PageRequest;
import lombok.Data;

/**
 * 审核列表查询请求
 */
@Data
public class LearningAssetReviewQueryRequest implements PageRequest {

    private String assetType;
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    @Override
    public LearningAssetReviewQueryRequest setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    @Override
    public LearningAssetReviewQueryRequest setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }
}
