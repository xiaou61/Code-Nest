package com.xiaou.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.service.AiJobBattleService;
import com.xiaou.ai.util.CozeResponseParser;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.enums.CozeWorkflowEnum;
import com.xiaou.common.utils.CozeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 求职作战台AI服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobBattleServiceImpl implements AiJobBattleService {

    private final CozeUtils cozeUtils;

    @Override
    public JobBattleJdParseResult parseJd(String jdText, String targetRole, String targetLevel, String city) {
        try {
            if (!cozeUtils.isClientAvailable()) {
                log.warn("Coze客户端不可用，使用降级JD解析结果");
                return JobBattleJdParseResult.fallbackResult(jdText);
            }

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("jdText", jdText);
            putIfNotBlank(params, "targetRole", targetRole);
            putIfNotBlank(params, "targetLevel", targetLevel);
            putIfNotBlank(params, "city", city);

            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.JOB_BATTLE_JD_PARSE, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                log.warn("JD解析工作流调用失败: {}", result.getMessage());
                return JobBattleJdParseResult.fallbackResult(jdText);
            }

            JSONObject json = CozeResponseParser.parse(result.getData());
            if (json == null) {
                return JobBattleJdParseResult.fallbackResult(jdText);
            }

            return parseJdResult(json);
        } catch (Exception e) {
            log.error("JD解析失败", e);
            return JobBattleJdParseResult.fallbackResult(jdText);
        }
    }

    @Override
    public JobBattleResumeMatchResult matchResume(String parsedJdJson, String resumeText,
                                                  String projectHighlights, String targetCompanyType) {
        try {
            if (!cozeUtils.isClientAvailable()) {
                log.warn("Coze客户端不可用，使用降级简历匹配结果");
                return JobBattleResumeMatchResult.fallbackResult(parsedJdJson, resumeText);
            }

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("parsedJdJson", parsedJdJson);
            params.put("resumeText", resumeText);
            putIfNotBlank(params, "projectHighlights", projectHighlights);
            putIfNotBlank(params, "targetCompanyType", targetCompanyType);

            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.JOB_BATTLE_RESUME_MATCH, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                log.warn("简历匹配工作流调用失败: {}", result.getMessage());
                return JobBattleResumeMatchResult.fallbackResult(parsedJdJson, resumeText);
            }

            JSONObject json = CozeResponseParser.parse(result.getData());
            if (json == null) {
                return JobBattleResumeMatchResult.fallbackResult(parsedJdJson, resumeText);
            }

            return parseResumeMatchResult(json);
        } catch (Exception e) {
            log.error("简历匹配评估失败", e);
            return JobBattleResumeMatchResult.fallbackResult(parsedJdJson, resumeText);
        }
    }

    @Override
    public JobBattlePlanResult generatePlan(String gapsJson, Integer targetDays, Integer weeklyHours,
                                            String preferredLearningMode, String nextInterviewDate) {
        try {
            if (!cozeUtils.isClientAvailable()) {
                log.warn("Coze客户端不可用，使用降级行动计划结果");
                return JobBattlePlanResult.fallbackResult(targetDays, weeklyHours);
            }

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("gapsJson", gapsJson);
            params.put("targetDays", targetDays == null || targetDays <= 0 ? 30 : targetDays);
            params.put("weeklyHours", weeklyHours == null || weeklyHours <= 0 ? 6 : weeklyHours);
            putIfNotBlank(params, "preferredLearningMode", preferredLearningMode);
            putIfNotBlank(params, "nextInterviewDate", nextInterviewDate);

            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.JOB_BATTLE_PLAN_GENERATE, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                log.warn("行动计划工作流调用失败: {}", result.getMessage());
                return JobBattlePlanResult.fallbackResult(targetDays, weeklyHours);
            }

            JSONObject json = CozeResponseParser.parse(result.getData());
            if (json == null) {
                return JobBattlePlanResult.fallbackResult(targetDays, weeklyHours);
            }

            return parsePlanResult(json);
        } catch (Exception e) {
            log.error("生成行动计划失败", e);
            return JobBattlePlanResult.fallbackResult(targetDays, weeklyHours);
        }
    }

    @Override
    public JobBattleInterviewReviewResult reviewInterview(String interviewNotes, String qaTranscriptJson,
                                                          String interviewResult, String targetRole,
                                                          String nextInterviewDate) {
        try {
            if (!cozeUtils.isClientAvailable()) {
                log.warn("Coze客户端不可用，使用降级复盘结果");
                return JobBattleInterviewReviewResult.fallbackResult(interviewResult);
            }

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("interviewNotes", interviewNotes);
            params.put("interviewResult", interviewResult);
            params.put("targetRole", targetRole);
            putIfNotBlank(params, "qaTranscriptJson", qaTranscriptJson);
            putIfNotBlank(params, "nextInterviewDate", nextInterviewDate);

            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.JOB_BATTLE_INTERVIEW_REVIEW, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                log.warn("面试复盘工作流调用失败: {}", result.getMessage());
                return JobBattleInterviewReviewResult.fallbackResult(interviewResult);
            }

            JSONObject json = CozeResponseParser.parse(result.getData());
            if (json == null) {
                return JobBattleInterviewReviewResult.fallbackResult(interviewResult);
            }

            return parseInterviewReviewResult(json);
        } catch (Exception e) {
            log.error("面试复盘总结失败", e);
            return JobBattleInterviewReviewResult.fallbackResult(interviewResult);
        }
    }

    private JobBattleJdParseResult parseJdResult(JSONObject json) {
        return new JobBattleJdParseResult()
                .setJobTitle(CozeResponseParser.getString(json, "jobTitle", "未识别岗位"))
                .setLevel(CozeResponseParser.getString(json, "level", "未知"))
                .setMustSkills(parseStringList(json.get("mustSkills")))
                .setNiceSkills(parseStringList(json.get("niceSkills")))
                .setResponsibilities(parseStringList(json.get("responsibilities")))
                .setSeniorityYears(CozeResponseParser.getString(json, "seniorityYears", "未识别"))
                .setKeywords(parseStringList(json.get("keywords")))
                .setRiskPoints(parseStringList(json.get("riskPoints")))
                .setSummary(CozeResponseParser.getString(json, "summary", "暂无总结"))
                .setFallback(false);
    }

    private JobBattleResumeMatchResult parseResumeMatchResult(JSONObject json) {
        JobBattleResumeMatchResult result = new JobBattleResumeMatchResult();
        int overallScore = CozeResponseParser.getInt(json, "overallScore", 60);
        result.setOverallScore(overallScore);
        result.setDimensionScores(parseDimensionScores(json.getJSONObject("dimensionScores"), overallScore));
        result.setStrengths(parseStringList(json.get("strengths")));
        result.setMissingKeywords(parseStringList(json.get("missingKeywords")));
        result.setEstimatedPassRate(CozeResponseParser.getInt(json, "estimatedPassRate", Math.max(20, overallScore - 10)));
        result.setGaps(parseGapList(json.getJSONArray("gaps")));
        result.setResumeRewriteSuggestions(parseRewriteSuggestions(json.getJSONArray("resumeRewriteSuggestions")));
        result.setFallback(false);
        return result;
    }

    private JobBattlePlanResult parsePlanResult(JSONObject json) {
        JobBattlePlanResult result = new JobBattlePlanResult();
        int totalDays = CozeResponseParser.getInt(json, "totalDays", 30);
        result.setPlanName(CozeResponseParser.getString(json, "planName", "求职冲刺计划"));
        result.setTotalDays(totalDays);
        result.setWeeklyGoals(parseStringList(json.get("weeklyGoals")));
        result.setRiskAndFallback(parseStringList(json.get("riskAndFallback")));
        result.setDailyTasks(parseDailyTasks(json.getJSONArray("dailyTasks")));
        result.setMilestones(parseMilestones(json.getJSONArray("milestones")));
        result.setFallback(false);
        return result;
    }

    private JobBattleInterviewReviewResult parseInterviewReviewResult(JSONObject json) {
        JobBattleInterviewReviewResult result = new JobBattleInterviewReviewResult();
        result.setOverallConclusion(CozeResponseParser.getString(json, "overallConclusion", "暂无复盘结论"));
        result.setRootCauses(parseStringList(json.get("rootCauses")));
        result.setHighImpactFixes(parseHighImpactFixes(json.getJSONArray("highImpactFixes")));
        result.setQuestionTypeWeakness(parseQuestionTypeWeakness(json.getJSONArray("questionTypeWeakness")));
        result.setNext7DayPlan(parseStringList(json.get("next7DayPlan")));
        result.setConfidenceScore(CozeResponseParser.getInt(json, "confidenceScore", 60));
        result.setFallback(false);
        return result;
    }

    private Map<String, Integer> parseDimensionScores(JSONObject dimensionJson, int overallScore) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        if (dimensionJson == null) {
            scores.put("skillMatch", overallScore);
            scores.put("projectDepth", Math.max(40, overallScore - 8));
            scores.put("architectureAbility", Math.max(35, overallScore - 12));
            scores.put("communicationClarity", Math.max(45, overallScore + 5));
            return scores;
        }
        scores.put("skillMatch", dimensionJson.getInt("skillMatch", overallScore));
        scores.put("projectDepth", dimensionJson.getInt("projectDepth", Math.max(40, overallScore - 8)));
        scores.put("architectureAbility", dimensionJson.getInt("architectureAbility", Math.max(35, overallScore - 12)));
        scores.put("communicationClarity", dimensionJson.getInt("communicationClarity", Math.max(45, overallScore + 5)));
        return scores;
    }

    private List<JobBattleResumeMatchResult.Gap> parseGapList(JSONArray array) {
        List<JobBattleResumeMatchResult.Gap> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                continue;
            }
            list.add(new JobBattleResumeMatchResult.Gap()
                    .setSkill(item.getStr("skill"))
                    .setPriority(item.getStr("priority"))
                    .setWhy(item.getStr("why"))
                    .setSuggestedAction(item.getStr("suggestedAction")));
        }
        return list;
    }

    private List<JobBattleResumeMatchResult.ResumeRewriteSuggestion> parseRewriteSuggestions(JSONArray array) {
        List<JobBattleResumeMatchResult.ResumeRewriteSuggestion> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                continue;
            }
            list.add(new JobBattleResumeMatchResult.ResumeRewriteSuggestion()
                    .setSection(item.getStr("section"))
                    .setAdvice(item.getStr("advice")));
        }
        return list;
    }

    private List<JobBattlePlanResult.DailyTask> parseDailyTasks(JSONArray array) {
        List<JobBattlePlanResult.DailyTask> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                continue;
            }
            list.add(new JobBattlePlanResult.DailyTask()
                    .setDay(item.getInt("day"))
                    .setTask(item.getStr("task"))
                    .setDurationMinutes(item.getInt("durationMinutes"))
                    .setTaskType(item.getStr("taskType"))
                    .setDeliverable(item.getStr("deliverable")));
        }
        return list;
    }

    private List<JobBattlePlanResult.Milestone> parseMilestones(JSONArray array) {
        List<JobBattlePlanResult.Milestone> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                continue;
            }
            list.add(new JobBattlePlanResult.Milestone()
                    .setDay(item.getInt("day"))
                    .setGoal(item.getStr("goal")));
        }
        return list;
    }

    private List<JobBattleInterviewReviewResult.HighImpactFix> parseHighImpactFixes(JSONArray array) {
        List<JobBattleInterviewReviewResult.HighImpactFix> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                continue;
            }
            list.add(new JobBattleInterviewReviewResult.HighImpactFix()
                    .setIssue(item.getStr("issue"))
                    .setAction(item.getStr("action"))
                    .setDeadline(item.getStr("deadline"))
                    .setMetric(item.getStr("metric")));
        }
        return list;
    }

    private List<JobBattleInterviewReviewResult.QuestionTypeWeakness> parseQuestionTypeWeakness(JSONArray array) {
        List<JobBattleInterviewReviewResult.QuestionTypeWeakness> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                continue;
            }
            list.add(new JobBattleInterviewReviewResult.QuestionTypeWeakness()
                    .setType(item.getStr("type"))
                    .setSuggestion(item.getStr("suggestion")));
        }
        return list;
    }

    private List<String> parseStringList(Object source) {
        List<String> result = new ArrayList<>();
        if (source == null) {
            return result;
        }

        if (source instanceof JSONArray jsonArray) {
            for (int i = 0; i < jsonArray.size(); i++) {
                String value = jsonArray.getStr(i);
                if (StrUtil.isNotBlank(value)) {
                    result.add(value.trim());
                }
            }
            return result;
        }

        if (source instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && StrUtil.isNotBlank(item.toString())) {
                    result.add(item.toString().trim());
                }
            }
            return result;
        }

        String text = source.toString();
        if (StrUtil.isBlank(text)) {
            return result;
        }
        String[] parts = text.split("[,，;；\\n]");
        for (String part : parts) {
            if (StrUtil.isNotBlank(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private void putIfNotBlank(Map<String, Object> params, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            params.put(key, value);
        }
    }
}

