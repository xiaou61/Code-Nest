package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.scene.jobbattle.JobBattleSceneSupport;

/**
 * 求职作战台 JD 解析评测场景。
 *
 * @author xiaou
 */
public class JobBattleJdParseEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "job_battle_jd_parse";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        JobBattleSceneSupport support = new JobBattleSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        return support.parseJd(
                stringInput(testCase, "jdText"),
                optionalStringInput(testCase, "targetRole", ""),
                optionalStringInput(testCase, "targetLevel", ""),
                optionalStringInput(testCase, "city", ""),
                optionalStringInput(testCase, "ragContext", "")
        );
    }
}
