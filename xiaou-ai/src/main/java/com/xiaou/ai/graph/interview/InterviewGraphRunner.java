package com.xiaou.ai.graph.interview;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.interview.AnswerEvaluationResult;
import com.xiaou.ai.dto.interview.GeneratedQuestion;
import com.xiaou.ai.dto.interview.InterviewSummaryResult;
import com.xiaou.ai.prompt.AiPromptSections;
import com.xiaou.ai.prompt.interview.InterviewRagQuerySpecs;
import com.xiaou.ai.rag.AiRagRetrievalProfile;
import com.xiaou.ai.rag.interview.InterviewRagRetrievalProfiles;
import com.xiaou.ai.rag.LlamaIndexClient;
import com.xiaou.ai.rag.LlamaIndexRetrieveRequest;
import com.xiaou.ai.rag.LlamaIndexRetrieveResponse;
import com.xiaou.ai.scene.interview.InterviewSceneSupport;
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
 * 模拟面试图编排执行器。
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewGraphRunner {

    private static final String START = "__START__";
    private static final String END = "__END__";

    private final InterviewSceneSupport sceneSupport;
    private final LlamaIndexClient llamaIndexClient;

    private volatile CompiledGraph<InterviewState> questionGraph;
    private volatile CompiledGraph<InterviewState> evaluationGraph;
    private volatile CompiledGraph<InterviewState> summaryGraph;
    private volatile CompiledGraph<InterviewState> followUpGraph;

    public List<GeneratedQuestion> runGenerateQuestions(String direction, String level, int count) {
        Map<String, Object> input = new HashMap<>();
        input.put(InterviewState.TASK_TYPE, InterviewTaskType.GENERATE_QUESTIONS.name());
        input.put(InterviewState.DIRECTION, direction);
        input.put(InterviewState.LEVEL, level);
        input.put(InterviewState.GENERATED_COUNT, count);

        InterviewState state = invokeGraph(getQuestionGraph(), input, "interview_generate_questions_graph");
        return parseGeneratedQuestions(state.generatedQuestionsJson());
    }

    public AnswerEvaluationResult runEvaluateAnswer(String direction, String level, String style,
                                                    String question, String answer, int followUpCount) {
        Map<String, Object> input = new HashMap<>();
        input.put(InterviewState.TASK_TYPE, InterviewTaskType.EVALUATE_ANSWER.name());
        input.put(InterviewState.DIRECTION, direction);
        input.put(InterviewState.LEVEL, level);
        input.put(InterviewState.STYLE, style);
        input.put(InterviewState.QUESTION, question);
        input.put(InterviewState.ANSWER, answer);
        input.put(InterviewState.FOLLOW_UP_COUNT, followUpCount);

        InterviewState state = invokeGraph(getEvaluationGraph(), input, "interview_evaluate_answer_graph");
        return parseEvaluationResult(state.evaluationResultJson(), answer, followUpCount);
    }

    public InterviewSummaryResult runGenerateSummary(String direction, String level,
                                                     int questionCount, int answeredCount, int skippedCount,
                                                     int totalScore, String qaListJson) {
        Map<String, Object> input = new HashMap<>();
        input.put(InterviewState.TASK_TYPE, InterviewTaskType.GENERATE_SUMMARY.name());
        input.put(InterviewState.DIRECTION, direction);
        input.put(InterviewState.LEVEL, level);
        input.put(InterviewState.QUESTION_COUNT, questionCount);
        input.put(InterviewState.ANSWERED_COUNT, answeredCount);
        input.put(InterviewState.SKIPPED_COUNT, skippedCount);
        input.put(InterviewState.TOTAL_SCORE, totalScore);
        input.put(InterviewState.QA_LIST_JSON, qaListJson);

        InterviewState state = invokeGraph(getSummaryGraph(), input, "interview_generate_summary_graph");
        return parseSummaryResult(state.summaryResultJson(), totalScore);
    }

    public String runGenerateFollowUp(String direction, String level, String style,
                                      String question, String answer, int followUpCount) {
        Map<String, Object> input = new HashMap<>();
        input.put(InterviewState.TASK_TYPE, InterviewTaskType.GENERATE_FOLLOW_UP.name());
        input.put(InterviewState.DIRECTION, direction);
        input.put(InterviewState.LEVEL, level);
        input.put(InterviewState.STYLE, style);
        input.put(InterviewState.QUESTION, question);
        input.put(InterviewState.ANSWER, answer);
        input.put(InterviewState.FOLLOW_UP_COUNT, followUpCount);

        InterviewState state = invokeGraph(getFollowUpGraph(), input, "interview_generate_follow_up_graph");
        return StringUtils.hasText(state.followUpQuestion())
                ? state.followUpQuestion()
                : fallbackFollowUpQuestion(answer);
    }

    private Map<String, Object> retrieveContext(InterviewState state) {
        if (!llamaIndexClient.isAvailable()) {
            return update(InterviewState.RAG_CONTEXT, "");
        }

        AiRagRetrievalProfile retrievalProfile = resolveRetrievalProfile(state);
        String query = buildRetrieveQuery(state);
        if (retrievalProfile == null || !StringUtils.hasText(query)) {
            return update(InterviewState.RAG_CONTEXT, "");
        }

        try {
            LlamaIndexRetrieveRequest request = retrievalProfile.buildRequest(query);
            LlamaIndexRetrieveResponse response = llamaIndexClient.retrieve(request);
            return update(InterviewState.RAG_CONTEXT, response.toContextSnippet());
        } catch (AiRetrievalException e) {
            log.warn("模拟面试图编排检索增强不可用，继续执行纯模型链路: {}", e.getMessage());
            return update(InterviewState.RAG_CONTEXT, "");
        }
    }

    private AiRagRetrievalProfile resolveRetrievalProfile(InterviewState state) {
        return switch (state.taskType()) {
            case GENERATE_QUESTIONS -> InterviewRagRetrievalProfiles.GENERATE_QUESTIONS;
            case GENERATE_SUMMARY -> InterviewRagRetrievalProfiles.GENERATE_SUMMARY;
            case GENERATE_FOLLOW_UP -> InterviewRagRetrievalProfiles.GENERATE_FOLLOW_UP;
            case EVALUATE_ANSWER -> InterviewRagRetrievalProfiles.EVALUATE_ANSWER;
        };
    }

    private Map<String, Object> generateQuestions(InterviewState state) {
        List<GeneratedQuestion> questions = sceneSupport.generateQuestions(
                state.direction(),
                state.level(),
                state.generatedCount(),
                state.ragContext()
        );
        return update(InterviewState.GENERATED_QUESTIONS_JSON, JSONUtil.toJsonStr(questions));
    }

    private Map<String, Object> evaluateAnswer(InterviewState state) {
        AnswerEvaluationResult result = sceneSupport.evaluateAnswer(
                state.direction(),
                state.level(),
                state.style(),
                state.question(),
                state.answer(),
                state.followUpCount(),
                state.ragContext()
        );
        return update(InterviewState.EVALUATION_RESULT_JSON, JSONUtil.toJsonStr(result));
    }

    private Map<String, Object> ensureFollowUpQuestion(InterviewState state) {
        AnswerEvaluationResult result = parseEvaluationResult(
                state.evaluationResultJson(),
                state.answer(),
                state.followUpCount()
        );

        if (!"followUp".equalsIgnoreCase(result.getNextAction())) {
            return update(InterviewState.EVALUATION_RESULT_JSON, JSONUtil.toJsonStr(result));
        }
        if (StringUtils.hasText(result.getFollowUpQuestion())) {
            return update(InterviewState.EVALUATION_RESULT_JSON, JSONUtil.toJsonStr(result));
        }

        String followUpQuestion = sceneSupport.generateFollowUpQuestion(
                state.direction(),
                state.level(),
                state.style(),
                state.question(),
                state.answer(),
                state.followUpCount(),
                state.ragContext()
        );
        result.setFollowUpQuestion(followUpQuestion);
        return update(InterviewState.EVALUATION_RESULT_JSON, JSONUtil.toJsonStr(result));
    }

    private Map<String, Object> generateSummary(InterviewState state) {
        InterviewSummaryResult result = sceneSupport.generateSummary(
                state.direction(),
                state.level(),
                state.questionCount(),
                state.answeredCount(),
                state.skippedCount(),
                state.totalScore(),
                state.qaListJson(),
                state.ragContext()
        );
        return update(InterviewState.SUMMARY_RESULT_JSON, JSONUtil.toJsonStr(result));
    }

    private Map<String, Object> generateFollowUp(InterviewState state) {
        String followUpQuestion = sceneSupport.generateFollowUpQuestion(
                state.direction(),
                state.level(),
                state.style(),
                state.question(),
                state.answer(),
                state.followUpCount(),
                state.ragContext()
        );
        return update(InterviewState.FOLLOW_UP_QUESTION, followUpQuestion);
    }

    private InterviewState invokeGraph(CompiledGraph<InterviewState> graph,
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

    private List<GeneratedQuestion> parseGeneratedQuestions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            JSONArray array = JSONUtil.parseArray(json);
            return JSONUtil.toList(array, GeneratedQuestion.class);
        } catch (Exception e) {
            log.warn("解析面试题图编排结果失败，返回空列表", e);
            return List.of();
        }
    }

    private AnswerEvaluationResult parseEvaluationResult(String json, String answer, int followUpCount) {
        if (!StringUtils.hasText(json)) {
            return AnswerEvaluationResult.fallbackResult(answer, followUpCount);
        }
        try {
            AnswerEvaluationResult result = JSONUtil.toBean(json, AnswerEvaluationResult.class);
            if (result.getFeedback() == null) {
                AnswerEvaluationResult.Feedback feedback = new AnswerEvaluationResult.Feedback();
                feedback.setStrengths(new ArrayList<>());
                feedback.setImprovements(new ArrayList<>());
                result.setFeedback(feedback);
            }
            if (result.getReferencePoints() == null) {
                result.setReferencePoints(new ArrayList<>());
            }
            if (!StringUtils.hasText(result.getNextAction())) {
                result.setNextAction("nextQuestion");
            }
            return result;
        } catch (Exception e) {
            log.warn("解析面试评价图编排结果失败，返回降级结果", e);
            return AnswerEvaluationResult.fallbackResult(answer, followUpCount);
        }
    }

    private InterviewSummaryResult parseSummaryResult(String json, int totalScore) {
        if (!StringUtils.hasText(json)) {
            return InterviewSummaryResult.fallbackResult(totalScore);
        }
        try {
            InterviewSummaryResult result = JSONUtil.toBean(json, InterviewSummaryResult.class);
            if (result.getSuggestions() == null) {
                result.setSuggestions(new ArrayList<>());
            }
            return result;
        } catch (Exception e) {
            log.warn("解析面试总结图编排结果失败，返回降级结果", e);
            return InterviewSummaryResult.fallbackResult(totalScore);
        }
    }

    private CompiledGraph<InterviewState> getQuestionGraph() {
        CompiledGraph<InterviewState> local = questionGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (questionGraph == null) {
                questionGraph = buildQuestionGraph();
            }
            return questionGraph;
        }
    }

    private CompiledGraph<InterviewState> getEvaluationGraph() {
        CompiledGraph<InterviewState> local = evaluationGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (evaluationGraph == null) {
                evaluationGraph = buildEvaluationGraph();
            }
            return evaluationGraph;
        }
    }

    private CompiledGraph<InterviewState> getSummaryGraph() {
        CompiledGraph<InterviewState> local = summaryGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (summaryGraph == null) {
                summaryGraph = buildSummaryGraph();
            }
            return summaryGraph;
        }
    }

    private CompiledGraph<InterviewState> getFollowUpGraph() {
        CompiledGraph<InterviewState> local = followUpGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (followUpGraph == null) {
                followUpGraph = buildFollowUpGraph();
            }
            return followUpGraph;
        }
    }

    private CompiledGraph<InterviewState> buildQuestionGraph() {
        try {
            StateGraph<InterviewState> graph = new StateGraph<>(channels(), InterviewState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("generate_questions", AsyncNodeAction.node_async(this::generateQuestions));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "generate_questions");
            graph.addEdge("generate_questions", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建模拟面试题目生成图失败", e);
        }
    }

    private CompiledGraph<InterviewState> buildEvaluationGraph() {
        try {
            StateGraph<InterviewState> graph = new StateGraph<>(channels(), InterviewState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("evaluate_answer", AsyncNodeAction.node_async(this::evaluateAnswer));
            graph.addNode("generate_follow_up", AsyncNodeAction.node_async(this::ensureFollowUpQuestion));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "evaluate_answer");
            graph.addEdge("evaluate_answer", "generate_follow_up");
            graph.addEdge("generate_follow_up", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建模拟面试评价图失败", e);
        }
    }

    private CompiledGraph<InterviewState> buildSummaryGraph() {
        try {
            StateGraph<InterviewState> graph = new StateGraph<>(channels(), InterviewState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("generate_summary", AsyncNodeAction.node_async(this::generateSummary));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "generate_summary");
            graph.addEdge("generate_summary", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建模拟面试总结图失败", e);
        }
    }

    private CompiledGraph<InterviewState> buildFollowUpGraph() {
        try {
            StateGraph<InterviewState> graph = new StateGraph<>(channels(), InterviewState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("generate_follow_up", AsyncNodeAction.node_async(this::generateFollowUp));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "generate_follow_up");
            graph.addEdge("generate_follow_up", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建模拟面试追问图失败", e);
        }
    }

    private Map<String, Channel<?>> channels() {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put(InterviewState.TASK_TYPE, Channels.base(() -> InterviewTaskType.EVALUATE_ANSWER.name()));
        channels.put(InterviewState.DIRECTION, Channels.base(() -> ""));
        channels.put(InterviewState.LEVEL, Channels.base(() -> ""));
        channels.put(InterviewState.STYLE, Channels.base(() -> ""));
        channels.put(InterviewState.QUESTION, Channels.base(() -> ""));
        channels.put(InterviewState.ANSWER, Channels.base(() -> ""));
        channels.put(InterviewState.FOLLOW_UP_COUNT, Channels.base(() -> 0));
        channels.put(InterviewState.GENERATED_COUNT, Channels.base(() -> 0));
        channels.put(InterviewState.QUESTION_COUNT, Channels.base(() -> 0));
        channels.put(InterviewState.ANSWERED_COUNT, Channels.base(() -> 0));
        channels.put(InterviewState.SKIPPED_COUNT, Channels.base(() -> 0));
        channels.put(InterviewState.TOTAL_SCORE, Channels.base(() -> 0));
        channels.put(InterviewState.QA_LIST_JSON, Channels.base(() -> ""));
        channels.put(InterviewState.RAG_CONTEXT, Channels.base(() -> ""));
        channels.put(InterviewState.GENERATED_QUESTIONS_JSON, Channels.base(() -> ""));
        channels.put(InterviewState.EVALUATION_RESULT_JSON, Channels.base(() -> ""));
        channels.put(InterviewState.SUMMARY_RESULT_JSON, Channels.base(() -> ""));
        channels.put(InterviewState.FOLLOW_UP_QUESTION, Channels.base(() -> ""));
        return channels;
    }

    private Map<String, Object> update(String key, Object value) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(key, value);
        return updates;
    }

    private String buildRetrieveQuery(InterviewState state) {
        return switch (state.taskType()) {
            case GENERATE_QUESTIONS -> InterviewRagQuerySpecs.GENERATE_QUESTIONS.render(Map.of(
                    "direction", AiPromptSections.text(state.direction()),
                    "level", AiPromptSections.text(state.level()),
                    "count", state.generatedCount()
            ));
            case GENERATE_SUMMARY -> InterviewRagQuerySpecs.GENERATE_SUMMARY.render(Map.of(
                    "direction", AiPromptSections.text(state.direction()),
                    "level", AiPromptSections.text(state.level()),
                    "questionCount", state.questionCount(),
                    "answeredCount", state.answeredCount(),
                    "skippedCount", state.skippedCount(),
                    "totalScore", state.totalScore()
            ));
            case GENERATE_FOLLOW_UP -> InterviewRagQuerySpecs.GENERATE_FOLLOW_UP.render(Map.of(
                    "direction", AiPromptSections.text(state.direction()),
                    "level", AiPromptSections.text(state.level()),
                    "style", AiPromptSections.text(state.style()),
                    "question", AiPromptSections.text(state.question()),
                    "answer", AiPromptSections.text(state.answer()),
                    "followUpCount", state.followUpCount()
            ));
            case EVALUATE_ANSWER -> InterviewRagQuerySpecs.EVALUATE_ANSWER.render(Map.of(
                    "direction", AiPromptSections.text(state.direction()),
                    "level", AiPromptSections.text(state.level()),
                    "style", AiPromptSections.text(state.style()),
                    "question", AiPromptSections.text(state.question()),
                    "answer", AiPromptSections.text(state.answer()),
                    "followUpCount", state.followUpCount()
            ));
        };
    }

    private String fallbackFollowUpQuestion(String answer) {
        if (!StringUtils.hasText(answer) || answer.trim().length() < 50) {
            return "你的回答比较简短，能否展开说说具体的实现方式或应用场景？";
        }
        if (answer.trim().length() < 150) {
            return "你提到了一些关键点，能否举一个实际项目中的例子来说明？";
        }
        return "你的回答很详细，那么如果遇到性能问题或边界情况，你会怎么处理？";
    }

}
