package com.xiaou.ai.structured.sql;

import com.xiaou.ai.prompt.sql.SqlOptimizePromptSpecs;
import com.xiaou.ai.structured.AiStructuredObjectContract;
import com.xiaou.ai.structured.AiStructuredOutputSpec;

/**
 * SQL 结构化输出契约。
 *
 * @author xiaou
 */
public final class SqlStructuredOutputSpecs {

    public static final AiStructuredOutputSpec ANALYZE = AiStructuredOutputSpec.object(
            SqlOptimizePromptSpecs.ANALYZE,
            SqlStructuredOutputSpecs::applyAnalyzeContract
    );

    public static final AiStructuredOutputSpec ANALYZE_V2 = AiStructuredOutputSpec.object(
            SqlOptimizePromptSpecs.ANALYZE_V2,
            SqlStructuredOutputSpecs::applyAnalyzeContract
    );

    public static final AiStructuredOutputSpec REWRITE = AiStructuredOutputSpec.object(
            SqlOptimizePromptSpecs.REWRITE,
            validator -> validator
                    .requireString("optimizedSql")
                    .requireStringArray("indexDdls")
                    .requireString("rewriteReason")
                    .requireStringArray("riskWarnings")
                    .requireString("expectedImprovement")
    );

    public static final AiStructuredOutputSpec COMPARE = AiStructuredOutputSpec.object(
            SqlOptimizePromptSpecs.COMPARE,
            validator -> validator
                    .requireIntRange("improvementScore", 0, 100)
                    .requireString("deltaRows")
                    .requireString("deltaType")
                    .requireString("deltaExtra")
                    .requireString("summary")
                    .requireStringArray("attention")
    );

    private SqlStructuredOutputSpecs() {
    }

    private static void applyAnalyzeContract(AiStructuredObjectContract validator) {
        validator
                .requireIntRange("score", 0, 100)
                .requireString("optimizedSql")
                .requireStringArray("knowledgePoints")
                .requireObjectArray("problems", item -> item
                        .requireString("type")
                        .requireString("severity")
                        .requireString("description")
                        .requireString("location"))
                .requireObjectArray("explainAnalysis", item -> item
                        .requireString("table")
                        .requireString("type")
                        .requireString("typeExplain")
                        .requireString("key")
                        .requireString("keyExplain")
                        .requireInt("rows")
                        .requireString("extra")
                        .requireString("extraExplain")
                        .requireString("issue"))
                .requireObjectArray("suggestions", item -> item
                        .requireString("type")
                        .requireString("title")
                        .requireString("ddl")
                        .requireString("sql")
                        .requireString("reason")
                        .requireString("expectedImprovement"));
    }
}
