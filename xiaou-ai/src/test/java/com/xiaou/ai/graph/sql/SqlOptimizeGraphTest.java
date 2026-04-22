package com.xiaou.ai.graph.sql;

import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlAnalyzeRewriteResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import com.xiaou.ai.rag.LlamaIndexClient;
import com.xiaou.ai.rag.LlamaIndexRetrieveResponse;
import com.xiaou.ai.scene.sql.SqlAnalyzeMode;
import com.xiaou.ai.scene.sql.SqlOptimizeSceneSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlOptimizeGraphTest {

    @Mock
    private SqlOptimizeSceneSupport sceneSupport;

    @Mock
    private LlamaIndexClient llamaIndexClient;

    @InjectMocks
    private SqlOptimizeGraphRunner graphRunner;

    @Test
    void shouldRunAnalyzeRewriteGraphWithRagContextAndRiskCheck() {
        when(llamaIndexClient.isAvailable()).thenReturn(true);
        when(llamaIndexClient.retrieve(any())).thenReturn(new LlamaIndexRetrieveResponse()
                .setQuery("select")
                .setNodes(List.of(new LlamaIndexRetrieveResponse.Node()
                        .setId("doc-1")
                        .setScore(0.9)
                        .setText("优先避免 filesort 和全表扫描"))));

        SqlAnalyzeResult analyzeResult = new SqlAnalyzeResult()
                .setScore(88)
                .setProblems(List.of())
                .setExplainAnalysis(List.of())
                .setSuggestions(List.of())
                .setKnowledgePoints(List.of("联合索引"))
                .setFallback(false);
        when(sceneSupport.analyze(eq(SqlAnalyzeMode.WORKBENCH_V2), any(), any(), any(), any(), any(), any()))
                .thenReturn(analyzeResult);

        SqlRewriteResult rewriteResult = new SqlRewriteResult()
                .setOptimizedSql("SELECT id FROM t_user WHERE name = ?")
                .setIndexDdls(List.of("ALTER TABLE t_user ADD INDEX idx_name(name)"))
                .setRewriteReason("减少回表和排序")
                .setRiskWarnings(List.of())
                .setExpectedImprovement("rows 降低")
                .setFallback(false);
        when(sceneSupport.rewrite(any(), any(), any(), any(), any())).thenReturn(rewriteResult);

        SqlAnalyzeRewriteResult result = graphRunner.runAnalyzeAndRewrite(
                SqlAnalyzeMode.WORKBENCH_V2,
                "SELECT * FROM t_user WHERE name = 'a'",
                "type=ALL; Extra=Using filesort",
                "TABLE",
                "[{\"tableName\":\"t_user\",\"ddl\":\"CREATE TABLE t_user(id bigint,name varchar(32))\"}]",
                "8.0"
        );

        assertFalse(result.isFallback());
        assertEquals(88, result.getAnalysis().getScore());
        assertTrue(result.getRewrite().getRiskWarnings().stream()
                .anyMatch(item -> item.contains("测试环境验证执行计划")));

        ArgumentCaptor<String> ragContextCaptor = ArgumentCaptor.forClass(String.class);
        verify(sceneSupport).analyze(eq(SqlAnalyzeMode.WORKBENCH_V2), any(), any(), any(), any(), any(), ragContextCaptor.capture());
        assertTrue(ragContextCaptor.getValue().contains("避免 filesort"));
    }

    @Test
    void shouldRunCompareGraphWithoutRagRetrieval() {
        SqlCompareResult compareResult = new SqlCompareResult()
                .setImprovementScore(35)
                .setDeltaRows("-1000")
                .setDeltaType("ALL -> range")
                .setDeltaExtra("Using filesort -> Using index")
                .setSummary("收益明显")
                .setAttention(List.of("注意回归测试"))
                .setFallback(false);
        when(sceneSupport.compare(any(), any(), any(), any(), any())).thenReturn(compareResult);

        SqlCompareResult result = graphRunner.runCompare(
                "SELECT * FROM t_user",
                "SELECT id FROM t_user WHERE name = ?",
                "type=ALL",
                "type=range",
                "TABLE"
        );

        assertEquals(35, result.getImprovementScore());
        verify(llamaIndexClient, never()).retrieve(any());
    }

    @Test
    void shouldMarkAnalyzeRewriteFallbackWhenRewriteFallsBack() {
        when(llamaIndexClient.isAvailable()).thenReturn(false);

        SqlAnalyzeResult analyzeResult = new SqlAnalyzeResult()
                .setScore(61)
                .setOptimizedSql("SELECT id, user_id FROM orders WHERE user_id = 1")
                .setProblems(List.of())
                .setExplainAnalysis(List.of())
                .setSuggestions(List.of())
                .setKnowledgePoints(List.of("覆盖索引"))
                .setFallback(false);
        when(sceneSupport.analyze(eq(SqlAnalyzeMode.WORKBENCH_V2), any(), any(), any(), any(), any(), any()))
                .thenReturn(analyzeResult);
        when(sceneSupport.rewrite(any(), any(), any(), any(), any()))
                .thenReturn(SqlRewriteResult.fallbackResult("SELECT * FROM orders WHERE user_id = 1"));

        SqlAnalyzeRewriteResult result = graphRunner.runAnalyzeAndRewrite(
                SqlAnalyzeMode.WORKBENCH_V2,
                "SELECT * FROM orders WHERE user_id = 1",
                "type=ALL",
                "TABLE",
                "CREATE TABLE orders(id bigint,user_id bigint);",
                "8.0"
        );

        assertTrue(result.isFallback());
        assertFalse(result.getAnalysis().isFallback());
        assertTrue(result.getRewrite().isFallback());
        assertTrue(result.getRewrite().getRiskWarnings().stream()
                .anyMatch(item -> item.contains("AI 重写能力暂不可用")));
    }
}
