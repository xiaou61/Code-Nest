package com.xiaou.mockinterview.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.service.AiJobBattleService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.mockinterview.domain.JobBattlePlanRecord;
import com.xiaou.mockinterview.dto.request.JobBattleGeneratePlanRequest;
import com.xiaou.mockinterview.dto.request.JobBattleInterviewReviewRequest;
import com.xiaou.mockinterview.dto.request.JobBattlePlanHistoryRequest;
import com.xiaou.mockinterview.dto.request.JobBattleParseJdRequest;
import com.xiaou.mockinterview.dto.request.JobBattleResumeMatchRequest;
import com.xiaou.mockinterview.mapper.JobBattlePlanRecordMapper;
import com.xiaou.mockinterview.service.JobBattleService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 求职作战台服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobBattleServiceImpl implements JobBattleService {

    private final AiJobBattleService aiJobBattleService;
    private final JobBattlePlanRecordMapper planRecordMapper;

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
    public JobBattlePlanResult generatePlan(Long userId, JobBattleGeneratePlanRequest request) {
        JobBattlePlanResult result = aiJobBattleService.generatePlan(
                request.getGapsJson(),
                request.getTargetDays(),
                request.getWeeklyHours(),
                request.getPreferredLearningMode(),
                request.getNextInterviewDate()
        );
        savePlanRecord(userId, request, result);
        return result;
    }

    @Override
    public PageResult<JobBattlePlanRecord> getPlanHistory(Long userId, JobBattlePlanHistoryRequest request) {
        JobBattlePlanHistoryRequest query = request == null ? new JobBattlePlanHistoryRequest() : request;
        return PageHelper.doPage(query.getPageNum(), query.getPageSize(),
                () -> planRecordMapper.selectPageByUserId(userId, query));
    }

    @Override
    public JobBattlePlanRecord getPlanHistoryDetail(Long userId, Long recordId) {
        JobBattlePlanRecord record = planRecordMapper.selectByIdAndUserId(recordId, userId);
        if (record == null) {
            throw new BusinessException("计划记录不存在");
        }
        return record;
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

    private void savePlanRecord(Long userId, JobBattleGeneratePlanRequest request, JobBattlePlanResult result) {
        try {
            String planName = result == null ? "求职作战计划" : StrUtil.blankToDefault(result.getPlanName(), "求职作战计划");
            Integer totalDays = result == null ? request.getTargetDays() : result.getTotalDays();
            Boolean fallback = result != null && result.isFallback();

            JobBattlePlanRecord record = new JobBattlePlanRecord()
                    .setUserId(userId)
                    .setPlanName(planName)
                    .setTotalDays(totalDays)
                    .setTargetDays(request.getTargetDays())
                    .setWeeklyHours(request.getWeeklyHours())
                    .setPreferredLearningMode(request.getPreferredLearningMode())
                    .setNextInterviewDate(request.getNextInterviewDate())
                    .setGapsJson(request.getGapsJson())
                    .setPlanResultJson(result == null ? "{}" : JSONUtil.toJsonStr(result))
                    .setFallback(fallback);
            planRecordMapper.insert(record);
        } catch (Exception e) {
            // 落库失败不影响主流程，避免阻塞AI返回
            log.error("保存求职作战台计划记录失败，userId={}", userId, e);
        }
    }
}
