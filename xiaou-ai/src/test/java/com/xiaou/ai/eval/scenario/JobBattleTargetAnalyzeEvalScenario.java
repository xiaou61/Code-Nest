package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.graph.jobbattle.JobBattleGraphRunner;
import com.xiaou.ai.scene.jobbattle.JobBattleSceneSupport;

/**
 * 求职作战台单岗位综合分析评测场景。
 *
 * @author xiaou
 */
public class JobBattleTargetAnalyzeEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "job_battle_target_analyze";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        JobBattleSceneSupport sceneSupport = new JobBattleSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        JobBattleGraphRunner graphRunner = new JobBattleGraphRunner(
                sceneSupport,
                buildUnavailableLlamaIndexClient()
        );
        return graphRunner.runAnalyzeTarget(
                stringInput(testCase, "jdText"),
                optionalStringInput(testCase, "targetRole", ""),
                optionalStringInput(testCase, "targetLevel", ""),
                optionalStringInput(testCase, "city", ""),
                stringInput(testCase, "resumeText"),
                optionalStringInput(testCase, "projectHighlights", ""),
                optionalStringInput(testCase, "targetCompanyType", "")
        );
    }
}
