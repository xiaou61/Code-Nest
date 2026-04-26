package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.scene.sql.SqlAnalyzeMode;
import com.xiaou.ai.scene.sql.SqlOptimizeSceneSupport;

/**
 * SQL 分析评测场景。
 *
 * @author xiaou
 */
public class SqlAnalyzeEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "sql_optimize_analyze";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        SqlOptimizeSceneSupport support = new SqlOptimizeSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        return support.analyze(
                SqlAnalyzeMode.valueOf(stringInput(testCase, "mode")),
                stringInput(testCase, "sql"),
                stringInput(testCase, "explainResult"),
                stringInput(testCase, "explainFormat"),
                stringInput(testCase, "tableStructures"),
                stringInput(testCase, "mysqlVersion"),
                optionalStringInput(testCase, "ragContext", "")
        );
    }
}
