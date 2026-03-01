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
import com.xiaou.mockinterview.dto.request.CareerLoopEventRequest;
import com.xiaou.mockinterview.enums.CareerLoopStageEnum;
import com.xiaou.mockinterview.mapper.JobBattlePlanRecordMapper;
import com.xiaou.mockinterview.service.CareerLoopService;
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
    private final CareerLoopService careerLoopService;

    @Override
    public JobBattleJdParseResult parseJd(Long userId, JobBattleParseJdRequest request) {
        JobBattleJdParseResult result = aiJobBattleService.parseJd(
                request.getJdText(),
                request.getTargetRole(),
                request.getTargetLevel(),
                request.getCity()
        );
        pushLoopEvent(userId, CareerLoopStageEnum.JD_PARSED.name(), "job_battle", null, "完成JD解析");
        return result;
    }

    @Override
    public JobBattleResumeMatchResult matchResume(Long userId, JobBattleResumeMatchRequest request) {
        JobBattleResumeMatchResult result = aiJobBattleService.matchResume(
                request.getParsedJdJson(),
                request.getResumeText(),
                request.getProjectHighlights(),
                request.getTargetCompanyType()
        );
        pushLoopEvent(userId, CareerLoopStageEnum.RESUME_MATCHED.name(), "job_battle", null, "完成简历匹配");
        return result;
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
        Long recordId = savePlanRecord(userId, request, result);
        pushLoopEvent(userId, CareerLoopStageEnum.PLAN_READY.name(), "job_battle",
                recordId == null ? null : String.valueOf(recordId), "生成行动计划");
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
    public JobBattleInterviewReviewResult reviewInterview(Long userId, JobBattleInterviewReviewRequest request) {
        JobBattleInterviewReviewResult result = aiJobBattleService.reviewInterview(
                request.getInterviewNotes(),
                request.getQaTranscriptJson(),
                request.getInterviewResult(),
                request.getTargetRole(),
                request.getNextInterviewDate()
        );
        pushLoopEvent(userId, CareerLoopStageEnum.REVIEWED.name(), "job_battle", null, "完成复盘总结");
        return result;
    }

    private Long savePlanRecord(Long userId, JobBattleGeneratePlanRequest request, JobBattlePlanResult result) {
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
            return record.getId();
        } catch (Exception e) {
            // 落库失败不影响主流程，避免阻塞AI返回
            log.error("保存求职作战台计划记录失败，userId={}", userId, e);
            return null;
        }
    }

    private void pushLoopEvent(Long userId, String targetStage, String source, String refId, String note) {
        if (userId == null) {
            return;
        }
        try {
            careerLoopService.onEvent(userId, new CareerLoopEventRequest()
                    .setSource(source)
                    .setTargetStage(targetStage)
                    .setRefId(refId)
                    .setNote(note));
        } catch (Exception e) {
            // 闭环中台异常不影响主流程
            log.warn("推送求职闭环事件失败，userId={}, stage={}", userId, targetStage, e);
        }
    }
}
