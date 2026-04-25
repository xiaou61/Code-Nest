package com.xiaou.ai.structured.interview;

import com.xiaou.ai.prompt.interview.InterviewPromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputSpec;

import java.util.Set;

/**
 * 模拟面试结构化输出契约。
 *
 * @author xiaou
 */
public final class InterviewStructuredOutputSpecs {

    public static final AiStructuredOutputSpec EVALUATE_ANSWER = AiStructuredOutputSpec.object(
            InterviewPromptSpecs.EVALUATE_ANSWER,
            validator -> validator
                    .requireIntRange("score", 0, 10)
                    .requireObject("feedback", nested -> nested
                            .requireStringArray("strengths")
                            .requireStringArray("improvements"))
                    .requireStringInSet("nextAction", Set.of("followUp", "nextQuestion"))
                    .requireString("followUpQuestion")
                    .requireStringArray("referencePoints")
    );

    public static final AiStructuredOutputSpec GENERATE_SUMMARY = AiStructuredOutputSpec.object(
            InterviewPromptSpecs.GENERATE_SUMMARY,
            validator -> validator
                    .requireString("summary")
                    .requireStringInSet("overallLevel", Set.of("优秀", "良好", "一般", "需加强"))
                    .requireStringArray("suggestions")
    );

    public static final AiStructuredOutputSpec GENERATE_QUESTIONS = AiStructuredOutputSpec.array(
            InterviewPromptSpecs.GENERATE_QUESTIONS,
            validator -> validator.requireObjectItems(item -> item
                    .requireString("question")
                    .requireString("answer")
                    .requireString("knowledgePoints"))
    );

    public static final AiStructuredOutputSpec GENERATE_FOLLOW_UP = AiStructuredOutputSpec.object(
            InterviewPromptSpecs.GENERATE_FOLLOW_UP,
            validator -> validator.requireString("followUpQuestion")
    );

    private InterviewStructuredOutputSpecs() {
    }
}
