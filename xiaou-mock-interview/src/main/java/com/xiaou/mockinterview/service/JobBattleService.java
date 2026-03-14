package com.xiaou.mockinterview.service;

import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.mockinterview.domain.JobBattleMatchRecord;
import com.xiaou.mockinterview.domain.JobBattlePlanRecord;
import com.xiaou.mockinterview.dto.request.JobBattleGeneratePlanRequest;
import com.xiaou.mockinterview.dto.request.JobBattleInterviewReviewRequest;
import com.xiaou.mockinterview.dto.request.JobBattleMatchEngineHistoryRequest;
import com.xiaou.mockinterview.dto.request.JobBattleMatchEngineRunRequest;
import com.xiaou.mockinterview.dto.request.JobBattlePlanHistoryRequest;
import com.xiaou.mockinterview.dto.request.JobBattleParseJdRequest;
import com.xiaou.mockinterview.dto.request.JobBattleResumeMatchRequest;
import com.xiaou.mockinterview.dto.response.JobBattleMatchEngineResult;

/**
 * 求职作战台服务
 *
 * @author xiaou
 */
public interface JobBattleService {

    /**
     * JD解析
     */
    JobBattleJdParseResult parseJd(Long userId, JobBattleParseJdRequest request);

    /**
     * 简历匹配
     */
    JobBattleResumeMatchResult matchResume(Long userId, JobBattleResumeMatchRequest request);

    /**
     * 生成计划
     */
    JobBattlePlanResult generatePlan(Long userId, JobBattleGeneratePlanRequest request);

    /**
     * 获取计划历史
     */
    PageResult<JobBattlePlanRecord> getPlanHistory(Long userId, JobBattlePlanHistoryRequest request);

    /**
     * 获取计划历史详情
     */
    JobBattlePlanRecord getPlanHistoryDetail(Long userId, Long recordId);

    /**
     * 运行岗位匹配引擎（批量岗位）
     */
    JobBattleMatchEngineResult runMatchEngine(Long userId, JobBattleMatchEngineRunRequest request);

    /**
     * 获取岗位匹配历史
     */
    PageResult<JobBattleMatchRecord> getMatchEngineHistory(Long userId, JobBattleMatchEngineHistoryRequest request);

    /**
     * 获取岗位匹配历史详情
     */
    JobBattleMatchEngineResult getMatchEngineDetail(Long userId, Long recordId);

    /**
     * 获取最近一次岗位匹配结果
     */
    JobBattleMatchEngineResult getLatestMatchEngineResult(Long userId);

    /**
     * 面试复盘
     */
    JobBattleInterviewReviewResult reviewInterview(Long userId, JobBattleInterviewReviewRequest request);
}
