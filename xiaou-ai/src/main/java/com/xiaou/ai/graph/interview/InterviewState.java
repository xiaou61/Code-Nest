package com.xiaou.ai.graph.interview;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * 模拟面试图状态。
 *
 * @author xiaou
 */
public class InterviewState extends AgentState {

    public static final String TASK_TYPE = "taskType";
    public static final String DIRECTION = "direction";
    public static final String LEVEL = "level";
    public static final String STYLE = "style";
    public static final String QUESTION = "question";
    public static final String ANSWER = "answer";
    public static final String FOLLOW_UP_COUNT = "followUpCount";
    public static final String GENERATED_COUNT = "generatedCount";
    public static final String QUESTION_COUNT = "questionCount";
    public static final String ANSWERED_COUNT = "answeredCount";
    public static final String SKIPPED_COUNT = "skippedCount";
    public static final String TOTAL_SCORE = "totalScore";
    public static final String QA_LIST_JSON = "qaListJson";
    public static final String RAG_CONTEXT = "ragContext";
    public static final String GENERATED_QUESTIONS_JSON = "generatedQuestionsJson";
    public static final String EVALUATION_RESULT_JSON = "evaluationResultJson";
    public static final String SUMMARY_RESULT_JSON = "summaryResultJson";
    public static final String FOLLOW_UP_QUESTION = "followUpQuestion";

    public InterviewState(Map<String, Object> data) {
        super(data);
    }

    public InterviewTaskType taskType() {
        String raw = stringValue(TASK_TYPE);
        if (raw == null || raw.isBlank()) {
            return InterviewTaskType.EVALUATE_ANSWER;
        }
        try {
            return InterviewTaskType.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return InterviewTaskType.EVALUATE_ANSWER;
        }
    }

    public String direction() {
        return stringValue(DIRECTION);
    }

    public String level() {
        return stringValue(LEVEL);
    }

    public String style() {
        return stringValue(STYLE);
    }

    public String question() {
        return stringValue(QUESTION);
    }

    public String answer() {
        return stringValue(ANSWER);
    }

    public int followUpCount() {
        return intValue(FOLLOW_UP_COUNT);
    }

    public int generatedCount() {
        return intValue(GENERATED_COUNT);
    }

    public int questionCount() {
        return intValue(QUESTION_COUNT);
    }

    public int answeredCount() {
        return intValue(ANSWERED_COUNT);
    }

    public int skippedCount() {
        return intValue(SKIPPED_COUNT);
    }

    public int totalScore() {
        return intValue(TOTAL_SCORE);
    }

    public String qaListJson() {
        return stringValue(QA_LIST_JSON);
    }

    public String ragContext() {
        return stringValue(RAG_CONTEXT);
    }

    public String generatedQuestionsJson() {
        return stringValue(GENERATED_QUESTIONS_JSON);
    }

    public String evaluationResultJson() {
        return stringValue(EVALUATION_RESULT_JSON);
    }

    public String summaryResultJson() {
        return stringValue(SUMMARY_RESULT_JSON);
    }

    public String followUpQuestion() {
        return stringValue(FOLLOW_UP_QUESTION);
    }

    private String stringValue(String key) {
        Object value = data().get(key);
        return value == null ? null : value.toString();
    }

    private int intValue(String key) {
        Object value = data().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
