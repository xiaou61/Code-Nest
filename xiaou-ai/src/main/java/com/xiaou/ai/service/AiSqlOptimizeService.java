package com.xiaou.ai.service;

import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlAnalyzeRewriteResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;

/**
 * SQL优化AI服务接口
 *
 * @author xiaou
 */
public interface AiSqlOptimizeService {

    /**
     * 分析SQL并给出优化建议
     *
     * @param sql             待优化的SQL语句
     * @param explainResult   EXPLAIN执行计划结果
     * @param explainFormat   EXPLAIN格式（TABLE/JSON）
     * @param tableStructures 表结构DDL（JSON数组格式）
     * @param mysqlVersion    MySQL版本
     * @return 分析结果
     */
    SqlAnalyzeResult analyzeSql(String sql, String explainResult, String explainFormat,
                                 String tableStructures, String mysqlVersion);

    /**
     * SQL优化工作台2.0分析
     * 优先调用V2分析流程，失败时自动回退到V1分析流程。
     *
     * @param sql             待优化的SQL语句
     * @param explainResult   EXPLAIN执行计划结果
     * @param explainFormat   EXPLAIN格式（TABLE/JSON）
     * @param tableStructures 表结构DDL（JSON数组格式）
     * @param mysqlVersion    MySQL版本
     * @return 分析结果
     */
    SqlAnalyzeResult analyzeSqlV2(String sql, String explainResult, String explainFormat,
                                   String tableStructures, String mysqlVersion);

    /**
     * SQL优化工作台2.0 分析 + 重写组合流程。
     *
     * @param sql             待优化的SQL语句
     * @param explainResult   EXPLAIN执行计划结果
     * @param explainFormat   EXPLAIN格式（TABLE/JSON）
     * @param tableStructures 表结构DDL（JSON数组格式）
     * @param mysqlVersion    MySQL版本
     * @return 分析与重写组合结果
     */
    SqlAnalyzeRewriteResult analyzeAndRewriteSqlV2(String sql, String explainResult, String explainFormat,
                                                   String tableStructures, String mysqlVersion);

    /**
     * SQL重写建议2.0
     *
     * @param originalSql     原始SQL
     * @param diagnoseJson    诊断JSON
     * @param tableStructures 表结构DDL（JSON数组格式）
     * @param mysqlVersion    MySQL版本
     * @return 重写建议结果
     */
    SqlRewriteResult rewriteSqlV2(String originalSql, String diagnoseJson, String tableStructures, String mysqlVersion);

    /**
     * SQL优化前后收益对比2.0
     *
     * @param beforeSql     优化前SQL
     * @param afterSql      优化后SQL
     * @param beforeExplain 优化前EXPLAIN
     * @param afterExplain  优化后EXPLAIN
     * @param explainFormat EXPLAIN格式
     * @return 收益对比结果
     */
    SqlCompareResult compareSqlV2(String beforeSql, String afterSql, String beforeExplain, String afterExplain, String explainFormat);
}
