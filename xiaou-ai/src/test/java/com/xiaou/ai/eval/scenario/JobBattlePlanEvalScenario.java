package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.scene.jobbattle.JobBattleSceneSupport;

/**
 * 求职作战台计划生成评测场景。
 *
 * @author xiaou
 */
public class JobBattlePlanEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "job_battle_plan_generate";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        JobBattleSceneSupport support = new JobBattleSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        return support.generatePlan(
                stringInput(testCase, "gapsJson"),
                integerInput(testCase, "targetDays"),
                integerInput(testCase, "weeklyHours"),
                stringInput(testCase, "preferredLearningMode"),
                stringInput(testCase, "nextInterviewDate"),
                optionalStringInput(testCase, "ragContext", "")
        );
    }
}
