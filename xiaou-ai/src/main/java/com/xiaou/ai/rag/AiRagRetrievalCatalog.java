package com.xiaou.ai.rag;

import com.xiaou.ai.rag.interview.InterviewRagRetrievalProfiles;
import com.xiaou.ai.rag.jobbattle.JobBattleRagRetrievalProfiles;
import com.xiaou.ai.rag.sql.SqlRagRetrievalProfiles;

import java.util.List;

/**
 * RAG 检索画像清单。
 *
 * @author xiaou
 */
public final class AiRagRetrievalCatalog {

    private static final List<AiRagRetrievalProfile> PROFILES = List.of(
            InterviewRagRetrievalProfiles.GENERATE_QUESTIONS,
            InterviewRagRetrievalProfiles.GENERATE_SUMMARY,
            InterviewRagRetrievalProfiles.GENERATE_FOLLOW_UP,
            InterviewRagRetrievalProfiles.EVALUATE_ANSWER,
            JobBattleRagRetrievalProfiles.PARSE_JD,
            JobBattleRagRetrievalProfiles.MATCH_RESUME,
            JobBattleRagRetrievalProfiles.GENERATE_PLAN,
            JobBattleRagRetrievalProfiles.REVIEW_INTERVIEW,
            JobBattleRagRetrievalProfiles.ANALYZE_TARGET,
            SqlRagRetrievalProfiles.ANALYZE,
            SqlRagRetrievalProfiles.REWRITE
    );

    private AiRagRetrievalCatalog() {
    }

    public static List<AiRagRetrievalProfile> all() {
        return PROFILES;
    }
}
