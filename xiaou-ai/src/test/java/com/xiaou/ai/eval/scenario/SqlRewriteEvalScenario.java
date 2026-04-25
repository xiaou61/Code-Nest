package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.scene.sql.SqlOptimizeSceneSupport;

/**
 * SQL 重写评测场景。
 *
 * @author xiaou
 */
public class SqlRewriteEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "sql_optimize_rewrite";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        SqlOptimizeSceneSupport support = new SqlOptimizeSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        return support.rewrite(
                stringInput(testCase, "originalSql"),
                stringInput(testCase, "diagnoseJson"),
                stringInput(testCase, "tableStructures"),
                stringInput(testCase, "mysqlVersion"),
                optionalStringInput(testCase, "ragContext", "")
        );
    }
}
