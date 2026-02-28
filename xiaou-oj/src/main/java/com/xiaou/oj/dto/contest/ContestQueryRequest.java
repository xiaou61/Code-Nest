package com.xiaou.oj.dto.contest;

import lombok.Data;

/**
 * 赛事查询请求
 *
 * @author xiaou
 */
@Data
public class ContestQueryRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**
     * 关键字（标题）
     */
    private String keyword;

    /**
     * 赛事类型 (weekly/challenge)
     */
    private String contestType;

    /**
     * 状态
     */
    private Integer status;
}
