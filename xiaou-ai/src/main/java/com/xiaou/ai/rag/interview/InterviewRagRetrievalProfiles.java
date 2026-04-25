package com.xiaou.ai.rag.interview;

import com.xiaou.ai.prompt.interview.InterviewRagQuerySpecs;
import com.xiaou.ai.rag.AiRagRetrievalProfile;

import java.util.Map;

/**
 * 模拟面试检索画像。
 *
 * @author xiaou
 */
public final class InterviewRagRetrievalProfiles {

    public static final AiRagRetrievalProfile GENERATE_QUESTIONS = AiRagRetrievalProfile.of(
            InterviewRagQuerySpecs.GENERATE_QUESTIONS,
            "mock_interview",
            3,
            Map.of("domain", "mock_interview", "task", "generate_questions")
    );

    public static final AiRagRetrievalProfile GENERATE_SUMMARY = AiRagRetrievalProfile.of(
            InterviewRagQuerySpecs.GENERATE_SUMMARY,
            "mock_interview",
            3,
            Map.of("domain", "mock_interview", "task", "generate_summary")
    );

    public static final AiRagRetrievalProfile GENERATE_FOLLOW_UP = AiRagRetrievalProfile.of(
            InterviewRagQuerySpecs.GENERATE_FOLLOW_UP,
            "mock_interview",
            3,
            Map.of("domain", "mock_interview", "task", "generate_follow_up")
    );

    public static final AiRagRetrievalProfile EVALUATE_ANSWER = AiRagRetrievalProfile.of(
            InterviewRagQuerySpecs.EVALUATE_ANSWER,
            "mock_interview",
            3,
            Map.of("domain", "mock_interview", "task", "evaluate_answer")
    );

    private InterviewRagRetrievalProfiles() {
    }
}
