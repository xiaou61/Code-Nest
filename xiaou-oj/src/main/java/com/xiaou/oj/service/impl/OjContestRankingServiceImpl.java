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
}
