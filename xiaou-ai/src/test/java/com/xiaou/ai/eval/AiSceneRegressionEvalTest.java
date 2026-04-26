package com.xiaou.ai.eval;

import com.xiaou.ai.eval.scenario.CommunitySummaryEvalScenario;
import com.xiaou.ai.eval.scenario.InterviewEvaluateEvalScenario;
import com.xiaou.ai.eval.scenario.InterviewGenerateFollowUpEvalScenario;
import com.xiaou.ai.eval.scenario.InterviewGenerateQuestionsEvalScenario;
import com.xiaou.ai.eval.scenario.InterviewSummaryEvalScenario;
import com.xiaou.ai.eval.scenario.JobBattleInterviewReviewEvalScenario;
import com.xiaou.ai.eval.scenario.JobBattleJdParseEvalScenario;
import com.xiaou.ai.eval.scenario.JobBattlePlanEvalScenario;
import com.xiaou.ai.eval.scenario.JobBattleResumeMatchEvalScenario;
import com.xiaou.ai.eval.scenario.JobBattleTargetAnalyzeEvalScenario;
import com.xiaou.ai.eval.scenario.SqlAnalyzeEvalScenario;
import com.xiaou.ai.eval.scenario.SqlAnalyzeRewriteEvalScenario;
import com.xiaou.ai.eval.scenario.SqlCompareEvalScenario;
import com.xiaou.ai.eval.scenario.SqlRewriteEvalScenario;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * AI 场景回归评测。
 *
 * <p>通过资源文件驱动固定黄金样例，保证 Prompt、结构化输出和解析逻辑调整后可以快速回归。</p>
 *
 * @author xiaou
 */
class AiSceneRegressionEvalTest {

    private static final Map<String, AiEvalScenario> SCENARIOS = Map.ofEntries(
            Map.entry("community_summary", new CommunitySummaryEvalScenario()),
            Map.entry("mock_interview_evaluate", new InterviewEvaluateEvalScenario()),
            Map.entry("mock_interview_generate_questions", new InterviewGenerateQuestionsEvalScenario()),
            Map.entry("mock_interview_generate_follow_up", new InterviewGenerateFollowUpEvalScenario()),
            Map.entry("mock_interview_summary", new InterviewSummaryEvalScenario()),
            Map.entry("job_battle_jd_parse", new JobBattleJdParseEvalScenario()),
            Map.entry("job_battle_resume_match", new JobBattleResumeMatchEvalScenario()),
            Map.entry("job_battle_plan_generate", new JobBattlePlanEvalScenario()),
            Map.entry("job_battle_interview_review", new JobBattleInterviewReviewEvalScenario()),
            Map.entry("job_battle_target_analyze", new JobBattleTargetAnalyzeEvalScenario()),
            Map.entry("sql_optimize_analyze", new SqlAnalyzeEvalScenario()),
            Map.entry("sql_optimize_analyze_rewrite", new SqlAnalyzeRewriteEvalScenario()),
            Map.entry("sql_optimize_rewrite", new SqlRewriteEvalScenario()),
            Map.entry("sql_optimize_compare", new SqlCompareEvalScenario())
    );

    static Stream<AiEvalCase> sceneRegressionCases() {
        List<AiEvalCase> cases = AiEvalFixtureLoader.loadSceneRegressionCases();
        return cases.stream();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("sceneRegressionCases")
    void shouldPassSceneRegressionCases(AiEvalCase testCase) {
        AiEvalScenario scenario = SCENARIOS.get(testCase.getScenario());
        assertNotNull(scenario, "未注册的 AI 评测场景: " + testCase.getScenario());

        Object actualResult = scenario.execute(testCase);
        AiEvalAssertions.assertMatches(testCase, actualResult);
    }
}
