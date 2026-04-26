package com.xiaou.ai.graph.sql;

import com.xiaou.ai.scene.sql.SqlAnalyzeMode;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * SQL 优化图状态。
 *
 * @author xiaou
 */
public class SqlOptimizeState extends AgentState {

    public static final String ANALYZE_MODE = "analyzeMode";
    public static final String SQL = "sql";
    public static final String EXPLAIN_RESULT = "explainResult";
    public static final String EXPLAIN_FORMAT = "explainFormat";
    public static final String TABLE_STRUCTURES = "tableStructures";
    public static final String MYSQL_VERSION = "mysqlVersion";
    public static final String RAG_CONTEXT = "ragContext";
    public static final String DIAGNOSE_JSON = "diagnoseJson";
    public static final String ANALYZE_RESULT_JSON = "analyzeResultJson";
    public static final String REWRITE_RESULT_JSON = "rewriteResultJson";
    public static final String COMPARE_RESULT_JSON = "compareResultJson";
    public static final String ORIGINAL_SQL = "originalSql";
    public static final String BEFORE_SQL = "beforeSql";
    public static final String AFTER_SQL = "afterSql";
    public static final String BEFORE_EXPLAIN = "beforeExplain";
    public static final String AFTER_EXPLAIN = "afterExplain";

    public SqlOptimizeState(Map<String, Object> data) {
        super(data);
    }

    public SqlAnalyzeMode analyzeMode() {
        String raw = stringValue(ANALYZE_MODE);
        if (raw == null || raw.isBlank()) {
            return SqlAnalyzeMode.STANDARD;
        }
        try {
            return SqlAnalyzeMode.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return SqlAnalyzeMode.STANDARD;
        }
    }

    public String sql() {
        return stringValue(SQL);
    }

    public String explainResult() {
        return stringValue(EXPLAIN_RESULT);
    }

    public String explainFormat() {
        return stringValue(EXPLAIN_FORMAT);
    }

    public String tableStructures() {
        return stringValue(TABLE_STRUCTURES);
    }

    public String mysqlVersion() {
        return stringValue(MYSQL_VERSION);
    }

    public String ragContext() {
        return stringValue(RAG_CONTEXT);
    }

    public String diagnoseJson() {
        return stringValue(DIAGNOSE_JSON);
    }

    public String analyzeResultJson() {
        return stringValue(ANALYZE_RESULT_JSON);
    }

    public String rewriteResultJson() {
        return stringValue(REWRITE_RESULT_JSON);
    }

    public String compareResultJson() {
        return stringValue(COMPARE_RESULT_JSON);
    }

    public String originalSql() {
        return stringValue(ORIGINAL_SQL);
    }

    public String beforeSql() {
        return stringValue(BEFORE_SQL);
    }

    public String afterSql() {
        return stringValue(AFTER_SQL);
    }

    public String beforeExplain() {
        return stringValue(BEFORE_EXPLAIN);
    }

    public String afterExplain() {
        return stringValue(AFTER_EXPLAIN);
    }

    private String stringValue(String key) {
        Object value = data().get(key);
        return value == null ? null : value.toString();
    }
}
