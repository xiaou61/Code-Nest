package com.xiaou.ai.rag.jobbattle;

import com.xiaou.ai.prompt.jobbattle.JobBattleRagQuerySpecs;
import com.xiaou.ai.rag.AiRagRetrievalProfile;

import java.util.Map;

/**
 * 求职作战台检索画像。
 *
 * @author xiaou
 */
public final class JobBattleRagRetrievalProfiles {

    public static final AiRagRetrievalProfile PARSE_JD = AiRagRetrievalProfile.of(
            JobBattleRagQuerySpecs.PARSE_JD,
            "job_battle",
            3,
            Map.of("domain", "job_battle", "task", "parse_jd")
    );

    public static final AiRagRetrievalProfile MATCH_RESUME = AiRagRetrievalProfile.of(
            JobBattleRagQuerySpecs.MATCH_RESUME,
            "job_battle",
            3,
            Map.of("domain", "job_battle", "task", "match_resume")
    );

    public static final AiRagRetrievalProfile GENERATE_PLAN = AiRagRetrievalProfile.of(
            JobBattleRagQuerySpecs.GENERATE_PLAN,
            "job_battle",
            3,
            Map.of("domain", "job_battle", "task", "generate_plan")
    );

    public static final AiRagRetrievalProfile REVIEW_INTERVIEW = AiRagRetrievalProfile.of(
            JobBattleRagQuerySpecs.REVIEW_INTERVIEW,
            "job_battle",
            3,
            Map.of("domain", "job_battle", "task", "review_interview")
    );

    public static final AiRagRetrievalProfile ANALYZE_TARGET = AiRagRetrievalProfile.of(
            JobBattleRagQuerySpecs.ANALYZE_TARGET,
            "job_battle",
            3,
            Map.of("domain", "job_battle", "task", "analyze_target")
    );

    private JobBattleRagRetrievalProfiles() {
    }
}
