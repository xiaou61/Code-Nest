package com.xiaou.ai.graph.jobbattle;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * 求职作战台图状态。
 *
 * @author xiaou
 */
public class JobBattleState extends AgentState {

    public static final String TASK_TYPE = "taskType";
    public static final String JD_TEXT = "jdText";
    public static final String TARGET_ROLE = "targetRole";
    public static final String TARGET_LEVEL = "targetLevel";
    public static final String CITY = "city";
    public static final String PARSED_JD_JSON = "parsedJdJson";
    public static final String RESUME_TEXT = "resumeText";
    public static final String PROJECT_HIGHLIGHTS = "projectHighlights";
    public static final String TARGET_COMPANY_TYPE = "targetCompanyType";
    public static final String GAPS_JSON = "gapsJson";
    public static final String TARGET_DAYS = "targetDays";
    public static final String WEEKLY_HOURS = "weeklyHours";
    public static final String PREFERRED_LEARNING_MODE = "preferredLearningMode";
    public static final String NEXT_INTERVIEW_DATE = "nextInterviewDate";
    public static final String INTERVIEW_NOTES = "interviewNotes";
    public static final String QA_TRANSCRIPT_JSON = "qaTranscriptJson";
    public static final String INTERVIEW_RESULT = "interviewResult";
    public static final String RAG_CONTEXT = "ragContext";
    public static final String JD_PARSE_RESULT_JSON = "jdParseResultJson";
    public static final String RESUME_MATCH_RESULT_JSON = "resumeMatchResultJson";
    public static final String PLAN_RESULT_JSON = "planResultJson";
    public static final String REVIEW_RESULT_JSON = "reviewResultJson";

    public JobBattleState(Map<String, Object> data) {
        super(data);
    }

    public JobBattleTaskType taskType() {
        String raw = stringValue(TASK_TYPE);
        if (raw == null || raw.isBlank()) {
            return JobBattleTaskType.PARSE_JD;
        }
        try {
            return JobBattleTaskType.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return JobBattleTaskType.PARSE_JD;
        }
    }

    public String jdText() {
        return stringValue(JD_TEXT);
    }

    public String targetRole() {
        return stringValue(TARGET_ROLE);
    }

    public String targetLevel() {
        return stringValue(TARGET_LEVEL);
    }

    public String city() {
        return stringValue(CITY);
    }

    public String parsedJdJson() {
        return stringValue(PARSED_JD_JSON);
    }

    public String resumeText() {
        return stringValue(RESUME_TEXT);
    }

    public String projectHighlights() {
        return stringValue(PROJECT_HIGHLIGHTS);
    }

    public String targetCompanyType() {
        return stringValue(TARGET_COMPANY_TYPE);
    }

    public String gapsJson() {
        return stringValue(GAPS_JSON);
    }

    public int targetDays() {
        return intValue(TARGET_DAYS);
    }

    public int weeklyHours() {
        return intValue(WEEKLY_HOURS);
    }

    public String preferredLearningMode() {
        return stringValue(PREFERRED_LEARNING_MODE);
    }

    public String nextInterviewDate() {
        return stringValue(NEXT_INTERVIEW_DATE);
    }

    public String interviewNotes() {
        return stringValue(INTERVIEW_NOTES);
    }

    public String qaTranscriptJson() {
        return stringValue(QA_TRANSCRIPT_JSON);
    }

    public String interviewResult() {
        return stringValue(INTERVIEW_RESULT);
    }

    public String ragContext() {
        return stringValue(RAG_CONTEXT);
    }

    public String jdParseResultJson() {
        return stringValue(JD_PARSE_RESULT_JSON);
    }

    public String resumeMatchResultJson() {
        return stringValue(RESUME_MATCH_RESULT_JSON);
    }

    public String planResultJson() {
        return stringValue(PLAN_RESULT_JSON);
    }

    public String reviewResultJson() {
        return stringValue(REVIEW_RESULT_JSON);
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
