package com.xiaou.ai.service.impl;

import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlAnalyzeRewriteResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import com.xiaou.ai.graph.sql.SqlOptimizeGraphRunner;
import com.xiaou.ai.scene.sql.SqlAnalyzeMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiSqlOptimizeServiceImplTest {

    @Mock
    private SqlOptimizeGraphRunner sqlOptimizeGraphRunner;

    @InjectMocks
    private AiSqlOptimizeServiceImpl service;

    @Test
    void shouldFallbackToStandardAnalyzeRewriteWhenWorkbenchV2ReturnsFallback() {
        SqlAnalyzeRewriteResult v2Fallback = SqlAnalyzeRewriteResult.fallbackResult(
                "SELECT * FROM orders WHERE user_id = 1",
                "type=ALL"
        );
        SqlAnalyzeRewriteResult standardResult = new SqlAnalyzeRewriteResult()
                .setAnalysis(new SqlAnalyzeResult()
                        .setScore(78)
                        .setOptimizedSql("SELECT id, user_id FROM orders WHERE user_id = 1")
                        .setProblems(List.of())
                        .setExplainAnalysis(List.of())
                        .setSuggestions(List.of())
                        .setKnowledgePoints(List.of("联合索引"))
                        .setFallback(false))
                .setRewrite(new SqlRewriteResult()
                        .setOptimizedSql("SELECT id, user_id FROM orders WHERE user_id = 1")
                        .setIndexDdls(List.of("ALTER TABLE orders ADD INDEX idx_user(user_id)"))
                        .setRewriteReason("减少回表")
                        .setRiskWarnings(List.of("先验证执行计划"))
                        .setExpectedImprovement("扫描行数下降")
                        .setFallback(false))
                .setFallback(false);

        when(sqlOptimizeGraphRunner.runAnalyzeAndRewrite(
                eq(SqlAnalyzeMode.WORKBENCH_V2),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(v2Fallback);
        when(sqlOptimizeGraphRunner.runAnalyzeAndRewrite(
                eq(SqlAnalyzeMode.STANDARD),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(standardResult);

        SqlAnalyzeRewriteResult result = service.analyzeAndRewriteSqlV2(
                "SELECT * FROM orders WHERE user_id = 1",
                "type=ALL",
                "TABLE",
                "CREATE TABLE orders(id bigint,user_id bigint);",
                "8.0"
        );

        assertFalse(result.isFallback());
        assertEquals(78, result.getAnalysis().getScore());
        verify(sqlOptimizeGraphRunner, times(1)).runAnalyzeAndRewrite(
                eq(SqlAnalyzeMode.WORKBENCH_V2),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
        verify(sqlOptimizeGraphRunner, times(1)).runAnalyzeAndRewrite(
                eq(SqlAnalyzeMode.STANDARD),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }
}
