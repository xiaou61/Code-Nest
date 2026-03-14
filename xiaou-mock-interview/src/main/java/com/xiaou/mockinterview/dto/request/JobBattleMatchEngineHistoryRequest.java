package com.xiaou.mockinterview.dto.request;

import lombok.Data;

/**
 * 岗位匹配引擎历史查询请求
 *
 * @author xiaou
 */
@Data
public class JobBattleMatchEngineHistoryRequest {

    /**
     * 关键词（匹配分析名称/最佳岗位）
     */
    private String keyword;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}

