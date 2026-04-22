package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.eval.InterviewFollowUpEvalResult;
import com.xiaou.ai.scene.interview.InterviewSceneSupport;

/**
 * 模拟面试追问生成评测场景。
 *
 * @author xiaou
 */
public class InterviewGenerateFollowUpEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "mock_interview_generate_follow_up";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        String answer = stringInput(testCase, "answer");
        InterviewSceneSupport support = new InterviewSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        String followUpQuestion = support.generateFollowUpQuestion(
                stringInput(testCase, "direction"),
                stringInput(testCase, "level"),
                stringInput(testCase, "style"),
                stringInput(testCase, "question"),
                answer,
                integerInput(testCase, "followUpCount"),
                optionalStringInput(testCase, "ragContext", "")
        );
        return new InterviewFollowUpEvalResult()
                .setFollowUpQuestion(followUpQuestion)
                .setFallback(isFallbackQuestion(answer, followUpQuestion));
    }

    private boolean isFallbackQuestion(String answer, String followUpQuestion) {
        return fallbackFollowUpQuestion(answer).equals(followUpQuestion);
    }

    private String fallbackFollowUpQuestion(String answer) {
        if (answer == null || answer.trim().length() < 50) {
            return "你的回答比较简短，能否展开说说具体的实现方式或应用场景？";
        }
        if (answer.trim().length() < 150) {
            return "你提到了一些关键点，能否举一个实际项目中的例子来说明？";
        }
        return "你的回答很详细，那么如果遇到性能问题或边界情况，你会怎么处理？";
    }
}
