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
import com.xiaou.mockinterview.domain.JobBattleMatchRecord;
import com.xiaou.mockinterview.domain.JobBattlePlanRecord;
import com.xiaou.mockinterview.dto.request.CareerLoopEventRequest;
import com.xiaou.mockinterview.dto.request.JobBattleGeneratePlanRequest;
import com.xiaou.mockinterview.dto.request.JobBattleInterviewReviewRequest;
import com.xiaou.mockinterview.dto.request.JobBattleMatchEngineHistoryRequest;
import com.xiaou.mockinterview.dto.request.JobBattleMatchEngineRunRequest;
import com.xiaou.mockinterview.dto.request.JobBattlePlanHistoryRequest;
import com.xiaou.mockinterview.dto.request.JobBattleParseJdRequest;
import com.xiaou.mockinterview.dto.request.JobBattleResumeMatchRequest;
import com.xiaou.mockinterview.dto.response.JobBattleMatchEngineResult;
import com.xiaou.mockinterview.enums.CareerLoopStageEnum;
import com.xiaou.mockinterview.mapper.JobBattleMatchRecordMapper;
import com.xiaou.mockinterview.mapper.JobBattlePlanRecordMapper;
import com.xiaou.mockinterview.service.CareerLoopService;
import com.xiaou.mockinterview.service.JobBattleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 求职作战台服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobBattleServiceImpl implements JobBattleService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AiJobBattleService aiJobBattleService;
    private final JobBattlePlanRecordMapper planRecordMapper;
    private final JobBattleMatchRecordMapper matchRecordMapper;
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
    public JobBattleMatchEngineResult runMatchEngine(Long userId, JobBattleMatchEngineRunRequest request) {
        if (request == null || request.getTargets() == null || request.getTargets().isEmpty()) {
            throw new BusinessException("请至少添加一个目标岗位");
        }
        if (request.getTargets().size() > 10) {
            throw new BusinessException("单次最多分析10个岗位");
        }

        List<JobBattleMatchEngineResult.TargetScore> ranking = new ArrayList<>();
        for (JobBattleMatchEngineRunRequest.TargetJob target : request.getTargets()) {
            JobBattleJdParseResult jdResult = aiJobBattleService.parseJd(
                    target.getJdText(),
                    target.getTargetRole(),
                    target.getTargetLevel(),
                    target.getCity()
            );
            String parsedJdJson = jdResult == null ? "{}" : JSONUtil.toJsonStr(jdResult);
            JobBattleResumeMatchResult matchResult = aiJobBattleService.matchResume(
                    parsedJdJson,
                    request.getResumeText(),
                    request.getProjectHighlights(),
                    request.getTargetCompanyType()
            );
            ranking.add(buildTargetScore(target, jdResult, matchResult));
        }

        if (ranking.isEmpty()) {
            throw new BusinessException("岗位匹配分析失败");
        }

        ranking.sort(
                Comparator.comparing(JobBattleMatchEngineResult.TargetScore::getEngineScore, Comparator.nullsLast(Integer::compareTo))
                        .reversed()
                        .thenComparing(JobBattleMatchEngineResult.TargetScore::getEstimatedPassRate, Comparator.nullsLast(Integer::compareTo))
                        .reversed()
        );
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setRank(i + 1);
        }

        JobBattleMatchEngineResult.TargetScore best = ranking.get(0);
        int targetCount = ranking.size();
        int bestScore = safeScore(best.getEngineScore(), 0);
        int averageScore = (int) Math.round(ranking.stream()
                .map(JobBattleMatchEngineResult.TargetScore::getEngineScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0D));
        int fallbackCount = (int) ranking.stream()
                .filter(item -> Boolean.TRUE.equals(item.getFallback()))
                .count();
        String bestTargetRole = StrUtil.blankToDefault(best.getTargetRole(), "未命名岗位");
        String analysisName = bestTargetRole + "岗位匹配分析（" + targetCount + "岗）";

        JobBattleMatchEngineResult result = new JobBattleMatchEngineResult()
                .setAnalysisName(analysisName)
                .setTargetCount(targetCount)
                .setBestScore(bestScore)
                .setAverageScore(averageScore)
                .setFallbackCount(fallbackCount)
                .setBestTargetRole(bestTargetRole)
                .setGeneratedAt(LocalDateTime.now().format(DATETIME_FORMATTER))
                .setRanking(ranking)
                .setNextActions(buildNextActions(ranking, fallbackCount));

        Long recordId = saveMatchRecord(userId, request, result);
        result.setAnalysisId(recordId);
        pushLoopEvent(userId, CareerLoopStageEnum.RESUME_MATCHED.name(),
                "job_battle_match_engine",
                recordId == null ? null : String.valueOf(recordId),
                "完成岗位批量匹配分析");
        return result;
    }

    @Override
    public PageResult<JobBattleMatchRecord> getMatchEngineHistory(Long userId, JobBattleMatchEngineHistoryRequest request) {
        JobBattleMatchEngineHistoryRequest query = request == null ? new JobBattleMatchEngineHistoryRequest() : request;
        return PageHelper.doPage(query.getPageNum(), query.getPageSize(),
                () -> matchRecordMapper.selectPageByUserId(userId, query));
    }

    @Override
    public JobBattleMatchEngineResult getMatchEngineDetail(Long userId, Long recordId) {
        JobBattleMatchRecord record = matchRecordMapper.selectByIdAndUserId(recordId, userId);
        if (record == null) {
            throw new BusinessException("匹配记录不存在");
        }
        return restoreResultFromRecord(record);
    }

    @Override
    public JobBattleMatchEngineResult getLatestMatchEngineResult(Long userId) {
        JobBattleMatchRecord record = matchRecordMapper.selectLatestByUserId(userId);
        if (record == null) {
            return null;
        }
        return restoreResultFromRecord(record);
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

    private JobBattleMatchEngineResult.TargetScore buildTargetScore(
            JobBattleMatchEngineRunRequest.TargetJob target,
            JobBattleJdParseResult jdResult,
            JobBattleResumeMatchResult matchResult) {
        Map<String, Integer> dimension = matchResult == null || matchResult.getDimensionScores() == null
                ? new LinkedHashMap<>()
                : matchResult.getDimensionScores();

        int overall = safeScore(matchResult == null ? null : matchResult.getOverallScore(), 0);
        int skillMatch = safeScore(dimension.get("skillMatch"), overall);
        int projectDepth = safeScore(dimension.get("projectDepth"), Math.max(40, overall - 8));
        int architectureAbility = safeScore(dimension.get("architectureAbility"), Math.max(35, overall - 12));
        int communicationClarity = safeScore(dimension.get("communicationClarity"), Math.max(45, overall + 5));
        List<JobBattleResumeMatchResult.Gap> gaps = matchResult == null || matchResult.getGaps() == null
                ? Collections.emptyList()
                : matchResult.getGaps();
        int p0GapCount = countPriorityGaps(gaps, "P0");
        int p1GapCount = countPriorityGaps(gaps, "P1");
        int missingCount = matchResult == null || matchResult.getMissingKeywords() == null
                ? 0 : matchResult.getMissingKeywords().size();
        int engineScore = calculateEngineScore(
                overall, skillMatch, projectDepth, architectureAbility, communicationClarity,
                p0GapCount, p1GapCount, missingCount, gaps.size()
        );

        String role = StrUtil.blankToDefault(target.getTargetRole(),
                jdResult == null ? null : jdResult.getJobTitle());
        role = StrUtil.blankToDefault(role, "未命名岗位");
        String level = StrUtil.blankToDefault(target.getTargetLevel(),
                jdResult == null ? null : jdResult.getLevel());

        return new JobBattleMatchEngineResult.TargetScore()
                .setTargetRole(role)
                .setTargetLevel(level)
                .setCity(target.getCity())
                .setEngineScore(engineScore)
                .setOverallScore(overall)
                .setEstimatedPassRate(safeScore(
                        matchResult == null ? null : matchResult.getEstimatedPassRate(),
                        Math.max(20, overall - 10)))
                .setDimensionScores(dimension)
                .setP0GapCount(p0GapCount)
                .setP1GapCount(p1GapCount)
                .setStrengths(limitTextList(matchResult == null ? null : matchResult.getStrengths(), 3))
                .setMissingKeywords(limitTextList(matchResult == null ? null : matchResult.getMissingKeywords(), 6))
                .setTopGaps(topGaps(gaps, 3))
                .setJdSummary(jdResult == null ? null : jdResult.getSummary())
                .setFallback((jdResult != null && jdResult.isFallback()) || (matchResult != null && matchResult.isFallback()));
    }

    private int calculateEngineScore(int overall, int skill, int project, int architecture, int communication,
                                     int p0GapCount, int p1GapCount, int missingCount, int totalGapCount) {
        double weighted = overall * 0.55D
                + skill * 0.20D
                + project * 0.15D
                + architecture * 0.07D
                + communication * 0.03D;

        int gapPenalty = p0GapCount * 4 + p1GapCount * 2 + Math.max(0, totalGapCount - 5);
        int keywordPenalty = Math.min(8, missingCount);
        return clampScore((int) Math.round(weighted - gapPenalty - keywordPenalty));
    }

    private int countPriorityGaps(List<JobBattleResumeMatchResult.Gap> gaps, String priority) {
        if (gaps == null || gaps.isEmpty()) {
            return 0;
        }
        return (int) gaps.stream()
                .map(JobBattleResumeMatchResult.Gap::getPriority)
                .filter(value -> StrUtil.equalsIgnoreCase(priority, value))
                .count();
    }

    private List<JobBattleResumeMatchResult.Gap> topGaps(List<JobBattleResumeMatchResult.Gap> gaps, int limit) {
        if (gaps == null || gaps.isEmpty()) {
            return Collections.emptyList();
        }
        return gaps.stream()
                .sorted(Comparator.comparingInt(this::gapPriorityWeight))
                .limit(limit)
                .toList();
    }

    private int gapPriorityWeight(JobBattleResumeMatchResult.Gap gap) {
        if (gap == null || StrUtil.isBlank(gap.getPriority())) {
            return 9;
        }
        String priority = gap.getPriority().toUpperCase(Locale.ROOT);
        if ("P0".equals(priority)) {
            return 0;
        }
        if ("P1".equals(priority)) {
            return 1;
        }
        if ("P2".equals(priority)) {
            return 2;
        }
        return 5;
    }

    private List<String> limitTextList(List<String> source, int limit) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> target = new ArrayList<>();
        for (String item : source) {
            if (StrUtil.isBlank(item)) {
                continue;
            }
            target.add(item.trim());
            if (target.size() >= limit) {
                break;
            }
        }
        return target;
    }

    private List<String> buildNextActions(List<JobBattleMatchEngineResult.TargetScore> ranking, int fallbackCount) {
        if (ranking == null || ranking.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> actions = new ArrayList<>();
        JobBattleMatchEngineResult.TargetScore best = ranking.get(0);
        actions.add("优先冲刺岗位【" + StrUtil.blankToDefault(best.getTargetRole(), "未命名岗位")
                + "】，当前综合分 " + safeScore(best.getEngineScore(), 0));

        if (best.getTopGaps() != null && !best.getTopGaps().isEmpty()) {
            JobBattleResumeMatchResult.Gap firstGap = best.getTopGaps().get(0);
            String gapSkill = firstGap == null ? null : firstGap.getSkill();
            String gapAction = firstGap == null ? null : firstGap.getSuggestedAction();
            if (StrUtil.isNotBlank(gapSkill)) {
                actions.add("优先补齐差距项【" + gapSkill + "】");
            }
            if (StrUtil.isNotBlank(gapAction)) {
                actions.add("执行建议动作：" + gapAction);
            }
        }

        if (best.getMissingKeywords() != null && !best.getMissingKeywords().isEmpty()) {
            actions.add("补全简历关键词：" + String.join("、", best.getMissingKeywords().stream().limit(4).toList()));
        }

        if (ranking.size() > 1) {
            JobBattleMatchEngineResult.TargetScore second = ranking.get(1);
            actions.add("次优岗位【" + StrUtil.blankToDefault(second.getTargetRole(), "未命名岗位")
                    + "】可作为并行投递目标");
        }

        if (fallbackCount > 0) {
            actions.add("本次有" + fallbackCount + "个岗位触发降级规则，建议复核对应JD文本质量");
        }

        if (actions.size() > 5) {
            return actions.subList(0, 5);
        }
        return actions;
    }

    private int safeScore(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return clampScore(value);
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private Long saveMatchRecord(Long userId, JobBattleMatchEngineRunRequest request, JobBattleMatchEngineResult result) {
        try {
            JobBattleMatchRecord record = new JobBattleMatchRecord()
                    .setUserId(userId)
                    .setAnalysisName(result.getAnalysisName())
                    .setTargetCount(result.getTargetCount())
                    .setBestScore(result.getBestScore())
                    .setAverageScore(result.getAverageScore())
                    .setFallbackCount(result.getFallbackCount())
                    .setBestTargetRole(result.getBestTargetRole())
                    .setResumeText(request.getResumeText())
                    .setProjectHighlights(request.getProjectHighlights())
                    .setTargetCompanyType(request.getTargetCompanyType())
                    .setResultJson(JSONUtil.toJsonStr(result));
            matchRecordMapper.insert(record);
            return record.getId();
        } catch (Exception e) {
            log.error("保存岗位匹配引擎记录失败，userId={}", userId, e);
            return null;
        }
    }

    private JobBattleMatchEngineResult restoreResultFromRecord(JobBattleMatchRecord record) {
        JobBattleMatchEngineResult result = null;
        if (StrUtil.isNotBlank(record.getResultJson())) {
            try {
                result = JSONUtil.toBean(record.getResultJson(), JobBattleMatchEngineResult.class);
            } catch (Exception e) {
                log.warn("解析岗位匹配结果JSON失败，recordId={}", record.getId(), e);
            }
        }
        if (result == null) {
            result = new JobBattleMatchEngineResult()
                    .setRanking(Collections.emptyList())
                    .setNextActions(Collections.emptyList());
        }
        result.setAnalysisId(record.getId());
        if (StrUtil.isBlank(result.getAnalysisName())) {
            result.setAnalysisName(record.getAnalysisName());
        }
        if (result.getTargetCount() == null) {
            result.setTargetCount(record.getTargetCount());
        }
        if (result.getBestScore() == null) {
            result.setBestScore(record.getBestScore());
        }
        if (result.getAverageScore() == null) {
            result.setAverageScore(record.getAverageScore());
        }
        if (result.getFallbackCount() == null) {
            result.setFallbackCount(record.getFallbackCount());
        }
        if (StrUtil.isBlank(result.getBestTargetRole())) {
            result.setBestTargetRole(record.getBestTargetRole());
        }
        if (StrUtil.isBlank(result.getGeneratedAt())) {
            LocalDateTime createTime = record.getCreateTime();
            result.setGeneratedAt(createTime == null ? null : createTime.format(DATETIME_FORMATTER));
        }
        if (result.getRanking() == null) {
            result.setRanking(Collections.emptyList());
        }
        if (result.getNextActions() == null || result.getNextActions().isEmpty()) {
            result.setNextActions(buildNextActions(result.getRanking(), safeScore(result.getFallbackCount(), 0)));
        }
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

