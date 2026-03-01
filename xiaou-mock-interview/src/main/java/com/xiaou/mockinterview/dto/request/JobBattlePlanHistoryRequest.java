package com.xiaou.mockinterview.dto.request;

import com.xiaou.common.core.domain.PageRequest;
import lombok.Data;

/**
 * 求职作战台-计划历史查询请求
 *
 * @author xiaou
 */
@Data
public class JobBattlePlanHistoryRequest implements PageRequest {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 关键词（计划名）
     */
    private String keyword;

    @Override
    public PageRequest setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    @Override
    public PageRequest setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }
}

