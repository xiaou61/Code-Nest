package com.xiaou.ai.scene.jobbattle;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.metrics.AiMetricsRecorder;
import com.xiaou.ai.prompt.AiPromptSections;
import com.xiaou.ai.prompt.jobbattle.JobBattlePromptSpecs;
import com.xiaou.ai.structured.jobbattle.JobBattleStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.ai.util.AiJsonResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * 求职作战台场景模型调用支撑。
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobBattleSceneSupport {
    private final AiExecutionSupport aiExecutionSupport;
    private final AiMetricsRecorder aiMetricsRecorder;

    public JobBattleJdParseResult parseJd(String jdText, String targetRole, String targetLevel, String city, String ragContext) {
        return aiExecutionSupport.chatWithFallback(
                "job_battle_jd_parse_graph",
                JobBattlePromptSpecs.JD_PARSE,
                buildJdParsePromptVariables(jdText, targetRole, targetLevel, city, ragContext),
                response -> parseJdResponse(response, jdText, targetRole, targetLevel),
                () -> buildLocalJdFallback(jdText, targetRole, targetLevel)
        );
    }

    public JobBattleResumeMatchResult matchResume(String parsedJdJson, String resumeText,
                                                  String projectHighlights, String targetCompanyType,
                                                  String ragContext) {
        return aiExecutionSupport.chatWithFallback(
                "job_battle_resume_match_graph",
                JobBattlePromptSpecs.RESUME_MATCH,
                buildResumeMatchPromptVariables(parsedJdJson, resumeText, projectHighlights, targetCompanyType, ragContext),
                response -> parseResumeMatchResponse(response, parsedJdJson, resumeText),
                () -> JobBattleResumeMatchResult.fallbackResult(parsedJdJson, resumeText)
        );
    }

    public JobBattlePlanResult generatePlan(String gapsJson, Integer targetDays, Integer weeklyHours,
                                            String preferredLearningMode, String nextInterviewDate,
                                            String ragContext) {
        return aiExecutionSupport.chatWithFallback(
                "job_battle_plan_generate_graph",
                JobBattlePromptSpecs.PLAN_GENERATE,
                buildPlanPromptVariables(gapsJson, targetDays, weeklyHours, preferredLearningMode, nextInterviewDate, ragContext),
                response -> parsePlanResponse(response, targetDays, weeklyHours),
                () -> JobBattlePlanResult.fallbackResult(targetDays, weeklyHours)
        );
    }

    public JobBattleInterviewReviewResult reviewInterview(String interviewNotes, String qaTranscriptJson,
                                                          String interviewResult, String targetRole,
                                                          String nextInterviewDate, String ragContext) {
        return aiExecutionSupport.chatWithFallback(
                "job_battle_interview_review_graph",
                JobBattlePromptSpecs.INTERVIEW_REVIEW,
                buildInterviewReviewPromptVariables(interviewNotes, qaTranscriptJson, interviewResult, targetRole, nextInterviewDate, ragContext),
                response -> parseInterviewReviewResponse(response, interviewResult),
                () -> JobBattleInterviewReviewResult.fallbackResult(interviewResult)
        );
    }

    private Map<String, Object> buildJdParsePromptVariables(String jdText, String targetRole, String targetLevel, String city, String ragContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("targetRole", defaultText(targetRole));
        variables.put("targetLevel", defaultText(targetLevel));
        variables.put("city", defaultText(city));
        variables.put("jdText", defaultText(jdText));
        variables.put("ragSection", AiPromptSections.ragSection(ragContext));
        return variables;
    }

    private Map<String, Object> buildResumeMatchPromptVariables(String parsedJdJson, String resumeText,
                                                                String projectHighlights, String targetCompanyType,
                                                                String ragContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("parsedJdJson", defaultText(parsedJdJson));
        variables.put("resumeText", defaultText(resumeText));
        variables.put("projectHighlights", defaultText(projectHighlights));
        variables.put("targetCompanyType", defaultText(targetCompanyType));
        variables.put("ragSection", AiPromptSections.ragSection(ragContext));
        return variables;
    }

    private Map<String, Object> buildPlanPromptVariables(String gapsJson, Integer targetDays, Integer weeklyHours,
                                                         String preferredLearningMode, String nextInterviewDate,
                                                         String ragContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("gapsJson", defaultText(gapsJson));
        variables.put("targetDays", AiPromptSections.positiveInt(targetDays, 30));
        variables.put("weeklyHours", AiPromptSections.positiveInt(weeklyHours, 6));
        variables.put("preferredLearningMode", defaultText(preferredLearningMode));
        variables.put("nextInterviewDate", defaultText(nextInterviewDate));
        variables.put("ragSection", AiPromptSections.ragSection(ragContext));
        return variables;
    }

    private Map<String, Object> buildInterviewReviewPromptVariables(String interviewNotes, String qaTranscriptJson,
                                                                    String interviewResult, String targetRole,
                                                                    String nextInterviewDate, String ragContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("targetRole", defaultText(targetRole));
        variables.put("interviewResult", defaultText(interviewResult));
        variables.put("nextInterviewDate", defaultText(nextInterviewDate));
        variables.put("interviewNotes", defaultText(interviewNotes));
        variables.put("qaTranscriptJson", defaultText(qaTranscriptJson));
        variables.put("ragSection", AiPromptSections.ragSection(ragContext));
        return variables;
    }

    private JobBattleJdParseResult parseJdResponse(String response, String jdText, String targetRole, String targetLevel) {
        log.info("JD解析模型原始响应: {}", abbreviate(response, 500));
        if (AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_jd_parse_graph", JobBattlePromptSpecs.JD_PARSE, "error_response");
            return buildLocalJdFallback(jdText, targetRole, targetLevel);
        }

        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_jd_parse_graph", JobBattlePromptSpecs.JD_PARSE, "invalid_json");
            return buildLocalJdFallback(jdText, targetRole, targetLevel);
        }

        return parseJdResult(json, jdText, targetRole, targetLevel);
    }

    private JobBattleResumeMatchResult parseResumeMatchResponse(String response, String parsedJdJson, String resumeText) {
        if (AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_resume_match_graph", JobBattlePromptSpecs.RESUME_MATCH, "error_response");
            return JobBattleResumeMatchResult.fallbackResult(parsedJdJson, resumeText);
        }
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_resume_match_graph", JobBattlePromptSpecs.RESUME_MATCH, "invalid_json");
            return JobBattleResumeMatchResult.fallbackResult(parsedJdJson, resumeText);
        }
        var validation = JobBattleStructuredOutputSpecs.RESUME_MATCH.validateObject(json);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_resume_match_graph", JobBattlePromptSpecs.RESUME_MATCH, validation.reason());
            return JobBattleResumeMatchResult.fallbackResult(parsedJdJson, resumeText);
        }
        return parseResumeMatchResult(json);
    }

    private JobBattlePlanResult parsePlanResponse(String response, Integer targetDays, Integer weeklyHours) {
        if (AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_plan_generate_graph", JobBattlePromptSpecs.PLAN_GENERATE, "error_response");
            return JobBattlePlanResult.fallbackResult(targetDays, weeklyHours);
        }
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_plan_generate_graph", JobBattlePromptSpecs.PLAN_GENERATE, "invalid_json");
            return JobBattlePlanResult.fallbackResult(targetDays, weeklyHours);
        }
        var validation = JobBattleStructuredOutputSpecs.PLAN_GENERATE.validateObject(json);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_plan_generate_graph", JobBattlePromptSpecs.PLAN_GENERATE, validation.reason());
            return JobBattlePlanResult.fallbackResult(targetDays, weeklyHours);
        }
        return parsePlanResult(json);
    }

    private JobBattleInterviewReviewResult parseInterviewReviewResponse(String response, String interviewResult) {
        if (AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_interview_review_graph", JobBattlePromptSpecs.INTERVIEW_REVIEW, "error_response");
            return JobBattleInterviewReviewResult.fallbackResult(interviewResult);
        }
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_interview_review_graph", JobBattlePromptSpecs.INTERVIEW_REVIEW, "invalid_json");
            return JobBattleInterviewReviewResult.fallbackResult(interviewResult);
        }
        var validation = JobBattleStructuredOutputSpecs.INTERVIEW_REVIEW.validateObject(json);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_interview_review_graph", JobBattlePromptSpecs.INTERVIEW_REVIEW, validation.reason());
            return JobBattleInterviewReviewResult.fallbackResult(interviewResult);
        }
        return parseInterviewReviewResult(json);
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
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_jd_parse_graph", JobBattlePromptSpecs.JD_PARSE, "unexpected_payload");
            JobBattleJdParseResult fallback = buildLocalJdFallback(jdText, targetRole, targetLevel);
            List<String> risks = new ArrayList<>(fallback.getRiskPoints() == null ? List.of() : fallback.getRiskPoints());
            risks.add("AI 返回格式与约定不一致，已使用本地规则兜底");
            fallback.setRiskPoints(risks);
            return fallback;
        }

        JSONObject canonical = new JSONObject();
        canonical.set("jobTitle", jobTitle);
        canonical.set("level", level);
        canonical.set("mustSkills", mustSkills);
        canonical.set("niceSkills", niceSkills);
        canonical.set("responsibilities", responsibilities);
        canonical.set("seniorityYears", seniorityYears);
        canonical.set("keywords", keywords);
        canonical.set("riskPoints", riskPoints);
        canonical.set("summary", summary);

        var validation = JobBattleStructuredOutputSpecs.JD_PARSE.validateObject(canonical);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure("job_battle_jd_parse_graph", JobBattlePromptSpecs.JD_PARSE, validation.reason());
            JobBattleJdParseResult fallback = buildLocalJdFallback(jdText, targetRole, targetLevel);
            List<String> risks = new ArrayList<>(fallback.getRiskPoints() == null ? List.of() : fallback.getRiskPoints());
            risks.add("AI 返回结构不满足字段约束，已使用本地规则兜底");
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
        int overallScore = AiJsonResponseParser.getInt(json, "overallScore", 60);
        result.setOverallScore(overallScore);
        result.setDimensionScores(parseDimensionScores(json.getJSONObject("dimensionScores"), overallScore));
        result.setStrengths(parseStringList(json.get("strengths")));
        result.setMissingKeywords(parseStringList(json.get("missingKeywords")));
        result.setEstimatedPassRate(AiJsonResponseParser.getInt(json, "estimatedPassRate", Math.max(20, overallScore - 10)));
        result.setGaps(parseGapList(json.getJSONArray("gaps")));
        result.setResumeRewriteSuggestions(parseRewriteSuggestions(json.getJSONArray("resumeRewriteSuggestions")));
        result.setFallback(false);
        return result;
    }

    private JobBattlePlanResult parsePlanResult(JSONObject json) {
        JobBattlePlanResult result = new JobBattlePlanResult();
        int totalDays = AiJsonResponseParser.getInt(json, "totalDays", 30);
        result.setPlanName(AiJsonResponseParser.getString(json, "planName", "求职冲刺计划"));
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
        result.setOverallConclusion(AiJsonResponseParser.getString(json, "overallConclusion", "暂无复盘结论"));
        result.setRootCauses(parseStringList(json.get("rootCauses")));
        result.setHighImpactFixes(parseHighImpactFixes(json.getJSONArray("highImpactFixes")));
        result.setQuestionTypeWeakness(parseQuestionTypeWeakness(json.getJSONArray("questionTypeWeakness")));
        result.setNext7DayPlan(parseStringList(json.get("next7DayPlan")));
        result.setConfidenceScore(AiJsonResponseParser.getInt(json, "confidenceScore", 60));
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
                ? "已根据 JD 文本进行本地规则解析，建议检查 AI 输出字段映射"
                : "暂无 JD 文本，无法解析";

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
            JSONObject parsed = AiJsonResponseParser.parse(nestedStr);
            if (parsed != null) {
                return parsed;
            }
        }

        nested = json.get("text");
        if (nested instanceof String nestedStr) {
            JSONObject parsed = AiJsonResponseParser.parse(nestedStr);
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

    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}
