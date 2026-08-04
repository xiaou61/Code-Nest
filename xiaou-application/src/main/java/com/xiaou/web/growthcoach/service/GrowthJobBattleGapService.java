package com.xiaou.web.growthcoach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.mockinterview.domain.JobBattleMatchRecord;
import com.xiaou.mockinterview.domain.JobBattlePlanRecord;
import com.xiaou.mockinterview.dto.response.JobBattleMatchEngineResult;
import com.xiaou.mockinterview.mapper.JobBattleMatchRecordMapper;
import com.xiaou.mockinterview.mapper.JobBattlePlanRecordMapper;
import com.xiaou.web.growthcoach.dto.GrowthJobBattleGapResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 将求职作战台中已持久化的匹配结果和补短板计划投影为一个可执行的只读摘要。
 *
 * 不把临时 JD 解析、简历正文或模型原始输出写入成长教练，也不据此自动修改用户计划。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthJobBattleGapService {

    private static final int MAX_GAPS = 3;
    private static final String MATCH_ENGINE_ROUTE = "/job-match-engine";
    private static final String RESUME_MATCH_ROUTE = "/job-battle?step=1";
    private static final String PLAN_ROUTE = "/job-battle?step=2";

    private final JobBattleMatchRecordMapper matchRecordMapper;
    private final JobBattlePlanRecordMapper planRecordMapper;
    private final ObjectMapper objectMapper;

    public GrowthJobBattleGapResponse getCurrentGap(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        JobBattleMatchRecord matchRecord = matchRecordMapper.selectLatestByUserId(userId);
        JobBattlePlanRecord planRecord = planRecordMapper.selectLatestByUserId(userId);
        if (matchRecord == null && planRecord == null) {
            return null;
        }

        JobBattleMatchEngineResult matchResult = readMatchResult(matchRecord);
        JobBattlePlanResult planResult = readPlanResult(planRecord);
        JobBattleMatchEngineResult.TargetScore bestTarget = selectBestTarget(matchResult);
        List<JobBattleResumeMatchResult.Gap> gaps = selectGaps(bestTarget, planRecord);

        GrowthJobBattleGapResponse response = new GrowthJobBattleGapResponse();
        response.setMatchRecordId(matchRecord == null ? null : matchRecord.getId());
        response.setPlanRecordId(planRecord == null ? null : planRecord.getId());
        response.setTargetRole(firstText(
                bestTarget == null ? null : bestTarget.getTargetRole(),
                matchRecord == null ? null : matchRecord.getBestTargetRole()
        ));
        response.setMatchScore(firstNumber(
                bestTarget == null ? null : bestTarget.getEngineScore(),
                bestTarget == null ? null : bestTarget.getOverallScore(),
                matchRecord == null ? null : matchRecord.getBestScore()
        ));
        response.setEstimatedPassRate(bestTarget == null ? null : bestTarget.getEstimatedPassRate());
        response.setP0GapCount(firstNumber(
                bestTarget == null ? null : bestTarget.getP0GapCount(),
                countPriority(gaps, "P0")
        ));
        response.setFallback(bestTarget != null && bestTarget.getFallback() != null
                ? bestTarget.getFallback()
                : matchRecord != null && matchRecord.getFallbackCount() != null
                        ? matchRecord.getFallbackCount() > 0
                        : null);
        response.setAnalyzedAt(matchRecord == null ? null : matchRecord.getCreateTime());
        response.setGaps(toGapItems(gaps));
        response.setNextAction(buildNextAction(matchRecord, matchResult, planRecord, planResult, gaps));
        response.setSourceRefs(buildSourceReferences(matchRecord, planRecord));
        return response;
    }

    private JobBattleMatchEngineResult readMatchResult(JobBattleMatchRecord record) {
        if (record == null || !StringUtils.hasText(record.getResultJson())) {
            return null;
        }
        try {
            return objectMapper.readValue(record.getResultJson(), JobBattleMatchEngineResult.class);
        } catch (JsonProcessingException exception) {
            log.warn("解析岗位匹配成长摘要失败，recordId={}", record.getId());
            return null;
        }
    }

    private JobBattlePlanResult readPlanResult(JobBattlePlanRecord record) {
        if (record == null || !StringUtils.hasText(record.getPlanResultJson())) {
            return null;
        }
        try {
            return objectMapper.readValue(record.getPlanResultJson(), JobBattlePlanResult.class);
        } catch (JsonProcessingException exception) {
            log.warn("解析补短板计划成长摘要失败，recordId={}", record.getId());
            return null;
        }
    }

    private JobBattleMatchEngineResult.TargetScore selectBestTarget(JobBattleMatchEngineResult result) {
        if (result == null || result.getRanking() == null) {
            return null;
        }
        return result.getRanking().stream()
                .filter(Objects::nonNull)
                .min(Comparator
                        .comparing(JobBattleMatchEngineResult.TargetScore::getRank,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(JobBattleMatchEngineResult.TargetScore::getEngineScore,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .orElse(null);
    }

    private List<JobBattleResumeMatchResult.Gap> selectGaps(
            JobBattleMatchEngineResult.TargetScore target,
            JobBattlePlanRecord planRecord
    ) {
        if (target != null && target.getTopGaps() != null && !target.getTopGaps().isEmpty()) {
            return sanitizeGaps(target.getTopGaps());
        }
        if (planRecord == null || !StringUtils.hasText(planRecord.getGapsJson())) {
            return List.of();
        }
        try {
            return sanitizeGaps(objectMapper.readValue(planRecord.getGapsJson(), new TypeReference<>() {
            }));
        } catch (JsonProcessingException exception) {
            log.warn("解析补短板计划差距项失败，recordId={}", planRecord.getId());
            return List.of();
        }
    }

    private List<JobBattleResumeMatchResult.Gap> sanitizeGaps(List<JobBattleResumeMatchResult.Gap> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .filter(gap -> StringUtils.hasText(gap.getSkill())
                        || StringUtils.hasText(gap.getSuggestedAction())
                        || StringUtils.hasText(gap.getWhy()))
                .sorted(Comparator.comparingInt(this::priorityWeight))
                .limit(MAX_GAPS)
                .toList();
    }

    private List<GrowthJobBattleGapResponse.GapItem> toGapItems(List<JobBattleResumeMatchResult.Gap> gaps) {
        List<GrowthJobBattleGapResponse.GapItem> items = new ArrayList<>();
        for (JobBattleResumeMatchResult.Gap gap : gaps) {
            GrowthJobBattleGapResponse.GapItem item = new GrowthJobBattleGapResponse.GapItem();
            item.setSkill(gap.getSkill());
            item.setPriority(gap.getPriority());
            item.setWhy(gap.getWhy());
            item.setSuggestedAction(gap.getSuggestedAction());
            items.add(item);
        }
        return items;
    }

    private GrowthJobBattleGapResponse.NextAction buildNextAction(
            JobBattleMatchRecord matchRecord,
            JobBattleMatchEngineResult matchResult,
            JobBattlePlanRecord planRecord,
            JobBattlePlanResult planResult,
            List<JobBattleResumeMatchResult.Gap> gaps
    ) {
        if (isPlanCurrent(planRecord, matchRecord)) {
            JobBattlePlanResult.DailyTask task = firstPlanTask(planResult);
            if (task != null) {
                GrowthJobBattleGapResponse.NextAction action = new GrowthJobBattleGapResponse.NextAction();
                action.setTitle(task.getTask());
                action.setDescription(planTaskDescription(planRecord, task));
                action.setExpectedMinutes(task.getDurationMinutes());
                action.setDeliverable(task.getDeliverable());
                action.setRoutePath(PLAN_ROUTE);
                action.setSourceLabel("补短板计划：" + displayPlanName(planRecord));
                action.setSourceObservedAt(planRecord.getCreateTime());
                return action;
            }
        }

        JobBattleResumeMatchResult.Gap firstGap = gaps.isEmpty() ? null : gaps.get(0);
        if (firstGap != null) {
            GrowthJobBattleGapResponse.NextAction action = new GrowthJobBattleGapResponse.NextAction();
            String skill = firstText(firstGap.getSkill(), "当前关键差距");
            action.setTitle("优先补齐：" + skill);
            action.setDescription(firstText(firstGap.getSuggestedAction(), firstGap.getWhy(), "在求职作战台补充对应能力证据"));
            action.setRoutePath(RESUME_MATCH_ROUTE);
            action.setSourceLabel("岗位匹配分析");
            action.setSourceObservedAt(matchRecord == null ? null : matchRecord.getCreateTime());
            return action;
        }

        String recordedAction = firstRecordedAction(matchResult);
        if (StringUtils.hasText(recordedAction)) {
            GrowthJobBattleGapResponse.NextAction action = new GrowthJobBattleGapResponse.NextAction();
            action.setTitle(recordedAction);
            action.setDescription("来自最近一次岗位匹配引擎的已保存建议");
            action.setRoutePath(MATCH_ENGINE_ROUTE);
            action.setSourceLabel("岗位匹配分析");
            action.setSourceObservedAt(matchRecord == null ? null : matchRecord.getCreateTime());
            return action;
        }
        return null;
    }

    private boolean isPlanCurrent(JobBattlePlanRecord planRecord, JobBattleMatchRecord matchRecord) {
        if (planRecord == null) {
            return false;
        }
        if (matchRecord == null || matchRecord.getCreateTime() == null || planRecord.getCreateTime() == null) {
            return true;
        }
        return !planRecord.getCreateTime().isBefore(matchRecord.getCreateTime());
    }

    private JobBattlePlanResult.DailyTask firstPlanTask(JobBattlePlanResult planResult) {
        if (planResult == null || planResult.getDailyTasks() == null) {
            return null;
        }
        return planResult.getDailyTasks().stream()
                .filter(Objects::nonNull)
                .filter(task -> StringUtils.hasText(task.getTask()))
                .min(Comparator.comparing(JobBattlePlanResult.DailyTask::getDay,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private String planTaskDescription(JobBattlePlanRecord planRecord, JobBattlePlanResult.DailyTask task) {
        String prefix = "来自" + displayPlanName(planRecord)
                + (task.getDay() == null ? "的任务" : "第 " + task.getDay() + " 天任务");
        if (StringUtils.hasText(task.getDeliverable())) {
            return prefix + "；完成产出：" + task.getDeliverable();
        }
        return prefix;
    }

    private String firstRecordedAction(JobBattleMatchEngineResult result) {
        if (result == null || result.getNextActions() == null) {
            return null;
        }
        return result.getNextActions().stream().filter(StringUtils::hasText).findFirst().orElse(null);
    }

    private List<GrowthJobBattleGapResponse.SourceReference> buildSourceReferences(
            JobBattleMatchRecord matchRecord,
            JobBattlePlanRecord planRecord
    ) {
        List<GrowthJobBattleGapResponse.SourceReference> sources = new ArrayList<>();
        if (matchRecord != null) {
            GrowthJobBattleGapResponse.SourceReference source = new GrowthJobBattleGapResponse.SourceReference();
            source.setSourceType("JOB_BATTLE_MATCH");
            source.setSourceId(matchRecord.getId());
            source.setLabel("岗位匹配分析：" + firstText(matchRecord.getAnalysisName(), matchRecord.getBestTargetRole(), "未命名分析"));
            source.setObservedAt(matchRecord.getCreateTime());
            source.setRoutePath(MATCH_ENGINE_ROUTE);
            sources.add(source);
        }
        if (planRecord != null) {
            GrowthJobBattleGapResponse.SourceReference source = new GrowthJobBattleGapResponse.SourceReference();
            source.setSourceType("JOB_BATTLE_PLAN");
            source.setSourceId(planRecord.getId());
            source.setLabel("补短板计划：" + displayPlanName(planRecord));
            source.setObservedAt(planRecord.getCreateTime());
            source.setRoutePath(PLAN_ROUTE);
            sources.add(source);
        }
        return sources;
    }

    private int countPriority(List<JobBattleResumeMatchResult.Gap> gaps, String priority) {
        return (int) gaps.stream()
                .map(JobBattleResumeMatchResult.Gap::getPriority)
                .filter(value -> priority.equalsIgnoreCase(value))
                .count();
    }

    private int priorityWeight(JobBattleResumeMatchResult.Gap gap) {
        String priority = gap == null || gap.getPriority() == null
                ? ""
                : gap.getPriority().trim().toUpperCase(Locale.ROOT);
        return switch (priority) {
            case "P0" -> 0;
            case "P1" -> 1;
            case "P2" -> 2;
            default -> 9;
        };
    }

    private String displayPlanName(JobBattlePlanRecord record) {
        return firstText(record == null ? null : record.getPlanName(), "补短板计划");
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Integer firstNumber(Integer... values) {
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
