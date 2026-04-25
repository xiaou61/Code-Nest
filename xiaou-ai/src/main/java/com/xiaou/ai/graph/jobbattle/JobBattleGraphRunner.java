package com.xiaou.ai.graph.jobbattle;

import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.dto.jobbattle.JobBattleTargetAnalysisResult;
import com.xiaou.ai.prompt.AiPromptSections;
import com.xiaou.ai.prompt.jobbattle.JobBattleRagQuerySpecs;
import com.xiaou.ai.rag.AiRagRetrievalProfile;
import com.xiaou.ai.rag.jobbattle.JobBattleRagRetrievalProfiles;
import com.xiaou.ai.rag.LlamaIndexClient;
import com.xiaou.ai.rag.LlamaIndexRetrieveRequest;
import com.xiaou.ai.rag.LlamaIndexRetrieveResponse;
import com.xiaou.ai.scene.jobbattle.JobBattleSceneSupport;
import com.xiaou.common.exception.ai.AiGraphExecutionException;
import com.xiaou.common.exception.ai.AiRetrievalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 求职作战台图编排执行器。
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobBattleGraphRunner {

    private static final String START = "__START__";
    private static final String END = "__END__";

    private final JobBattleSceneSupport sceneSupport;
    private final LlamaIndexClient llamaIndexClient;

    private volatile CompiledGraph<JobBattleState> parseJdGraph;
    private volatile CompiledGraph<JobBattleState> matchResumeGraph;
    private volatile CompiledGraph<JobBattleState> generatePlanGraph;
    private volatile CompiledGraph<JobBattleState> reviewInterviewGraph;
    private volatile CompiledGraph<JobBattleState> analyzeTargetGraph;

    public JobBattleJdParseResult runParseJd(String jdText, String targetRole, String targetLevel, String city) {
        Map<String, Object> input = new HashMap<>();
        input.put(JobBattleState.TASK_TYPE, JobBattleTaskType.PARSE_JD.name());
        input.put(JobBattleState.JD_TEXT, jdText);
        input.put(JobBattleState.TARGET_ROLE, targetRole);
        input.put(JobBattleState.TARGET_LEVEL, targetLevel);
        input.put(JobBattleState.CITY, city);

        JobBattleState state = invokeGraph(getParseJdGraph(), input, "job_battle_parse_jd_graph");
        return parseJdResult(state.jdParseResultJson(), jdText);
    }

    public JobBattleResumeMatchResult runMatchResume(String parsedJdJson, String resumeText,
                                                     String projectHighlights, String targetCompanyType) {
        Map<String, Object> input = new HashMap<>();
        input.put(JobBattleState.TASK_TYPE, JobBattleTaskType.MATCH_RESUME.name());
        input.put(JobBattleState.PARSED_JD_JSON, parsedJdJson);
        input.put(JobBattleState.RESUME_TEXT, resumeText);
        input.put(JobBattleState.PROJECT_HIGHLIGHTS, projectHighlights);
        input.put(JobBattleState.TARGET_COMPANY_TYPE, targetCompanyType);

        JobBattleState state = invokeGraph(getMatchResumeGraph(), input, "job_battle_match_resume_graph");
        return parseResumeMatchResult(state.resumeMatchResultJson(), parsedJdJson, resumeText);
    }

    public JobBattlePlanResult runGeneratePlan(String gapsJson, Integer targetDays, Integer weeklyHours,
                                               String preferredLearningMode, String nextInterviewDate) {
        Map<String, Object> input = new HashMap<>();
        input.put(JobBattleState.TASK_TYPE, JobBattleTaskType.GENERATE_PLAN.name());
        input.put(JobBattleState.GAPS_JSON, gapsJson);
        input.put(JobBattleState.TARGET_DAYS, targetDays);
        input.put(JobBattleState.WEEKLY_HOURS, weeklyHours);
        input.put(JobBattleState.PREFERRED_LEARNING_MODE, preferredLearningMode);
        input.put(JobBattleState.NEXT_INTERVIEW_DATE, nextInterviewDate);

        JobBattleState state = invokeGraph(getGeneratePlanGraph(), input, "job_battle_generate_plan_graph");
        return parsePlanResult(state.planResultJson(), targetDays, weeklyHours);
    }

    public JobBattleInterviewReviewResult runReviewInterview(String interviewNotes, String qaTranscriptJson,
                                                             String interviewResult, String targetRole,
                                                             String nextInterviewDate) {
        Map<String, Object> input = new HashMap<>();
        input.put(JobBattleState.TASK_TYPE, JobBattleTaskType.REVIEW_INTERVIEW.name());
        input.put(JobBattleState.INTERVIEW_NOTES, interviewNotes);
        input.put(JobBattleState.QA_TRANSCRIPT_JSON, qaTranscriptJson);
        input.put(JobBattleState.INTERVIEW_RESULT, interviewResult);
        input.put(JobBattleState.TARGET_ROLE, targetRole);
        input.put(JobBattleState.NEXT_INTERVIEW_DATE, nextInterviewDate);

        JobBattleState state = invokeGraph(getReviewInterviewGraph(), input, "job_battle_review_interview_graph");
        return parseReviewResult(state.reviewResultJson(), interviewResult);
    }

    public JobBattleTargetAnalysisResult runAnalyzeTarget(String jdText, String targetRole, String targetLevel, String city,
                                                          String resumeText, String projectHighlights, String targetCompanyType) {
        Map<String, Object> input = new HashMap<>();
        input.put(JobBattleState.TASK_TYPE, JobBattleTaskType.ANALYZE_TARGET.name());
        input.put(JobBattleState.JD_TEXT, jdText);
        input.put(JobBattleState.TARGET_ROLE, targetRole);
        input.put(JobBattleState.TARGET_LEVEL, targetLevel);
        input.put(JobBattleState.CITY, city);
        input.put(JobBattleState.RESUME_TEXT, resumeText);
        input.put(JobBattleState.PROJECT_HIGHLIGHTS, projectHighlights);
        input.put(JobBattleState.TARGET_COMPANY_TYPE, targetCompanyType);

        JobBattleState state = invokeGraph(getAnalyzeTargetGraph(), input, "job_battle_analyze_target_graph");
        JobBattleJdParseResult jdParse = parseJdResult(state.jdParseResultJson(), jdText);
        JobBattleResumeMatchResult resumeMatch = parseResumeMatchResult(
                state.resumeMatchResultJson(),
                state.parsedJdJson(),
                resumeText
        );
        return new JobBattleTargetAnalysisResult()
                .setJdParse(jdParse)
                .setResumeMatch(resumeMatch)
                .setFallback(jdParse.isFallback() || resumeMatch.isFallback());
    }

    private Map<String, Object> retrieveContext(JobBattleState state) {
        if (!llamaIndexClient.isAvailable()) {
            return update(JobBattleState.RAG_CONTEXT, "");
        }

        AiRagRetrievalProfile retrievalProfile = resolveRetrievalProfile(state);
        String query = buildRetrieveQuery(state);
        if (retrievalProfile == null || !StringUtils.hasText(query)) {
            return update(JobBattleState.RAG_CONTEXT, "");
        }

        try {
            LlamaIndexRetrieveRequest request = retrievalProfile.buildRequest(query);
            LlamaIndexRetrieveResponse response = llamaIndexClient.retrieve(request);
            return update(JobBattleState.RAG_CONTEXT, response.toContextSnippet());
        } catch (AiRetrievalException e) {
            log.warn("求职作战台图编排检索增强不可用，继续执行纯模型链路: {}", e.getMessage());
            return update(JobBattleState.RAG_CONTEXT, "");
        }
    }

    private AiRagRetrievalProfile resolveRetrievalProfile(JobBattleState state) {
        return switch (state.taskType()) {
            case PARSE_JD -> JobBattleRagRetrievalProfiles.PARSE_JD;
            case MATCH_RESUME -> JobBattleRagRetrievalProfiles.MATCH_RESUME;
            case GENERATE_PLAN -> JobBattleRagRetrievalProfiles.GENERATE_PLAN;
            case REVIEW_INTERVIEW -> JobBattleRagRetrievalProfiles.REVIEW_INTERVIEW;
            case ANALYZE_TARGET -> JobBattleRagRetrievalProfiles.ANALYZE_TARGET;
        };
    }

    private Map<String, Object> parseJd(JobBattleState state) {
        JobBattleJdParseResult result = sceneSupport.parseJd(
                state.jdText(),
                state.targetRole(),
                state.targetLevel(),
                state.city(),
                state.ragContext()
        );
        Map<String, Object> updates = new LinkedHashMap<>();
        String json = JSONUtil.toJsonStr(result);
        updates.put(JobBattleState.JD_PARSE_RESULT_JSON, json);
        updates.put(JobBattleState.PARSED_JD_JSON, json);
        return updates;
    }

    private Map<String, Object> matchResume(JobBattleState state) {
        String parsedJdJson = StringUtils.hasText(state.parsedJdJson())
                ? state.parsedJdJson()
                : state.jdParseResultJson();
        JobBattleResumeMatchResult result = sceneSupport.matchResume(
                parsedJdJson,
                state.resumeText(),
                state.projectHighlights(),
                state.targetCompanyType(),
                state.ragContext()
        );
        return update(JobBattleState.RESUME_MATCH_RESULT_JSON, JSONUtil.toJsonStr(result));
    }

    private Map<String, Object> generatePlan(JobBattleState state) {
        JobBattlePlanResult result = sceneSupport.generatePlan(
                state.gapsJson(),
                state.targetDays(),
                state.weeklyHours(),
                state.preferredLearningMode(),
                state.nextInterviewDate(),
                state.ragContext()
        );
        return update(JobBattleState.PLAN_RESULT_JSON, JSONUtil.toJsonStr(result));
    }

    private Map<String, Object> reviewInterview(JobBattleState state) {
        JobBattleInterviewReviewResult result = sceneSupport.reviewInterview(
                state.interviewNotes(),
                state.qaTranscriptJson(),
                state.interviewResult(),
                state.targetRole(),
                state.nextInterviewDate(),
                state.ragContext()
        );
        return update(JobBattleState.REVIEW_RESULT_JSON, JSONUtil.toJsonStr(result));
    }

    private JobBattleState invokeGraph(CompiledGraph<JobBattleState> graph,
                                       Map<String, Object> input,
                                       String graphName) {
        try {
            return graph.invoke(input)
                    .orElseThrow(() -> new AiGraphExecutionException("图编排执行未返回最终状态: " + graphName));
        } catch (AiGraphExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new AiGraphExecutionException("图编排执行失败: " + graphName, e);
        }
    }

    private JobBattleJdParseResult parseJdResult(String json, String jdText) {
        if (!StringUtils.hasText(json)) {
            return JobBattleJdParseResult.fallbackResult(jdText);
        }
        try {
            JobBattleJdParseResult result = JSONUtil.toBean(json, JobBattleJdParseResult.class);
            if (result.getMustSkills() == null) {
                result.setMustSkills(new ArrayList<>());
            }
            if (result.getNiceSkills() == null) {
                result.setNiceSkills(new ArrayList<>());
            }
            if (result.getResponsibilities() == null) {
                result.setResponsibilities(new ArrayList<>());
            }
            if (result.getKeywords() == null) {
                result.setKeywords(new ArrayList<>());
            }
            if (result.getRiskPoints() == null) {
                result.setRiskPoints(new ArrayList<>());
            }
            return result;
        } catch (Exception e) {
            log.warn("解析求职作战台 JD 结果失败，返回降级结果", e);
            return JobBattleJdParseResult.fallbackResult(jdText);
        }
    }

    private JobBattleResumeMatchResult parseResumeMatchResult(String json, String parsedJdJson, String resumeText) {
        if (!StringUtils.hasText(json)) {
            return JobBattleResumeMatchResult.fallbackResult(parsedJdJson, resumeText);
        }
        try {
            JobBattleResumeMatchResult result = JSONUtil.toBean(json, JobBattleResumeMatchResult.class);
            if (result.getDimensionScores() == null) {
                result.setDimensionScores(new LinkedHashMap<>());
            }
            if (result.getStrengths() == null) {
                result.setStrengths(new ArrayList<>());
            }
            if (result.getGaps() == null) {
                result.setGaps(new ArrayList<>());
            }
            if (result.getResumeRewriteSuggestions() == null) {
                result.setResumeRewriteSuggestions(new ArrayList<>());
            }
            if (result.getMissingKeywords() == null) {
                result.setMissingKeywords(new ArrayList<>());
            }
            return result;
        } catch (Exception e) {
            log.warn("解析求职作战台简历匹配结果失败，返回降级结果", e);
            return JobBattleResumeMatchResult.fallbackResult(parsedJdJson, resumeText);
        }
    }

    private JobBattlePlanResult parsePlanResult(String json, Integer targetDays, Integer weeklyHours) {
        if (!StringUtils.hasText(json)) {
            return JobBattlePlanResult.fallbackResult(targetDays, weeklyHours);
        }
        try {
            JobBattlePlanResult result = JSONUtil.toBean(json, JobBattlePlanResult.class);
            if (result.getWeeklyGoals() == null) {
                result.setWeeklyGoals(new ArrayList<>());
            }
            if (result.getDailyTasks() == null) {
                result.setDailyTasks(new ArrayList<>());
            }
            if (result.getMilestones() == null) {
                result.setMilestones(new ArrayList<>());
            }
            if (result.getRiskAndFallback() == null) {
                result.setRiskAndFallback(new ArrayList<>());
            }
            return result;
        } catch (Exception e) {
            log.warn("解析求职作战台计划结果失败，返回降级结果", e);
            return JobBattlePlanResult.fallbackResult(targetDays, weeklyHours);
        }
    }

    private JobBattleInterviewReviewResult parseReviewResult(String json, String interviewResult) {
        if (!StringUtils.hasText(json)) {
            return JobBattleInterviewReviewResult.fallbackResult(interviewResult);
        }
        try {
            JobBattleInterviewReviewResult result = JSONUtil.toBean(json, JobBattleInterviewReviewResult.class);
            if (result.getRootCauses() == null) {
                result.setRootCauses(new ArrayList<>());
            }
            if (result.getHighImpactFixes() == null) {
                result.setHighImpactFixes(new ArrayList<>());
            }
            if (result.getQuestionTypeWeakness() == null) {
                result.setQuestionTypeWeakness(new ArrayList<>());
            }
            if (result.getNext7DayPlan() == null) {
                result.setNext7DayPlan(new ArrayList<>());
            }
            return result;
        } catch (Exception e) {
            log.warn("解析求职作战台复盘结果失败，返回降级结果", e);
            return JobBattleInterviewReviewResult.fallbackResult(interviewResult);
        }
    }

    private CompiledGraph<JobBattleState> getParseJdGraph() {
        CompiledGraph<JobBattleState> local = parseJdGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (parseJdGraph == null) {
                parseJdGraph = buildParseJdGraph();
            }
            return parseJdGraph;
        }
    }

    private CompiledGraph<JobBattleState> getMatchResumeGraph() {
        CompiledGraph<JobBattleState> local = matchResumeGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (matchResumeGraph == null) {
                matchResumeGraph = buildMatchResumeGraph();
            }
            return matchResumeGraph;
        }
    }

    private CompiledGraph<JobBattleState> getGeneratePlanGraph() {
        CompiledGraph<JobBattleState> local = generatePlanGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (generatePlanGraph == null) {
                generatePlanGraph = buildGeneratePlanGraph();
            }
            return generatePlanGraph;
        }
    }

    private CompiledGraph<JobBattleState> getReviewInterviewGraph() {
        CompiledGraph<JobBattleState> local = reviewInterviewGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (reviewInterviewGraph == null) {
                reviewInterviewGraph = buildReviewInterviewGraph();
            }
            return reviewInterviewGraph;
        }
    }

    private CompiledGraph<JobBattleState> getAnalyzeTargetGraph() {
        CompiledGraph<JobBattleState> local = analyzeTargetGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (analyzeTargetGraph == null) {
                analyzeTargetGraph = buildAnalyzeTargetGraph();
            }
            return analyzeTargetGraph;
        }
    }

    private CompiledGraph<JobBattleState> buildParseJdGraph() {
        try {
            StateGraph<JobBattleState> graph = new StateGraph<>(channels(), JobBattleState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("parse_jd", AsyncNodeAction.node_async(this::parseJd));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "parse_jd");
            graph.addEdge("parse_jd", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建求职作战台 JD 解析图失败", e);
        }
    }

    private CompiledGraph<JobBattleState> buildMatchResumeGraph() {
        try {
            StateGraph<JobBattleState> graph = new StateGraph<>(channels(), JobBattleState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("match_resume", AsyncNodeAction.node_async(this::matchResume));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "match_resume");
            graph.addEdge("match_resume", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建求职作战台简历匹配图失败", e);
        }
    }

    private CompiledGraph<JobBattleState> buildGeneratePlanGraph() {
        try {
            StateGraph<JobBattleState> graph = new StateGraph<>(channels(), JobBattleState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("generate_plan", AsyncNodeAction.node_async(this::generatePlan));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "generate_plan");
            graph.addEdge("generate_plan", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建求职作战台计划图失败", e);
        }
    }

    private CompiledGraph<JobBattleState> buildReviewInterviewGraph() {
        try {
            StateGraph<JobBattleState> graph = new StateGraph<>(channels(), JobBattleState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("review_interview", AsyncNodeAction.node_async(this::reviewInterview));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "review_interview");
            graph.addEdge("review_interview", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建求职作战台复盘图失败", e);
        }
    }

    private CompiledGraph<JobBattleState> buildAnalyzeTargetGraph() {
        try {
            StateGraph<JobBattleState> graph = new StateGraph<>(channels(), JobBattleState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("parse_jd", AsyncNodeAction.node_async(this::parseJd));
            graph.addNode("match_resume", AsyncNodeAction.node_async(this::matchResume));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "parse_jd");
            graph.addEdge("parse_jd", "match_resume");
            graph.addEdge("match_resume", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建求职作战台综合分析图失败", e);
        }
    }

    private Map<String, Channel<?>> channels() {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put(JobBattleState.TASK_TYPE, Channels.base(() -> JobBattleTaskType.PARSE_JD.name()));
        channels.put(JobBattleState.JD_TEXT, Channels.base(() -> ""));
        channels.put(JobBattleState.TARGET_ROLE, Channels.base(() -> ""));
        channels.put(JobBattleState.TARGET_LEVEL, Channels.base(() -> ""));
        channels.put(JobBattleState.CITY, Channels.base(() -> ""));
        channels.put(JobBattleState.PARSED_JD_JSON, Channels.base(() -> ""));
        channels.put(JobBattleState.RESUME_TEXT, Channels.base(() -> ""));
        channels.put(JobBattleState.PROJECT_HIGHLIGHTS, Channels.base(() -> ""));
        channels.put(JobBattleState.TARGET_COMPANY_TYPE, Channels.base(() -> ""));
        channels.put(JobBattleState.GAPS_JSON, Channels.base(() -> ""));
        channels.put(JobBattleState.TARGET_DAYS, Channels.base(() -> 0));
        channels.put(JobBattleState.WEEKLY_HOURS, Channels.base(() -> 0));
        channels.put(JobBattleState.PREFERRED_LEARNING_MODE, Channels.base(() -> ""));
        channels.put(JobBattleState.NEXT_INTERVIEW_DATE, Channels.base(() -> ""));
        channels.put(JobBattleState.INTERVIEW_NOTES, Channels.base(() -> ""));
        channels.put(JobBattleState.QA_TRANSCRIPT_JSON, Channels.base(() -> ""));
        channels.put(JobBattleState.INTERVIEW_RESULT, Channels.base(() -> ""));
        channels.put(JobBattleState.RAG_CONTEXT, Channels.base(() -> ""));
        channels.put(JobBattleState.JD_PARSE_RESULT_JSON, Channels.base(() -> ""));
        channels.put(JobBattleState.RESUME_MATCH_RESULT_JSON, Channels.base(() -> ""));
        channels.put(JobBattleState.PLAN_RESULT_JSON, Channels.base(() -> ""));
        channels.put(JobBattleState.REVIEW_RESULT_JSON, Channels.base(() -> ""));
        return channels;
    }

    private Map<String, Object> update(String key, Object value) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(key, value);
        return updates;
    }

    private String buildRetrieveQuery(JobBattleState state) {
        return switch (state.taskType()) {
            case PARSE_JD -> JobBattleRagQuerySpecs.PARSE_JD.render(Map.of(
                    "targetRole", AiPromptSections.text(state.targetRole()),
                    "targetLevel", AiPromptSections.text(state.targetLevel()),
                    "city", AiPromptSections.text(state.city()),
                    "jdText", AiPromptSections.text(state.jdText())
            ));
            case MATCH_RESUME -> JobBattleRagQuerySpecs.MATCH_RESUME.render(Map.of(
                    "parsedJdJson", AiPromptSections.text(state.parsedJdJson()),
                    "resumeText", AiPromptSections.text(state.resumeText()),
                    "projectHighlights", AiPromptSections.text(state.projectHighlights()),
                    "targetCompanyType", AiPromptSections.text(state.targetCompanyType())
            ));
            case GENERATE_PLAN -> JobBattleRagQuerySpecs.GENERATE_PLAN.render(Map.of(
                    "gapsJson", AiPromptSections.text(state.gapsJson()),
                    "targetDays", state.targetDays(),
                    "weeklyHours", state.weeklyHours(),
                    "preferredLearningMode", AiPromptSections.text(state.preferredLearningMode()),
                    "nextInterviewDate", AiPromptSections.text(state.nextInterviewDate())
            ));
            case REVIEW_INTERVIEW -> JobBattleRagQuerySpecs.REVIEW_INTERVIEW.render(Map.of(
                    "targetRole", AiPromptSections.text(state.targetRole()),
                    "interviewResult", AiPromptSections.text(state.interviewResult()),
                    "interviewNotes", AiPromptSections.text(state.interviewNotes()),
                    "qaTranscriptJson", AiPromptSections.text(state.qaTranscriptJson()),
                    "nextInterviewDate", AiPromptSections.text(state.nextInterviewDate())
            ));
            case ANALYZE_TARGET -> JobBattleRagQuerySpecs.ANALYZE_TARGET.render(Map.of(
                    "targetRole", AiPromptSections.text(state.targetRole()),
                    "targetLevel", AiPromptSections.text(state.targetLevel()),
                    "city", AiPromptSections.text(state.city()),
                    "jdText", AiPromptSections.text(state.jdText()),
                    "resumeText", AiPromptSections.text(state.resumeText()),
                    "projectHighlights", AiPromptSections.text(state.projectHighlights()),
                    "targetCompanyType", AiPromptSections.text(state.targetCompanyType())
            ));
        };
    }

}
