package com.xiaou.ai.prompt.sql;

import com.xiaou.ai.prompt.AiRagQuerySpec;

/**
 * SQL 优化 RAG 检索查询模板。
 *
 * @author xiaou
 */
public final class SqlOptimizeRagQuerySpecs {

    public static final AiRagQuerySpec ANALYZE = AiRagQuerySpec.of(
            "sql_optimize.retrieve.analyze",
            "v1",
            """
                    SQL 优化问题：
                    SQL={{sql}}
                    EXPLAIN={{explainResult}}
                    """
    );

    public static final AiRagQuerySpec REWRITE = AiRagQuerySpec.of(
            "sql_optimize.retrieve.rewrite",
            "v1",
            """
                    SQL 重写参考：
                    SQL={{originalSql}}
                    诊断={{diagnoseJson}}
                    """
    );

    private SqlOptimizeRagQuerySpecs() {
    }
}
