package com.xiaou.oj.service;

import com.xiaou.oj.dto.contest.ContestRankingItem;

import java.util.List;

/**
 * OJ 赛事榜单服务
 *
 * @author xiaou
 */
public interface OjContestRankingService {

    /**
     * 查询赛事榜单
     */
    List<ContestRankingItem> getContestRanking(Long contestId);
}
