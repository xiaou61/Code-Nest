package com.xiaou.ai.regression;

import com.xiaou.ai.client.AiModelFactory;
import com.xiaou.ai.dto.community.PostSummaryResult;
import com.xiaou.ai.dto.interview.AnswerEvaluationResult;
import com.xiaou.ai.dto.interview.GeneratedQuestion;
import com.xiaou.ai.dto.interview.InterviewSummaryResult;
import com.xiaou.ai.dto.jobbattle.JobBattleInterviewReviewResult;
import com.xiaou.ai.dto.jobbattle.JobBattleJdParseResult;
import com.xiaou.ai.dto.jobbattle.JobBattlePlanResult;
import com.xiaou.ai.dto.jobbattle.JobBattleResumeMatchResult;
import com.xiaou.ai.dto.jobbattle.JobBattleTargetAnalysisResult;
import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlAnalyzeRewriteResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import com.xiaou.ai.graph.interview.InterviewGraphRunner;
import com.xiaou.ai.graph.jobbattle.JobBattleGraphRunner;
import com.xiaou.ai.graph.sql.SqlOptimizeGraphRunner;
import com.xiaou.ai.metrics.AiMetricsRecorder;
import com.xiaou.ai.metrics.AiRuntimeMetricsCollector;
import com.xiaou.ai.prompt.AiPromptSpec;
import com.xiaou.ai.rag.LlamaIndexClient;
import com.xiaou.ai.scene.interview.InterviewSceneSupport;
import com.xiaou.ai.scene.jobbattle.JobBattleSceneSupport;
import com.xiaou.ai.scene.sql.SqlAnalyzeMode;
import com.xiaou.ai.scene.sql.SqlOptimizeSceneSupport;
import com.xiaou.ai.service.impl.AiCommunityServiceImpl;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.common.config.AiProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * AI 黄金样例回归服务实现。
 *
 * <p>该服务复用现有固定样例，在后台可视化场景下提供手动回归能力。</p>
 *
 * @author xiaou
 */
@Slf4j
@Service
public class AiRegressionServiceImpl implements AiRegressionService {

    @Override
    public List<AiRegressionCaseCatalogItem> listCases() {
        ArrayList<AiRegressionCaseCatalogItem> items = new ArrayList<>();
        for (AiRegressionCase testCase : AiRegressionFixtureLoader.loadCases()) {
            items.add(new AiRegressionCaseCatalogItem()
                    .setCaseId(testCase.getId())
                    .setScenario(testCase.getScenario())
                    .setDescription(testCase.getDescription())
                    .setExpectedFallback(testCase.getExpect() == null ? null : testCase.getExpect().getFallback())
                    .setInputKeys(sortedInputKeys(testCase.getInput())));
        }
        items.sort(Comparator.comparing(AiRegressionCaseCatalogItem::getScenario)
                .thenComparing(AiRegressionCaseCatalogItem::getCaseId));
        return items;
    }

    @Override
    public AiRegressionRunSummary run(String scenario, String caseId) {
        String normalizedScenario = normalizeOptionalText(scenario);
        String normalizedCaseId = normalizeOptionalText(caseId);
        List<AiRegressionCase> selectedCases = AiRegressionFixtureLoader.loadCases().stream()
                .filter(item -> !StringUtils.hasText(normalizedScenario) || normalizedScenario.equals(item.getScenario()))
                .filter(item -> !StringUtils.hasText(normalizedCaseId) || normalizedCaseId.equals(item.getId()))
                .sorted(Comparator.comparing(AiRegressionCase::getScenario).thenComparing(AiRegressionCase::getId))
                .toList();

        if (selectedCases.isEmpty()) {
            throw new IllegalArgumentException("未找到匹配的 AI 回归用例");
        }

        long startNanos = System.nanoTime();
        ArrayList<AiRegressionCaseResult> caseResults = new ArrayList<>();
        int passedCount = 0;
        for (AiRegressionCase testCase : selectedCases) {
            AiRegressionCaseResult caseResult = runSingleCase(testCase);
            caseResults.add(caseResult);
            if (caseResult.isPassed()) {
                passedCount++;
            }
        }

        return new AiRegressionRunSummary()
                .setScenario(normalizedScenario)
                .setCaseId(normalizedCaseId)
                .setTotalCount(caseResults.size())
                .setPassedCount(passedCount)
                .setFailedCount(caseResults.size() - passedCount)
                .setDurationMs((System.nanoTime() - startNanos) / 1_000_000L)
                .setExecutedAt(System.currentTimeMillis())
                .setCaseResults(caseResults);
    }

    private AiRegressionCaseResult runSingleCase(AiRegressionCase testCase) {
        long startNanos = System.nanoTime();
        AiRegressionCaseResult caseResult = new AiRegressionCaseResult()
                .setCaseId(testCase.getId())
                .setScenario(testCase.getScenario())
                .setDescription(testCase.getDescription())
                .setExpectedFallback(testCase.getExpect() == null ? null : testCase.getExpect().getFallback());

        try {
            Object actualResult = executeScenario(testCase);
            List<String> failures = AiRegressionAssertionSupport.validate(testCase, actualResult);
            caseResult.setPassed(failures.isEmpty());
            caseResult.setFailureReasons(failures);
            caseResult.setActualFallback(AiRegressionAssertionSupport.readFallback(actualResult));
            return caseResult;
        } catch (Exception e) {
            log.warn("执行 AI 回归用例失败: caseId={}, scenario={}, message={}",
                    testCase.getId(), testCase.getScenario(), e.getMessage());
            caseResult.setPassed(false);
            caseResult.setFailureReasons(List.of(compactExceptionMessage(e)));
            return caseResult;
        } finally {
            caseResult.setDurationMs((System.nanoTime() - startNanos) / 1_000_000L);
        }
    }

    private Object executeScenario(AiRegressionCase testCase) {
        String scenario = normalizeRequiredText(testCase.getScenario(), "scenario");
        return switch (scenario) {
            case "community_summary" -> runCommunitySummaryCase(testCase);
            case "mock_interview_evaluate" -> runInterviewEvaluateCase(testCase);
            case "mock_interview_generate_questions" -> runInterviewGenerateQuestionsCase(testCase);
            case "mock_interview_generate_follow_up" -> runInterviewGenerateFollowUpCase(testCase);
            case "mock_interview_summary" -> runInterviewSummaryCase(testCase);
            case "job_battle_jd_parse" -> runJobBattleJdParseCase(testCase);
            case "job_battle_resume_match" -> runJobBattleResumeMatchCase(testCase);
            case "job_battle_plan_generate" -> runJobBattlePlanCase(testCase);
            case "job_battle_interview_review" -> runJobBattleInterviewReviewCase(testCase);
            case "job_battle_target_analyze" -> runJobBattleTargetAnalyzeCase(testCase);
            case "sql_optimize_analyze" -> runSqlAnalyzeCase(testCase);
            case "sql_optimize_analyze_rewrite" -> runSqlAnalyzeRewriteCase(testCase);
            case "sql_optimize_rewrite" -> runSqlRewriteCase(testCase);
            case "sql_optimize_compare" -> runSqlCompareCase(testCase);
            default -> throw new IllegalArgumentException("未注册的 AI 回归场景: " + scenario);
        };
    }

    private PostSummaryResult runCommunitySummaryCase(AiRegressionCase testCase) {
        AiMetricsRecorder metricsRecorder = buildMetricsRecorder();
        AiCommunityServiceImpl service = new AiCommunityServiceImpl(
                buildExecutionSupport(testCase, metricsRecorder),
                metricsRecorder
        );
        return service.generatePostSummary(
                stringInput(testCase, "title"),
                stringInput(testCase, "content")
        );
    }

    private AnswerEvaluationResult runInterviewEvaluateCase(AiRegressionCase testCase) {
        InterviewGraphRunner graphRunner = buildInterviewGraphRunner(testCase);
        return graphRunner.runEvaluateAnswer(
                stringInput(testCase, "direction"),
                stringInput(testCase, "level"),
                stringInput(testCase, "style"),
                stringInput(testCase, "question"),
                stringInput(testCase, "answer"),
                integerInput(testCase, "followUpCount")
        );
    }

    private List<GeneratedQuestion> runInterviewGenerateQuestionsCase(AiRegressionCase testCase) {
        InterviewGraphRunner graphRunner = buildInterviewGraphRunner(testCase);
        return graphRunner.runGenerateQuestions(
                stringInput(testCase, "direction"),
                stringInput(testCase, "level"),
                integerInput(testCase, "count")
        );
    }

    private String runInterviewGenerateFollowUpCase(AiRegressionCase testCase) {
        InterviewGraphRunner graphRunner = buildInterviewGraphRunner(testCase);
        return graphRunner.runGenerateFollowUp(
                stringInput(testCase, "direction"),
                stringInput(testCase, "level"),
                stringInput(testCase, "style"),
                stringInput(testCase, "question"),
                stringInput(testCase, "answer"),
                integerInput(testCase, "followUpCount")
        );
    }

    private InterviewSummaryResult runInterviewSummaryCase(AiRegressionCase testCase) {
        InterviewGraphRunner graphRunner = buildInterviewGraphRunner(testCase);
        return graphRunner.runGenerateSummary(
                stringInput(testCase, "direction"),
                stringInput(testCase, "level"),
                integerInput(testCase, "questionCount"),
                integerInput(testCase, "answeredCount"),
                integerInput(testCase, "skippedCount"),
                integerInput(testCase, "totalScore"),
                stringInput(testCase, "qaListJson")
        );
    }

    private JobBattleJdParseResult runJobBattleJdParseCase(AiRegressionCase testCase) {
        JobBattleGraphRunner graphRunner = buildJobBattleGraphRunner(testCase);
        return graphRunner.runParseJd(
                stringInput(testCase, "jdText"),
                optionalStringInput(testCase, "targetRole", ""),
                optionalStringInput(testCase, "targetLevel", ""),
                optionalStringInput(testCase, "city", "")
        );
    }

    private JobBattleResumeMatchResult runJobBattleResumeMatchCase(AiRegressionCase testCase) {
        JobBattleGraphRunner graphRunner = buildJobBattleGraphRunner(testCase);
        return graphRunner.runMatchResume(
                stringInput(testCase, "parsedJdJson"),
                stringInput(testCase, "resumeText"),
                optionalStringInput(testCase, "projectHighlights", ""),
                optionalStringInput(testCase, "targetCompanyType", "")
        );
    }

    private JobBattlePlanResult runJobBattlePlanCase(AiRegressionCase testCase) {
        JobBattleGraphRunner graphRunner = buildJobBattleGraphRunner(testCase);
        return graphRunner.runGeneratePlan(
                stringInput(testCase, "gapsJson"),
                integerInput(testCase, "targetDays"),
                integerInput(testCase, "weeklyHours"),
                optionalStringInput(testCase, "preferredLearningMode", ""),
                optionalStringInput(testCase, "nextInterviewDate", "")
        );
    }

    private JobBattleInterviewReviewResult runJobBattleInterviewReviewCase(AiRegressionCase testCase) {
        JobBattleGraphRunner graphRunner = buildJobBattleGraphRunner(testCase);
        return graphRunner.runReviewInterview(
                stringInput(testCase, "interviewNotes"),
                stringInput(testCase, "qaTranscriptJson"),
                optionalStringInput(testCase, "interviewResult", ""),
                optionalStringInput(testCase, "targetRole", ""),
                optionalStringInput(testCase, "nextInterviewDate", "")
        );
    }

    private JobBattleTargetAnalysisResult runJobBattleTargetAnalyzeCase(AiRegressionCase testCase) {
        JobBattleGraphRunner graphRunner = buildJobBattleGraphRunner(testCase);
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

    private SqlAnalyzeResult runSqlAnalyzeCase(AiRegressionCase testCase) {
        SqlOptimizeGraphRunner graphRunner = buildSqlGraphRunner(testCase);
        return graphRunner.runAnalyze(
                parseSqlAnalyzeMode(testCase, "mode"),
                stringInput(testCase, "sql"),
                stringInput(testCase, "explainResult"),
                optionalStringInput(testCase, "explainFormat", "TABLE"),
                optionalStringInput(testCase, "tableStructures", ""),
                optionalStringInput(testCase, "mysqlVersion", "8.0")
        );
    }

    private SqlAnalyzeRewriteResult runSqlAnalyzeRewriteCase(AiRegressionCase testCase) {
        SqlOptimizeGraphRunner graphRunner = buildSqlGraphRunner(testCase);
        return graphRunner.runAnalyzeAndRewrite(
                parseSqlAnalyzeMode(testCase, "mode"),
                stringInput(testCase, "sql"),
                stringInput(testCase, "explainResult"),
                optionalStringInput(testCase, "explainFormat", "TABLE"),
                optionalStringInput(testCase, "tableStructures", ""),
                optionalStringInput(testCase, "mysqlVersion", "8.0")
        );
    }

    private SqlRewriteResult runSqlRewriteCase(AiRegressionCase testCase) {
        SqlOptimizeGraphRunner graphRunner = buildSqlGraphRunner(testCase);
        return graphRunner.runRewrite(
                stringInput(testCase, "originalSql"),
                stringInput(testCase, "diagnoseJson"),
                optionalStringInput(testCase, "tableStructures", ""),
                optionalStringInput(testCase, "mysqlVersion", "8.0")
        );
    }

    private SqlCompareResult runSqlCompareCase(AiRegressionCase testCase) {
        SqlOptimizeGraphRunner graphRunner = buildSqlGraphRunner(testCase);
        return graphRunner.runCompare(
                stringInput(testCase, "beforeSql"),
                stringInput(testCase, "afterSql"),
                stringInput(testCase, "beforeExplain"),
                stringInput(testCase, "afterExplain"),
                optionalStringInput(testCase, "explainFormat", "TABLE")
        );
    }

    private InterviewGraphRunner buildInterviewGraphRunner(AiRegressionCase testCase) {
        AiMetricsRecorder metricsRecorder = buildMetricsRecorder();
        InterviewSceneSupport sceneSupport = new InterviewSceneSupport(
                buildExecutionSupport(testCase, metricsRecorder),
                metricsRecorder
        );
        return new InterviewGraphRunner(sceneSupport, buildUnavailableLlamaIndexClient());
    }

    private JobBattleGraphRunner buildJobBattleGraphRunner(AiRegressionCase testCase) {
        AiMetricsRecorder metricsRecorder = buildMetricsRecorder();
        JobBattleSceneSupport sceneSupport = new JobBattleSceneSupport(
                buildExecutionSupport(testCase, metricsRecorder),
                metricsRecorder
        );
        return new JobBattleGraphRunner(sceneSupport, buildUnavailableLlamaIndexClient());
    }

    private SqlOptimizeGraphRunner buildSqlGraphRunner(AiRegressionCase testCase) {
        AiMetricsRecorder metricsRecorder = buildMetricsRecorder();
        SqlOptimizeSceneSupport sceneSupport = new SqlOptimizeSceneSupport(
                buildExecutionSupport(testCase, metricsRecorder),
                metricsRecorder
        );
        return new SqlOptimizeGraphRunner(sceneSupport, buildUnavailableLlamaIndexClient());
    }

    private AiExecutionSupport buildExecutionSupport(AiRegressionCase testCase, AiMetricsRecorder metricsRecorder) {
        return new AiRegressionExecutionSupport(testCase, metricsRecorder);
    }

    private AiMetricsRecorder buildMetricsRecorder() {
        return new AiMetricsRecorder(
                new SimpleMeterRegistry(),
                new AiRuntimeMetricsCollector(),
                new AiProperties()
        );
    }

    private LlamaIndexClient buildUnavailableLlamaIndexClient() {
        return new LlamaIndexClient(new AiProperties(), RestClient.builder());
    }

    private List<String> sortedInputKeys(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }
        return input.keySet().stream()
                .sorted()
                .toList();
    }

    private SqlAnalyzeMode parseSqlAnalyzeMode(AiRegressionCase testCase, String key) {
        String value = stringInput(testCase, key);
        try {
            return SqlAnalyzeMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("非法的 SQL analyze mode: " + value);
        }
    }

    private String stringInput(AiRegressionCase testCase, String key) {
        Object value = requiredInput(testCase, key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer integerInput(AiRegressionCase testCase, String key) {
        Object value = requiredInput(testCase, key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String optionalStringInput(AiRegressionCase testCase, String key, String defaultValue) {
        if (testCase.getInput() == null || !testCase.getInput().containsKey(key) || testCase.getInput().get(key) == null) {
            return defaultValue;
        }
        return String.valueOf(testCase.getInput().get(key));
    }

    private Object requiredInput(AiRegressionCase testCase, String key) {
        if (testCase.getInput() == null || !testCase.getInput().containsKey(key)) {
            throw new IllegalArgumentException("AI 回归用例缺少输入字段: " + key + "，case=" + testCase.getId());
        }
        return testCase.getInput().get(key);
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String compactExceptionMessage(Exception e) {
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + message.trim();
    }

    /**
     * AI 回归专用执行支撑。
     */
    private static final class AiRegressionExecutionSupport extends AiExecutionSupport {

        private final AiRegressionCase testCase;
        private final Deque<String> responses = new ArrayDeque<>();
        private final Deque<Boolean> fallbackSequence = new ArrayDeque<>();

        private AiRegressionExecutionSupport(AiRegressionCase testCase, AiMetricsRecorder metricsRecorder) {
            super(new AiModelFactory(new AiProperties()), metricsRecorder, new AiProperties());
            this.testCase = testCase;
            if (testCase.getResponses() != null) {
                responses.addAll(testCase.getResponses());
            }
            if (testCase.getFallbackSequence() != null) {
                fallbackSequence.addAll(testCase.getFallbackSequence());
            }
        }

        @Override
        public boolean isChatAvailable() {
            return true;
        }

        @Override
        public <T> T chatWithFallback(String sceneName,
                                      String systemPrompt,
                                      String userPrompt,
                                      Function<String, T> parser,
                                      Supplier<T> fallbackSupplier) {
            return execute(parser, fallbackSupplier);
        }

        @Override
        public <T> T chatWithFallback(String sceneName,
                                      AiPromptSpec promptSpec,
                                      Map<String, ?> variables,
                                      Function<String, T> parser,
                                      Supplier<T> fallbackSupplier) {
            return execute(parser, fallbackSupplier);
        }

        private <T> T execute(Function<String, T> parser, Supplier<T> fallbackSupplier) {
            boolean shouldFallback = fallbackSequence.isEmpty()
                    ? testCase.isUseFallback()
                    : Boolean.TRUE.equals(fallbackSequence.removeFirst());
            if (shouldFallback) {
                return fallbackSupplier.get();
            }

            String response = responses.isEmpty() ? testCase.getResponse() : responses.removeFirst();
            return parser.apply(response);
        }
    }
}
