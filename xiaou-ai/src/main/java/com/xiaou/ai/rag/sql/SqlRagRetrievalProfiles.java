package com.xiaou.ai.rag.sql;

import com.xiaou.ai.prompt.sql.SqlOptimizeRagQuerySpecs;
import com.xiaou.ai.rag.AiRagRetrievalProfile;

import java.util.Map;

/**
 * SQL 检索画像。
 *
 * @author xiaou
 */
public final class SqlRagRetrievalProfiles {

    public static final AiRagRetrievalProfile ANALYZE = AiRagRetrievalProfile.of(
            SqlOptimizeRagQuerySpecs.ANALYZE,
            "sql_optimize",
            3,
            Map.of("domain", "sql_optimize", "task", "analyze")
    );

    public static final AiRagRetrievalProfile REWRITE = AiRagRetrievalProfile.of(
            SqlOptimizeRagQuerySpecs.REWRITE,
            "sql_optimize",
            3,
            Map.of("domain", "sql_optimize", "task", "rewrite")
    );

    private SqlRagRetrievalProfiles() {
    }
}
