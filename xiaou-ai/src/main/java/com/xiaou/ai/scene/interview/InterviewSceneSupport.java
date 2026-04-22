package com.xiaou.ai.scene.interview;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.interview.AnswerEvaluationResult;
import com.xiaou.ai.dto.interview.GeneratedQuestion;
import com.xiaou.ai.dto.interview.InterviewSummaryResult;
import com.xiaou.ai.metrics.AiMetricsRecorder;
import com.xiaou.ai.prompt.AiPromptSections;
import com.xiaou.ai.prompt.interview.InterviewPromptSpecs;
import com.xiaou.ai.structured.interview.InterviewStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.ai.util.AiJsonResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * 模拟面试场景模型调用支撑。
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewSceneSupport {
    private final AiExecutionSupport aiExecutionSupport;
    private final AiMetricsRecorder aiMetricsRecorder;

    public AnswerEvaluationResult evaluateAnswer(String direction, String level, String style,
                                                 String question, String answer, int followUpCount,
                                                 String ragContext) {
        return aiExecutionSupport.chatWithFallback(
                "mock_interview_evaluate_graph",
                InterviewPromptSpecs.EVALUATE_ANSWER,
                buildEvaluationPromptVariables(direction, level, style, question, answer, followUpCount, ragContext),
                response -> parseEvaluationResult(response, answer, followUpCount),
                () -> AnswerEvaluationResult.fallbackResult(answer, followUpCount)
        );
    }

    public InterviewSummaryResult generateSummary(String direction, String level,
                                                  int questionCount, int answeredCount, int skippedCount,
                                                  int totalScore, String qaListJson, String ragContext) {
        return aiExecutionSupport.chatWithFallback(
                "mock_interview_summary_graph",
                InterviewPromptSpecs.GENERATE_SUMMARY,
                buildSummaryPromptVariables(direction, level, questionCount, answeredCount, skippedCount, totalScore, qaListJson, ragContext),
                response -> parseSummaryResult(response, totalScore),
                () -> InterviewSummaryResult.fallbackResult(totalScore)
        );
    }

    public List<GeneratedQuestion> generateQuestions(String direction, String level, int count, String ragContext) {
        return aiExecutionSupport.chatWithFallback(
                "mock_interview_generate_questions_graph",
                InterviewPromptSpecs.GENERATE_QUESTIONS,
                buildGenerateQuestionsPromptVariables(direction, level, count, ragContext),
                this::parseGeneratedQuestions,
                List::of
        );
    }

    public String generateFollowUpQuestion(String direction, String level, String style,
                                           String question, String answer, int followUpCount,
                                           String ragContext) {
        return aiExecutionSupport.chatWithFallback(
                "mock_interview_generate_follow_up_graph",
                InterviewPromptSpecs.GENERATE_FOLLOW_UP,
                buildFollowUpPromptVariables(direction, level, style, question, answer, followUpCount, ragContext),
                response -> parseFollowUpQuestion(response, answer),
                () -> fallbackFollowUpQuestion(answer)
        );
    }

    private Map<String, Object> buildEvaluationPromptVariables(String direction, String level, String style,
                                                               String question, String answer, int followUpCount,
                                                               String ragContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("direction", AiPromptSections.text(direction));
        variables.put("level", AiPromptSections.text(level));
        variables.put("style", AiPromptSections.text(style));
        variables.put("followUpCount", followUpCount);
        variables.put("question", AiPromptSections.text(question));
        variables.put("answer", AiPromptSections.text(answer));
        variables.put("ragSection", AiPromptSections.ragSection(ragContext));
        return variables;
    }

    private Map<String, Object> buildSummaryPromptVariables(String direction, String level,
                                                            int questionCount, int answeredCount, int skippedCount,
                                                            int totalScore, String qaListJson, String ragContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("direction", AiPromptSections.text(direction));
        variables.put("level", AiPromptSections.text(level));
        variables.put("questionCount", questionCount);
        variables.put("answeredCount", answeredCount);
        variables.put("skippedCount", skippedCount);
        variables.put("totalScore", totalScore);
        variables.put("qaListJson", AiPromptSections.text(qaListJson));
        variables.put("ragSection", AiPromptSections.ragSection(ragContext));
        return variables;
    }

    private Map<String, Object> buildGenerateQuestionsPromptVariables(String direction, String level, int count, String ragContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("direction", AiPromptSections.text(direction));
        variables.put("level", AiPromptSections.text(level));
        variables.put("count", count);
        variables.put("ragSection", AiPromptSections.ragSection(ragContext));
        return variables;
    }

    private Map<String, Object> buildFollowUpPromptVariables(String direction, String level, String style,
                                                             String question, String answer, int followUpCount,
                                                             String ragContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("direction", AiPromptSections.text(direction));
        variables.put("level", AiPromptSections.text(level));
        variables.put("style", AiPromptSections.text(style));
        variables.put("followUpCount", followUpCount);
        variables.put("question", AiPromptSections.text(question));
        variables.put("answer", AiPromptSections.text(answer));
        variables.put("ragSection", AiPromptSections.ragSection(ragContext));
        return variables;
    }

    private AnswerEvaluationResult parseEvaluationResult(String response, String answer, int followUpCount) {
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null || AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure("mock_interview_evaluate_graph", InterviewPromptSpecs.EVALUATE_ANSWER, "invalid_json");
            return AnswerEvaluationResult.fallbackResult(answer, followUpCount);
        }

        var validation = InterviewStructuredOutputSpecs.EVALUATE_ANSWER.validateObject(json);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure("mock_interview_evaluate_graph", InterviewPromptSpecs.EVALUATE_ANSWER, validation.reason());
            return AnswerEvaluationResult.fallbackResult(answer, followUpCount);
        }

        AnswerEvaluationResult result = new AnswerEvaluationResult();
        result.setScore(AiJsonResponseParser.getInt(json, "score", 5));

        JSONObject feedbackJson = json.getJSONObject("feedback");
        if (feedbackJson != null) {
            AnswerEvaluationResult.Feedback feedback = new AnswerEvaluationResult.Feedback();
            feedback.setStrengths(defaultList(feedbackJson.getBeanList("strengths", String.class)));
            feedback.setImprovements(defaultList(feedbackJson.getBeanList("improvements", String.class)));
            result.setFeedback(feedback);
        } else {
            result.setFeedback(defaultFeedback());
        }

        result.setNextAction(AiJsonResponseParser.getString(json, "nextAction", "nextQuestion"));
        result.setFollowUpQuestion(json.getStr("followUpQuestion"));
        result.setReferencePoints(defaultList(json.getBeanList("referencePoints", String.class)));
        result.setFallback(false);
        return result;
    }

    private InterviewSummaryResult parseSummaryResult(String response, int totalScore) {
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null || AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure("mock_interview_summary_graph", InterviewPromptSpecs.GENERATE_SUMMARY, "invalid_json");
            return InterviewSummaryResult.fallbackResult(totalScore);
        }

        var validation = InterviewStructuredOutputSpecs.GENERATE_SUMMARY.validateObject(json);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure("mock_interview_summary_graph", InterviewPromptSpecs.GENERATE_SUMMARY, validation.reason());
            return InterviewSummaryResult.fallbackResult(totalScore);
        }

        InterviewSummaryResult result = new InterviewSummaryResult();
        result.setSummary(AiJsonResponseParser.getString(json, "summary", "感谢您完成本次面试"));
        result.setOverallLevel(AiJsonResponseParser.getString(json, "overallLevel", "良好"));
        result.setSuggestions(defaultList(json.getBeanList("suggestions", String.class)));
        result.setFallback(false);
        return result;
    }

    private List<GeneratedQuestion> parseGeneratedQuestions(String response) {
        List<GeneratedQuestion> questions = new ArrayList<>();

        try {
            String content = unwrapContent(response);
            if (content == null) {
                return questions;
            }

            Object parsed = JSONUtil.parse(content);
            JSONArray array;
            if (parsed instanceof JSONArray jsonArray) {
                array = jsonArray;
            } else if (parsed instanceof JSONObject jsonObject && jsonObject.containsKey("questions")) {
                array = jsonObject.getJSONArray("questions");
            } else {
                log.warn("AI 生成面试题返回格式不符合预期: {}", response);
                aiMetricsRecorder.recordStructuredParseFailure("mock_interview_generate_questions_graph", InterviewPromptSpecs.GENERATE_QUESTIONS, "unexpected_payload");
                return questions;
            }

            if (array == null) {
                return questions;
            }

            var validation = InterviewStructuredOutputSpecs.GENERATE_QUESTIONS.validateArray(array);
            if (!validation.valid()) {
                aiMetricsRecorder.recordStructuredParseFailure("mock_interview_generate_questions_graph", InterviewPromptSpecs.GENERATE_QUESTIONS, validation.reason());
                return questions;
            }

            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                GeneratedQuestion question = new GeneratedQuestion();
                question.setQuestion(item.getStr("question"));
                question.setAnswer(item.getStr("answer"));
                question.setKnowledgePoints(item.getStr("knowledgePoints"));
                questions.add(question);
            }
        } catch (Exception e) {
            log.error("解析 AI 生成面试题失败: {}", response, e);
            aiMetricsRecorder.recordStructuredParseFailure("mock_interview_generate_questions_graph", InterviewPromptSpecs.GENERATE_QUESTIONS, "parse_exception");
        }

        return questions;
    }

    private String parseFollowUpQuestion(String response, String answer) {
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null || AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure("mock_interview_generate_follow_up_graph", InterviewPromptSpecs.GENERATE_FOLLOW_UP, "invalid_json");
            return fallbackFollowUpQuestion(answer);
        }

        var validation = InterviewStructuredOutputSpecs.GENERATE_FOLLOW_UP.validateObject(json);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure("mock_interview_generate_follow_up_graph", InterviewPromptSpecs.GENERATE_FOLLOW_UP, validation.reason());
            return fallbackFollowUpQuestion(answer);
        }

        String followUpQuestion = AiJsonResponseParser.getString(json, "followUpQuestion", "");
        return StringUtils.hasText(followUpQuestion)
                ? followUpQuestion.trim()
                : fallbackFollowUpQuestion(answer);
    }

    private String unwrapContent(String response) {
        if (response == null) {
            return null;
        }

        try {
            JSONObject wrapper = JSONUtil.parseObj(response);
            if (wrapper.containsKey("output")) {
                return wrapper.getStr("output");
            }
            return wrapper.toString();
        } catch (Exception ignored) {
            return response;
        }
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? new ArrayList<>() : values;
    }

    private AnswerEvaluationResult.Feedback defaultFeedback() {
        AnswerEvaluationResult.Feedback feedback = new AnswerEvaluationResult.Feedback();
        feedback.setStrengths(new ArrayList<>());
        feedback.setImprovements(new ArrayList<>());
        return feedback;
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
