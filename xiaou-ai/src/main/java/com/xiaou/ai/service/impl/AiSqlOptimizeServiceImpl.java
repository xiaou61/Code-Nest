package com.xiaou.ai.service.impl;

import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlAnalyzeRewriteResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import com.xiaou.ai.graph.sql.SqlOptimizeGraphRunner;
import com.xiaou.ai.scene.sql.SqlAnalyzeMode;
import com.xiaou.ai.service.AiSqlOptimizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SQL优化AI服务实现。
 *
 * <p>当前将复杂链路切换为 LangGraph4j 图编排，
 * 对外仍保持原有 service 接口与 DTO 不变。</p>
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSqlOptimizeServiceImpl implements AiSqlOptimizeService {

    private final SqlOptimizeGraphRunner sqlOptimizeGraphRunner;

    @Override
    public SqlAnalyzeResult analyzeSql(String sql, String explainResult, String explainFormat,
                                       String tableStructures, String mysqlVersion) {
        try {
            return sqlOptimizeGraphRunner.runAnalyze(
                    SqlAnalyzeMode.STANDARD,
                    sql,
                    explainResult,
                    explainFormat,
                    tableStructures,
                    mysqlVersion
            );
        } catch (Exception e) {
            log.error("SQL 基础分析图编排执行失败，返回降级结果", e);
            return SqlAnalyzeResult.fallbackResult(explainResult);
        }
    }

    @Override
    public SqlAnalyzeResult analyzeSqlV2(String sql, String explainResult, String explainFormat,
                                         String tableStructures, String mysqlVersion) {
        try {
            SqlAnalyzeResult v2Result = sqlOptimizeGraphRunner.runAnalyze(
                    SqlAnalyzeMode.WORKBENCH_V2,
                    sql,
                    explainResult,
                    explainFormat,
                    tableStructures,
                    mysqlVersion
            );
            if (!v2Result.isFallback()) {
                return v2Result;
            }

            log.warn("SQL 优化工作台 2.0 分析降级，自动回退到基础分析图");
            return sqlOptimizeGraphRunner.runAnalyze(
                    SqlAnalyzeMode.STANDARD,
                    sql,
                    explainResult,
                    explainFormat,
                    tableStructures,
                    mysqlVersion
            );
        } catch (Exception e) {
            log.error("SQL 工作台分析图编排执行失败，返回降级结果", e);
            return SqlAnalyzeResult.fallbackResult(explainResult);
        }
    }

    @Override
    public SqlAnalyzeRewriteResult analyzeAndRewriteSqlV2(String sql, String explainResult, String explainFormat,
                                                          String tableStructures, String mysqlVersion) {
        try {
            SqlAnalyzeRewriteResult result = sqlOptimizeGraphRunner.runAnalyzeAndRewrite(
                    SqlAnalyzeMode.WORKBENCH_V2,
                    sql,
                    explainResult,
                    explainFormat,
                    tableStructures,
                    mysqlVersion
            );
            if (!result.isFallback()) {
                return result;
            }

            log.warn("SQL 优化工作台 2.0 分析+重写降级，自动回退到基础分析图");
            return sqlOptimizeGraphRunner.runAnalyzeAndRewrite(
                    SqlAnalyzeMode.STANDARD,
                    sql,
                    explainResult,
                    explainFormat,
                    tableStructures,
                    mysqlVersion
            );
        } catch (Exception e) {
            log.error("SQL 分析+重写图编排执行失败，返回降级结果", e);
            return SqlAnalyzeRewriteResult.fallbackResult(sql, explainResult);
        }
    }

    @Override
    public SqlRewriteResult rewriteSqlV2(String originalSql, String diagnoseJson, String tableStructures, String mysqlVersion) {
        try {
            return sqlOptimizeGraphRunner.runRewrite(
                    originalSql,
                    diagnoseJson,
                    tableStructures,
                    mysqlVersion
            );
        } catch (Exception e) {
            log.error("SQL 重写图编排执行失败，返回降级结果", e);
            return SqlRewriteResult.fallbackResult(originalSql);
        }
    }

    @Override
    public SqlCompareResult compareSqlV2(String beforeSql, String afterSql, String beforeExplain, String afterExplain, String explainFormat) {
        try {
            return sqlOptimizeGraphRunner.runCompare(
                    beforeSql,
                    afterSql,
                    beforeExplain,
                    afterExplain,
                    explainFormat
            );
        } catch (Exception e) {
            log.error("SQL 收益对比图编排执行失败，返回降级结果", e);
            return SqlCompareResult.fallbackResult();
        }
    }
}
