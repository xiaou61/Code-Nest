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
import java.util.Locale;

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

            Map<String, Object> params = buildJdParseParams(jdText, targetRole, targetLevel, city);

            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.JOB_BATTLE_JD_PARSE, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                log.warn("JD解析工作流调用失败: {}", result.getMessage());
                return buildLocalJdFallback(jdText, targetRole, targetLevel);
            }

            log.info("JD解析工作流原始响应: {}", abbreviate(result.getData(), 500));
            JSONObject json = CozeResponseParser.parse(result.getData());
            if (json == null) {
                return buildLocalJdFallback(jdText, targetRole, targetLevel);
            }

            return parseJdResult(json, jdText, targetRole, targetLevel);
        } catch (Exception e) {
            log.error("JD解析失败", e);
            return buildLocalJdFallback(jdText, targetRole, targetLevel);
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

    private JobBattleJdParseResult parseJdResult(JSONObject json, String jdText, String targetRole, String targetLevel) {
        JSONObject normalized = normalizeJdJson(json);

        String jobTitle = getStringByAliases(normalized, "未识别岗位",
                "jobTitle", "position", "positionName", "job_title", "岗位", "岗位名称");
        String level = getStringByAliases(normalized, "未知",
                "level", "jobLevel", "seniority", "seniorityLevel", "级别", "职级");
        List<String> mustSkills = getListByAliases(normalized,
                "mustSkills", "requiredSkills", "coreSkills", "required_skills", "must_have_skills", "必备技能", "核心技能");
        List<String> niceSkills = getListByAliases(normalized,
                "niceSkills", "preferredSkills", "plusSkills", "preferred_skills", "加分技能", "优先技能");
        List<String> responsibilities = getListByAliases(normalized,
                "responsibilities", "jobResponsibilities", "duties", "岗位职责", "工作职责");
        String seniorityYears = getStringByAliases(normalized, "未识别",
                "seniorityYears", "experienceYears", "yearsOfExperience", "experience", "经验要求", "工作年限");
        List<String> keywords = getListByAliases(normalized,
                "keywords", "keyWords", "tags", "关键字", "关键词");
        List<String> riskPoints = getListByAliases(normalized,
                "riskPoints", "risks", "ambiguities", "risk_points", "风险点", "模糊点");
        String summary = getStringByAliases(normalized, "暂无总结",
                "summary", "jdSummary", "overview", "description", "摘要", "总结");

        boolean invalid = "未识别岗位".equals(jobTitle)
                && mustSkills.isEmpty()
                && keywords.isEmpty()
                && "暂无总结".equals(summary);
        if (invalid) {
            log.warn("JD解析返回字段与约定不一致，原始结果: {}", json);
            JobBattleJdParseResult fallback = buildLocalJdFallback(jdText, targetRole, targetLevel);
            List<String> risks = new ArrayList<>(fallback.getRiskPoints() == null ? List.of() : fallback.getRiskPoints());
            risks.add("AI返回格式与约定不一致，已使用本地规则兜底");
            fallback.setRiskPoints(risks);
            return fallback;
        }

        return new JobBattleJdParseResult()
                .setJobTitle(jobTitle)
                .setLevel(level)
                .setMustSkills(mustSkills)
                .setNiceSkills(niceSkills)
                .setResponsibilities(responsibilities)
                .setSeniorityYears(seniorityYears)
                .setKeywords(keywords)
                .setRiskPoints(riskPoints)
                .setSummary(summary)
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

    private Map<String, Object> buildJdParseParams(String jdText, String targetRole, String targetLevel, String city) {
        Map<String, Object> params = new LinkedHashMap<>();
        String safeJd = jdText == null ? "" : jdText;
        params.put("jdText", safeJd);
        // 兼容不同工作流参数命名
        params.put("jd", safeJd);
        params.put("input", safeJd);
        params.put("content", safeJd);
        params.put("text", safeJd);

        putIfNotBlank(params, "targetRole", targetRole);
        putIfNotBlank(params, "role", targetRole);
        putIfNotBlank(params, "targetLevel", targetLevel);
        putIfNotBlank(params, "level", targetLevel);
        putIfNotBlank(params, "city", city);
        return params;
    }

    private JobBattleJdParseResult buildLocalJdFallback(String jdText, String targetRole, String targetLevel) {
        String text = jdText == null ? "" : jdText;
        String lower = text.toLowerCase(Locale.ROOT);

        String jobTitle = StrUtil.isNotBlank(targetRole) ? targetRole : "未识别岗位";
        if ("未识别岗位".equals(jobTitle)) {
            if (lower.contains("java")) {
                jobTitle = "Java后端开发";
            } else if (lower.contains("前端") || lower.contains("vue") || lower.contains("react")) {
                jobTitle = "前端开发";
            } else if (lower.contains("python")) {
                jobTitle = "Python开发";
            } else if (lower.contains("go")) {
                jobTitle = "Go开发";
            } else if (lower.contains("算法")) {
                jobTitle = "算法工程师";
            }
        }

        List<String> mustSkills = new ArrayList<>();
        collectSkillIfContains(lower, mustSkills, "java");
        collectSkillIfContains(lower, mustSkills, "spring boot");
        collectSkillIfContains(lower, mustSkills, "mysql");
        collectSkillIfContains(lower, mustSkills, "redis");
        collectSkillIfContains(lower, mustSkills, "mq");
        collectSkillIfContains(lower, mustSkills, "kafka");
        collectSkillIfContains(lower, mustSkills, "es");
        collectSkillIfContains(lower, mustSkills, "vue");
        collectSkillIfContains(lower, mustSkills, "react");
        collectSkillIfContains(lower, mustSkills, "typescript");
        collectSkillIfContains(lower, mustSkills, "docker");
        collectSkillIfContains(lower, mustSkills, "k8s");

        List<String> keywords = new ArrayList<>();
        collectKeywordIfContains(lower, keywords, "高并发");
        collectKeywordIfContains(lower, keywords, "分布式");
        collectKeywordIfContains(lower, keywords, "性能优化");
        collectKeywordIfContains(lower, keywords, "架构");
        collectKeywordIfContains(lower, keywords, "微服务");

        String summary = StrUtil.isNotBlank(text)
                ? "已根据JD文本进行本地规则解析，建议检查Coze工作流输出字段映射"
                : "暂无JD文本，无法解析";

        return new JobBattleJdParseResult()
                .setJobTitle(jobTitle)
                .setLevel(StrUtil.isNotBlank(targetLevel) ? targetLevel : "未知")
                .setMustSkills(mustSkills)
                .setNiceSkills(List.of())
                .setResponsibilities(List.of())
                .setSeniorityYears("未识别")
                .setKeywords(keywords)
                .setRiskPoints(new ArrayList<>(List.of("当前为本地规则兜底结果")))
                .setSummary(summary)
                .setFallback(true);
    }

    private void collectSkillIfContains(String lowerText, List<String> list, String keyword) {
        if (lowerText.contains(keyword) && list.stream().noneMatch(v -> v.equalsIgnoreCase(keyword))) {
            list.add(keyword);
        }
    }

    private void collectKeywordIfContains(String lowerText, List<String> list, String keyword) {
        if (lowerText.contains(keyword) && !list.contains(keyword)) {
            list.add(keyword);
        }
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return null;
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...";
    }

    /**
     * 兼容工作流可能返回 data/result/content 包裹层
     */
    private JSONObject normalizeJdJson(JSONObject json) {
        if (json == null) {
            return null;
        }

        if (json.containsKey("jobTitle") || json.containsKey("mustSkills")
                || json.containsKey("岗位") || json.containsKey("必备技能")) {
            return json;
        }

        Object nested = json.get("data");
        if (nested instanceof JSONObject nestedObj) {
            return nestedObj;
        }

        nested = json.get("result");
        if (nested instanceof JSONObject nestedObj) {
            return nestedObj;
        }

        nested = json.get("content");
        if (nested instanceof JSONObject nestedObj) {
            return nestedObj;
        }
        if (nested instanceof String nestedStr) {
            JSONObject parsed = CozeResponseParser.parse(nestedStr);
            if (parsed != null) {
                return parsed;
            }
        }

        nested = json.get("text");
        if (nested instanceof String nestedStr) {
            JSONObject parsed = CozeResponseParser.parse(nestedStr);
            if (parsed != null) {
                return parsed;
            }
        }

        return json;
    }

    private String getStringByAliases(JSONObject json, String fallback, String... keys) {
        if (json == null || keys == null) {
            return fallback;
        }
        for (String key : keys) {
            if (StrUtil.isBlank(key)) {
                continue;
            }
            String value = json.getStr(key);
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return fallback;
    }

    private List<String> getListByAliases(JSONObject json, String... keys) {
        List<String> result = new ArrayList<>();
        if (json == null || keys == null) {
            return result;
        }

        for (String key : keys) {
            if (StrUtil.isBlank(key)) {
                continue;
            }
            List<String> values = parseStringList(json.get(key));
            if (!values.isEmpty()) {
                return values;
            }
        }

        return result;
    }
}
