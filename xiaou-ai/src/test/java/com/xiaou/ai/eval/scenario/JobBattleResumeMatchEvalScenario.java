package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.scene.jobbattle.JobBattleSceneSupport;

/**
 * 求职作战台简历匹配评测场景。
 *
 * @author xiaou
 */
public class JobBattleResumeMatchEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "job_battle_resume_match";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        JobBattleSceneSupport support = new JobBattleSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        return support.matchResume(
                stringInput(testCase, "parsedJdJson"),
                stringInput(testCase, "resumeText"),
                optionalStringInput(testCase, "projectHighlights", ""),
                optionalStringInput(testCase, "targetCompanyType", ""),
                optionalStringInput(testCase, "ragContext", "")
        );
    }
}
