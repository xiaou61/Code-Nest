package com.xiaou.mockinterview.service;

import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.mockinterview.dto.request.JobBattleGeneratePlanRequest;
import com.xiaou.mockinterview.dto.request.JobBattleInterviewReviewRequest;
import com.xiaou.mockinterview.dto.request.JobBattleParseJdRequest;
import com.xiaou.mockinterview.dto.request.JobBattleResumeMatchRequest;

/**
 * 求职作战台服务
 *
 * @author xiaou
 */
public interface JobBattleService {

    /**
     * JD解析
     */
    JobBattleJdParseResult parseJd(JobBattleParseJdRequest request);

    /**
     * 简历匹配
     */
    JobBattleResumeMatchResult matchResume(JobBattleResumeMatchRequest request);

    /**
     * 生成计划
     */
    JobBattlePlanResult generatePlan(JobBattleGeneratePlanRequest request);

    /**
     * 面试复盘
     */
    JobBattleInterviewReviewResult reviewInterview(JobBattleInterviewReviewRequest request);
}

