package com.xiaou.mockinterview.service.impl;

import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.service.AiJobBattleService;
import com.xiaou.mockinterview.dto.request.JobBattleGeneratePlanRequest;
import com.xiaou.mockinterview.dto.request.JobBattleInterviewReviewRequest;
import com.xiaou.mockinterview.dto.request.JobBattleParseJdRequest;
import com.xiaou.mockinterview.dto.request.JobBattleResumeMatchRequest;
import com.xiaou.mockinterview.service.JobBattleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 求职作战台服务实现
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class JobBattleServiceImpl implements JobBattleService {

    private final AiJobBattleService aiJobBattleService;

    @Override
    public JobBattleJdParseResult parseJd(JobBattleParseJdRequest request) {
        return aiJobBattleService.parseJd(
                request.getJdText(),
                request.getTargetRole(),
                request.getTargetLevel(),
                request.getCity()
        );
    }

    @Override
    public JobBattleResumeMatchResult matchResume(JobBattleResumeMatchRequest request) {
        return aiJobBattleService.matchResume(
                request.getParsedJdJson(),
                request.getResumeText(),
                request.getProjectHighlights(),
                request.getTargetCompanyType()
        );
    }

    @Override
    public JobBattlePlanResult generatePlan(JobBattleGeneratePlanRequest request) {
        return aiJobBattleService.generatePlan(
                request.getGapsJson(),
                request.getTargetDays(),
                request.getWeeklyHours(),
                request.getPreferredLearningMode(),
                request.getNextInterviewDate()
        );
    }

    @Override
    public JobBattleInterviewReviewResult reviewInterview(JobBattleInterviewReviewRequest request) {
        return aiJobBattleService.reviewInterview(
                request.getInterviewNotes(),
                request.getQaTranscriptJson(),
                request.getInterviewResult(),
                request.getTargetRole(),
                request.getNextInterviewDate()
        );
    }
}

