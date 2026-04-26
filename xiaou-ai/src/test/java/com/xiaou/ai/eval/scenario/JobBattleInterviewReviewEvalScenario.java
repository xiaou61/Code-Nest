package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.scene.jobbattle.JobBattleSceneSupport;

/**
 * 求职作战台面试复盘评测场景。
 *
 * @author xiaou
 */
public class JobBattleInterviewReviewEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "job_battle_interview_review";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        JobBattleSceneSupport support = new JobBattleSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        return support.reviewInterview(
                stringInput(testCase, "interviewNotes"),
                optionalStringInput(testCase, "qaTranscriptJson", ""),
                stringInput(testCase, "interviewResult"),
                stringInput(testCase, "targetRole"),
                optionalStringInput(testCase, "nextInterviewDate", ""),
                optionalStringInput(testCase, "ragContext", "")
        );
    }
}
