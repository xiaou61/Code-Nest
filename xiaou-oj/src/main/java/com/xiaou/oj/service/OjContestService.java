package com.xiaou.oj.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.dto.contest.ContestQueryRequest;
import com.xiaou.oj.dto.contest.ContestSaveRequest;

/**
 * OJ 赛事服务
 *
 * @author xiaou
 */
public interface OjContestService {

    /**
     * 创建赛事
     */
    Long createContest(Long adminId, ContestSaveRequest request);

    /**
     * 更新赛事
     */
    void updateContest(Long contestId, ContestSaveRequest request);

    /**
     * 删除赛事
     */
    void deleteContest(Long contestId);

    /**
     * 获取赛事详情
     */
    OjContest getContestById(Long contestId);

    /**
     * 分页查询赛事
     */
    PageResult<OjContest> getContests(ContestQueryRequest request);

    /**
     * 用户端分页查询赛事
     */
    PageResult<OjContest> getPublicContests(ContestQueryRequest request);

    /**
     * 更新赛事状态
     */
    void updateContestStatus(Long contestId, Integer status);

    /**
     * 报名赛事
     */
    void joinContest(Long contestId, Long userId);
}
