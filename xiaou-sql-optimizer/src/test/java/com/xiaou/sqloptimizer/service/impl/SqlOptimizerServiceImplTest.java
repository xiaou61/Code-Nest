package com.xiaou.sqloptimizer.service.impl;

import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import com.xiaou.ai.service.AiSqlOptimizeService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.sqloptimizer.domain.SqlOptimizeRecord;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCaseSummary;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchRecordPayload;
import com.xiaou.sqloptimizer.mapper.SqlOptimizeRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlOptimizerServiceImplTest {

    @Mock
    private AiSqlOptimizeService aiSqlOptimizeService;

    @Mock
    private SqlOptimizeRecordMapper recordMapper;

    @InjectMocks
    private SqlOptimizerServiceImpl sqlOptimizerService;

    @Test
    void shouldFilterFavoriteAndSortByScoreDesc() {
        Long userId = 1001L;
        when(recordMapper.selectAllByUserId(userId)).thenReturn(mockRecords());

        PageResult<SqlWorkbenchCaseSummary> page = sqlOptimizerService.getWorkbenchCases(
                userId,
                1,
                10,
                true,
                null,
                null,
                null,
                "score",
                "desc"
        );

        assertEquals(2L, page.getTotal());
        assertEquals(2, page.getRecords().size());
        assertEquals(2L, page.getRecords().get(0).getId());
        assertEquals(1L, page.getRecords().get(1).getId());
    }

    @Test
    void shouldFilterByRewriteAndSeverity() {
        Long userId = 1001L;
        when(recordMapper.selectAllByUserId(userId)).thenReturn(mockRecords());

        PageResult<SqlWorkbenchCaseSummary> page = sqlOptimizerService.getWorkbenchCases(
                userId,
                1,
                10,
                null,
                true,
                null,
                "HIGH",
                "createTime",
                "desc"
        );

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getRecords().size());
        SqlWorkbenchCaseSummary summary = page.getRecords().get(0);
        assertEquals(2L, summary.getId());
        assertTrue(summary.getHasRewrite());
        assertEquals("HIGH", summary.getHighestSeverity());
    }

    @Test
    void shouldSortBySeverityDesc() {
        Long userId = 1001L;
        when(recordMapper.selectAllByUserId(userId)).thenReturn(mockRecords());

        PageResult<SqlWorkbenchCaseSummary> page = sqlOptimizerService.getWorkbenchCases(
                userId,
                1,
                10,
                null,
                null,
                null,
                null,
                "severity",
                "desc"
        );

        assertEquals(3L, page.getTotal());
        assertEquals(3, page.getRecords().size());
        assertEquals(2L, page.getRecords().get(0).getId());
        assertEquals(3L, page.getRecords().get(1).getId());
        assertEquals(1L, page.getRecords().get(2).getId());
    }

    private List<SqlOptimizeRecord> mockRecords() {
        SqlOptimizeRecord r1 = new SqlOptimizeRecord()
                .setId(1L)
                .setUserId(1001L)
                .setOriginalSql("SELECT * FROM t_order")
                .setAnalysisResult(buildPayload("LOW", false, false))
                .setScore(65)
                .setIsFavorite(1)
                .setCreateTime(LocalDateTime.of(2026, 3, 1, 10, 0, 0));

        SqlOptimizeRecord r2 = new SqlOptimizeRecord()
                .setId(2L)
                .setUserId(1001L)
                .setOriginalSql("SELECT * FROM t_user WHERE name = 'a'")
                .setAnalysisResult(buildPayload("HIGH", true, false))
                .setScore(90)
                .setIsFavorite(1)
                .setCreateTime(LocalDateTime.of(2026, 3, 2, 10, 0, 0));

        SqlOptimizeRecord r3 = new SqlOptimizeRecord()
                .setId(3L)
                .setUserId(1001L)
                .setOriginalSql("SELECT id FROM t_user")
                .setAnalysisResult(buildPayload("MEDIUM", false, true))
                .setScore(80)
                .setIsFavorite(0)
                .setCreateTime(LocalDateTime.of(2026, 3, 3, 10, 0, 0));

        return List.of(r1, r2, r3);
    }

    private String buildPayload(String severity, boolean hasRewrite, boolean hasCompare) {
        SqlAnalyzeResult analysis = new SqlAnalyzeResult()
                .setScore(80)
                .setFallback(false)
                .setProblems(List.of(
                        new SqlAnalyzeResult.Problem()
                                .setType("TEST")
                                .setSeverity(severity)
                                .setDescription("测试问题")
                                .setLocation("WHERE")
                ))
                .setExplainAnalysis(List.of())
                .setSuggestions(List.of());

        SqlRewriteResult rewrite = null;
        if (hasRewrite) {
            rewrite = new SqlRewriteResult()
                    .setOptimizedSql("SELECT id FROM t_user")
                    .setIndexDdls(List.of("ALTER TABLE t_user ADD INDEX idx_name(name)"))
                    .setRewriteReason("测试重写")
                    .setRiskWarnings(List.of("测试风险"))
                    .setExpectedImprovement("测试收益")
                    .setFallback(false);
        }

        SqlCompareResult compare = null;
        if (hasCompare) {
            compare = new SqlCompareResult()
                    .setImprovementScore(20)
                    .setDeltaRows("-1000")
                    .setDeltaType("ALL -> ref")
                    .setDeltaExtra("Using filesort -> NULL")
                    .setSummary("测试对比")
                    .setAttention(List.of())
                    .setFallback(false);
        }

        SqlWorkbenchRecordPayload payload = new SqlWorkbenchRecordPayload()
                .setWorkflowVersion("v2")
                .setFallback(false)
                .setAnalysis(analysis)
                .setRewrite(rewrite)
                .setCompare(compare);

        return JSONUtil.toJsonStr(payload);
    }
}

