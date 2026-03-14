package com.xiaou.sqloptimizer.controller;

import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.sqloptimizer.domain.SqlOptimizeRecord;
import com.xiaou.sqloptimizer.dto.SqlAnalyzeRequest;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchBatchAnalyzeRequest;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchBatchAnalyzeResponse;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchAnalyzeResponse;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCaseDetailResponse;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCaseSummary;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCompareRequest;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCompareResponse;
import com.xiaou.sqloptimizer.service.SqlOptimizerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 慢SQL优化控制器
 *
 * @author xiaou
 */
@Tag(name = "慢SQL智能优化", description = "SQL性能分析与优化建议")
@RestController
@RequestMapping("/user/sql-optimizer")
@RequiredArgsConstructor
public class SqlOptimizerController {

    private final SqlOptimizerService sqlOptimizerService;

    @Operation(summary = "分析SQL")
    @PostMapping("/analyze")
    public Result<SqlAnalyzeResult> analyze(@Validated @RequestBody SqlAnalyzeRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        SqlAnalyzeResult result = sqlOptimizerService.analyze(userId, request);
        return Result.success("分析完成", result);
    }

    @Operation(summary = "工作台2.0分析")
    @PostMapping("/workbench/analyze")
    public Result<SqlWorkbenchAnalyzeResponse> analyzeWorkbench(@Validated @RequestBody SqlAnalyzeRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        SqlWorkbenchAnalyzeResponse response = sqlOptimizerService.analyzeWorkbench(userId, request);
        return Result.success("工作台分析完成", response);
    }

    @Operation(summary = "工作台2.0重写建议")
    @PostMapping("/workbench/rewrite")
    public Result<SqlWorkbenchAnalyzeResponse> rewriteWorkbench(@Validated @RequestBody SqlAnalyzeRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        SqlWorkbenchAnalyzeResponse response = sqlOptimizerService.rewriteWorkbench(userId, request);
        return Result.success("重写建议生成完成", response);
    }

    @Operation(summary = "工作台2.0批量分析")
    @PostMapping("/workbench/batch-analyze")
    public Result<SqlWorkbenchBatchAnalyzeResponse> batchAnalyzeWorkbench(@Validated @RequestBody SqlWorkbenchBatchAnalyzeRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        SqlWorkbenchBatchAnalyzeResponse response = sqlOptimizerService.batchAnalyzeWorkbench(userId, request.getItems());
        return Result.success("批量分析完成", response);
    }

    @Operation(summary = "工作台2.0收益对比")
    @PostMapping("/workbench/compare")
    public Result<SqlWorkbenchCompareResponse> compareWorkbench(@Validated @RequestBody SqlWorkbenchCompareRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        SqlWorkbenchCompareResponse response = sqlOptimizerService.compareWorkbench(userId, request);
        return Result.success("收益对比完成", response);
    }

    @Operation(summary = "工作台2.0案例列表")
    @GetMapping("/workbench/cases")
    public Result<PageResult<SqlWorkbenchCaseSummary>> getWorkbenchCases(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(required = false) Boolean hasRewrite,
            @RequestParam(required = false) Boolean hasCompare,
            @RequestParam(required = false) String highestSeverity,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        PageResult<SqlWorkbenchCaseSummary> result = sqlOptimizerService.getWorkbenchCases(
                userId,
                pageNum,
                pageSize,
                favorite,
                hasRewrite,
                hasCompare,
                highestSeverity,
                sortBy,
                sortOrder
        );
        return Result.success(result);
    }

    @Operation(summary = "工作台2.0案例详情")
    @GetMapping("/workbench/cases/{id}")
    public Result<SqlWorkbenchCaseDetailResponse> getWorkbenchCaseById(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        SqlWorkbenchCaseDetailResponse result = sqlOptimizerService.getWorkbenchCaseById(userId, id);
        return Result.success(result);
    }

    @Operation(summary = "获取分析历史")
    @GetMapping("/history")
    public Result<PageResult<SqlOptimizeRecord>> getHistory(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        PageResult<SqlOptimizeRecord> result = sqlOptimizerService.getHistory(userId, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "获取记录详情")
    @GetMapping("/{id}")
    public Result<SqlOptimizeRecord> getById(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        SqlOptimizeRecord record = sqlOptimizerService.getById(userId, id);
        return Result.success(record);
    }

    @Operation(summary = "切换收藏状态")
    @PostMapping("/favorite/{id}")
    public Result<Boolean> toggleFavorite(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        boolean isFavorite = sqlOptimizerService.toggleFavorite(userId, id);
        return Result.success(isFavorite ? "已收藏" : "已取消收藏", isFavorite);
    }

    @Operation(summary = "删除记录")
    @DeleteMapping("/history/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        sqlOptimizerService.delete(userId, id);
        return Result.success("删除成功");
    }
}
