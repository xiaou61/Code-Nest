package com.xiaou.mockinterview.dto.request;

import com.xiaou.common.core.domain.PageRequest;
import lombok.Data;

/**
 * 面试历史查询请求DTO
 *
 * @author xiaou
 */
@Data
public class InterviewHistoryRequest implements PageRequest {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 面试方向筛选
     */
    private String direction;

    /**
     * 状态筛选
     */
    private Integer status;

    /**
     * 用户ID筛选（管理端）
     */
    private Long userId;

    /**
     * 开始时间筛选
     */
    private String startTime;

    /**
     * 结束时间筛选
     */
    private String endTime;

    /**
     * 最低分筛选
     */
    private Integer minScore;

    /**
     * 最高分筛选
     */
    private Integer maxScore;

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
