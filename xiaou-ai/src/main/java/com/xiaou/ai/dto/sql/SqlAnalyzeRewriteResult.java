package com.xiaou.ai.dto.sql;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * SQL 分析与重写组合结果。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class SqlAnalyzeRewriteResult {

    /**
     * SQL 分析结果
     */
    private SqlAnalyzeResult analysis;

    /**
     * SQL 重写结果
     */
    private SqlRewriteResult rewrite;

    /**
     * 是否包含降级结果
     */
    private boolean fallback;

    public static SqlAnalyzeRewriteResult fallbackResult(String originalSql, String explainResult) {
        return new SqlAnalyzeRewriteResult()
                .setAnalysis(SqlAnalyzeResult.fallbackResult(explainResult))
                .setRewrite(SqlRewriteResult.fallbackResult(originalSql))
                .setFallback(true);
    }
}
