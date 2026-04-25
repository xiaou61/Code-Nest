package com.xiaou.ai.structured;

import com.xiaou.ai.structured.community.CommunityStructuredOutputSpecs;
import com.xiaou.ai.structured.interview.InterviewStructuredOutputSpecs;
import com.xiaou.ai.structured.jobbattle.JobBattleStructuredOutputSpecs;
import com.xiaou.ai.structured.sql.SqlStructuredOutputSpecs;

import java.util.List;

/**
 * AI 结构化输出契约清单。
 *
 * @author xiaou
 */
public final class AiStructuredOutputCatalog {

    private static final List<AiStructuredOutputSpec> SPECS = List.of(
            CommunityStructuredOutputSpecs.POST_SUMMARY,
            InterviewStructuredOutputSpecs.EVALUATE_ANSWER,
            InterviewStructuredOutputSpecs.GENERATE_SUMMARY,
            InterviewStructuredOutputSpecs.GENERATE_QUESTIONS,
            InterviewStructuredOutputSpecs.GENERATE_FOLLOW_UP,
            JobBattleStructuredOutputSpecs.JD_PARSE,
            JobBattleStructuredOutputSpecs.RESUME_MATCH,
            JobBattleStructuredOutputSpecs.PLAN_GENERATE,
            JobBattleStructuredOutputSpecs.INTERVIEW_REVIEW,
            SqlStructuredOutputSpecs.ANALYZE,
            SqlStructuredOutputSpecs.ANALYZE_V2,
            SqlStructuredOutputSpecs.REWRITE,
            SqlStructuredOutputSpecs.COMPARE
    );

    private AiStructuredOutputCatalog() {
    }

    public static List<AiStructuredOutputSpec> all() {
        return SPECS;
    }
}
