package com.xiaou.ai.structured.jobbattle;

import com.xiaou.ai.prompt.jobbattle.JobBattlePromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputSpec;

import java.util.Set;

/**
 * 求职作战台结构化输出契约。
 *
 * @author xiaou
 */
public final class JobBattleStructuredOutputSpecs {

    public static final AiStructuredOutputSpec JD_PARSE = AiStructuredOutputSpec.object(
            JobBattlePromptSpecs.JD_PARSE,
            validator -> validator
                    .requireString("jobTitle")
                    .requireString("level")
                    .requireStringArray("mustSkills")
                    .requireStringArray("niceSkills")
                    .requireStringArray("responsibilities")
                    .requireString("seniorityYears")
                    .requireStringArray("keywords")
                    .requireStringArray("riskPoints")
                    .requireString("summary")
    );

    public static final AiStructuredOutputSpec RESUME_MATCH = AiStructuredOutputSpec.object(
            JobBattlePromptSpecs.RESUME_MATCH,
            validator -> validator
                    .requireIntRange("overallScore", 0, 100)
                    .requireObject("dimensionScores", nested -> nested
                            .requireIntRange("skillMatch", 0, 100)
                            .requireIntRange("projectDepth", 0, 100)
                            .requireIntRange("architectureAbility", 0, 100)
                            .requireIntRange("communicationClarity", 0, 100))
                    .requireStringArray("strengths")
                    .requireStringArray("missingKeywords")
                    .requireIntRange("estimatedPassRate", 0, 100)
                    .requireObjectArray("gaps", item -> item
                            .requireString("skill")
                            .requireStringInSet("priority", Set.of("P0", "P1", "P2"))
                            .requireString("why")
                            .requireString("suggestedAction"))
                    .requireObjectArray("resumeRewriteSuggestions", item -> item
                            .requireString("section")
                            .requireString("advice"))
    );

    public static final AiStructuredOutputSpec PLAN_GENERATE = AiStructuredOutputSpec.object(
            JobBattlePromptSpecs.PLAN_GENERATE,
            validator -> validator
                    .requireString("planName")
                    .requirePositiveInt("totalDays")
                    .requireStringArray("weeklyGoals")
                    .requireObjectArray("dailyTasks", item -> item
                            .requirePositiveInt("day")
                            .requireString("task")
                            .requirePositiveInt("durationMinutes")
                            .requireString("taskType")
                            .requireString("deliverable"))
                    .requireObjectArray("milestones", item -> item
                            .requirePositiveInt("day")
                            .requireString("goal"))
                    .requireStringArray("riskAndFallback")
    );

    public static final AiStructuredOutputSpec INTERVIEW_REVIEW = AiStructuredOutputSpec.object(
            JobBattlePromptSpecs.INTERVIEW_REVIEW,
            validator -> validator
                    .requireString("overallConclusion")
                    .requireStringArray("rootCauses")
                    .requireObjectArray("highImpactFixes", item -> item
                            .requireString("issue")
                            .requireString("action")
                            .requireString("deadline")
                            .requireString("metric"))
                    .requireObjectArray("questionTypeWeakness", item -> item
                            .requireString("type")
                            .requireString("suggestion"))
                    .requireStringArray("next7DayPlan")
                    .requireIntRange("confidenceScore", 0, 100)
    );

    private JobBattleStructuredOutputSpecs() {
    }
}
