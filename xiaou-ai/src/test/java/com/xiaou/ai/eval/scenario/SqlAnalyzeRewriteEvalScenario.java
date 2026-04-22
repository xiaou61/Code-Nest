package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.graph.sql.SqlOptimizeGraphRunner;
import com.xiaou.ai.scene.sql.SqlAnalyzeMode;
import com.xiaou.ai.scene.sql.SqlOptimizeSceneSupport;

/**
 * SQL 分析 + 重写组合评测场景。
 *
 * @author xiaou
 */
public class SqlAnalyzeRewriteEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "sql_optimize_analyze_rewrite";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        SqlOptimizeSceneSupport sceneSupport = new SqlOptimizeSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        SqlOptimizeGraphRunner graphRunner = new SqlOptimizeGraphRunner(
                sceneSupport,
                buildUnavailableLlamaIndexClient()
        );
        return graphRunner.runAnalyzeAndRewrite(
                SqlAnalyzeMode.valueOf(stringInput(testCase, "mode")),
                stringInput(testCase, "sql"),
                stringInput(testCase, "explainResult"),
                stringInput(testCase, "explainFormat"),
                stringInput(testCase, "tableStructures"),
                stringInput(testCase, "mysqlVersion")
        );
    }
}
