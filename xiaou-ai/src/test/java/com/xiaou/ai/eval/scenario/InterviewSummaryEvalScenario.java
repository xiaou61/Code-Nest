package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.scene.interview.InterviewSceneSupport;

/**
 * 模拟面试总结评测场景。
 *
 * @author xiaou
 */
public class InterviewSummaryEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "mock_interview_summary";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        InterviewSceneSupport support = new InterviewSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        return support.generateSummary(
                stringInput(testCase, "direction"),
                stringInput(testCase, "level"),
                integerInput(testCase, "questionCount"),
                integerInput(testCase, "answeredCount"),
                integerInput(testCase, "skippedCount"),
                integerInput(testCase, "totalScore"),
                stringInput(testCase, "qaListJson"),
                optionalStringInput(testCase, "ragContext", "")
        );
    }
}
