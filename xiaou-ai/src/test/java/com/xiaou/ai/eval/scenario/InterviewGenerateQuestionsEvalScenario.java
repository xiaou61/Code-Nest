package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.dto.interview.GeneratedQuestion;
import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.eval.InterviewGeneratedQuestionsEvalResult;
import com.xiaou.ai.scene.interview.InterviewSceneSupport;

import java.util.List;

/**
 * 模拟面试题生成评测场景。
 *
 * @author xiaou
 */
public class InterviewGenerateQuestionsEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "mock_interview_generate_questions";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        InterviewSceneSupport support = new InterviewSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        List<GeneratedQuestion> questions = support.generateQuestions(
                stringInput(testCase, "direction"),
                stringInput(testCase, "level"),
                integerInput(testCase, "count"),
                optionalStringInput(testCase, "ragContext", "")
        );
        List<GeneratedQuestion> safeQuestions = questions == null ? List.of() : questions;
        return new InterviewGeneratedQuestionsEvalResult()
                .setQuestions(safeQuestions)
                .setQuestionCount(safeQuestions.size())
                .setFallback(safeQuestions.isEmpty());
    }
}
