package com.xiaou.oj.service.impl;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.oj.contest.ContestRankingCalculator;
import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.domain.OjContestParticipant;
import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.dto.contest.ContestRankingItem;
import com.xiaou.oj.mapper.OjContestMapper;
import com.xiaou.oj.mapper.OjContestParticipantMapper;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import com.xiaou.oj.service.OjContestRankingService;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OJ 赛事榜单服务实现
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class OjContestRankingServiceImpl implements OjContestRankingService {

    private static final int DEFAULT_CONTEST_RATING = 1500;
    private static final int MIN_RATING_CHANGE = -80;
    private static final int MAX_RATING_CHANGE = 120;

    private final OjContestMapper contestMapper;
    private final OjSubmissionMapper submissionMapper;
    private final OjContestParticipantMapper participantMapper;
    private final ContestRankingCalculator calculator;
    private final UserInfoApiService userInfoApiService;

    @Override
    public List<ContestRankingItem> getContestRanking(Long contestId) {
        OjContest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException("赛事不存在");
        }

        List<OjSubmission> submissions = submissionMapper.selectByContestId(contestId);
        List<OjContestParticipant> participants = participantMapper.selectByContestId(contestId);
        List<ContestRankingItem> ranking = calculator.calculate(contest, submissions, participants);
        enrichUserInfo(ranking);
        enrichRatingEstimates(ranking);
        return ranking;
    }

    private void enrichUserInfo(List<ContestRankingItem> ranking) {
        if (ranking == null || ranking.isEmpty()) {
            return;
        }
        List<Long> userIds = ranking.stream().map(ContestRankingItem::getUserId).collect(Collectors.toList());
        Map<Long, SimpleUserInfo> userInfoMap = userInfoApiService.getSimpleUserInfoBatch(userIds);
        for (ContestRankingItem item : ranking) {
            SimpleUserInfo userInfo = userInfoMap.get(item.getUserId());
            if (userInfo != null) {
                item.setNickname(userInfo.getDisplayName());
                item.setAvatar(userInfo.getAvatar());
            } else {
                item.setNickname("用户" + item.getUserId());
            }
        }
    }

    private void enrichRatingEstimates(List<ContestRankingItem> ranking) {
        if (ranking == null || ranking.isEmpty()) {
            return;
        }

        int total = ranking.size();
        int maxSolvedCount = ranking.stream()
                .map(ContestRankingItem::getSolvedCount)
                .filter(count -> count != null && count > 0)
                .max(Integer::compareTo)
                .orElse(1);

        for (ContestRankingItem item : ranking) {
            int rank = item.getRank() == null ? total : item.getRank();
            int solvedCount = item.getSolvedCount() == null ? 0 : item.getSolvedCount();
            long penalty = item.getPenalty() == null ? 0L : item.getPenalty();

            int rankScore = Math.round((total - rank + 1) * 100.0F / total);
            int solveScore = Math.round(solvedCount * 100.0F / maxSolvedCount);
            int speedScore = solvedCount <= 0 ? 0 : Math.max(0, 100 - (int) Math.min(100L, penalty / 10L));
            int performanceScore = Math.round(solveScore * 0.55F + rankScore * 0.3F + speedScore * 0.15F);

            int rankDelta = Math.round((total + 1 - 2 * rank) * 60.0F / total);
            int solveDelta = solvedCount <= 0 ? -20 : Math.min(40, solvedCount * 10);
            int penaltyDelta = solvedCount <= 0 ? 0 : -(int) Math.min(25L, penalty / 120L);
            int ratingChange = clamp(rankDelta + solveDelta + penaltyDelta, MIN_RATING_CHANGE, MAX_RATING_CHANGE);

            item.setPerformanceScore(performanceScore);
            item.setRatingChange(ratingChange);
            item.setRatingAfter(DEFAULT_CONTEST_RATING + ratingChange);
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
