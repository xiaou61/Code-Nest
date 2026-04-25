package com.xiaou.ai.eval.scenario;

import com.xiaou.ai.eval.AiEvalCase;
import com.xiaou.ai.scene.sql.SqlOptimizeSceneSupport;

/**
 * SQL 收益对比评测场景。
 *
 * @author xiaou
 */
public class SqlCompareEvalScenario extends AbstractAiEvalScenario {

    @Override
    public String scenarioKey() {
        return "sql_optimize_compare";
    }

    @Override
    public Object execute(AiEvalCase testCase) {
        SqlOptimizeSceneSupport support = new SqlOptimizeSceneSupport(
                buildExecutionSupport(testCase),
                buildMetricsRecorder()
        );
        return support.compare(
                stringInput(testCase, "beforeSql"),
                stringInput(testCase, "afterSql"),
                stringInput(testCase, "beforeExplain"),
                stringInput(testCase, "afterExplain"),
                optionalStringInput(testCase, "explainFormat", "TABLE")
        );
    }
}
