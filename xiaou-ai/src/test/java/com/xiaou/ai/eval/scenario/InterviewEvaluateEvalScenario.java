package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.scene.interview.InterviewSceneSupport;

/**
 * 模拟面试评价评测场景。
 *
 * @author xiaou
 */
public class InterviewEvaluateEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "mock_interview_evaluate";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        InterviewSceneSupport support = new InterviewSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        return support.evaluateAnswer(
                stringInput(testCase, "direction"),
                stringInput(testCase, "level"),
                stringInput(testCase, "style"),
                stringInput(testCase, "question"),
                stringInput(testCase, "answer"),
                integerInput(testCase, "followUpCount"),
                optionalStringInput(testCase, "ragContext", "")
        );
    }
}
