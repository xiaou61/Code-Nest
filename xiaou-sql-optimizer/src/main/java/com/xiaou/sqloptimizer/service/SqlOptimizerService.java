package com.xiaou.sqloptimizer.service;

import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.sqloptimizer.domain.SqlOptimizeRecord;
import com.xiaou.sqloptimizer.dto.SqlAnalyzeRequest;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchBatchAnalyzeResponse;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchAnalyzeResponse;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCaseDetailResponse;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCaseSummary;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCompareRequest;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCompareResponse;

import java.util.List;

/**
 * SQL优化服务接口
 *
 * @author xiaou
 */
public interface SqlOptimizerService {

    /**
     * 分析SQL
     *
     * @param userId  用户ID
     * @param request 分析请求
     * @return 分析结果
     */
    SqlAnalyzeResult analyze(Long userId, SqlAnalyzeRequest request);

    /**
     * SQL优化工作台2.0分析
     *
     * @param userId  用户ID
     * @param request 分析请求
     * @return 工作台分析响应
     */
    SqlWorkbenchAnalyzeResponse analyzeWorkbench(Long userId, SqlAnalyzeRequest request);

    /**
     * SQL优化工作台2.0重写建议
     *
     * @param userId  用户ID
     * @param request 重写请求（复用分析入参）
     * @return 工作台分析响应（包含rewrite字段）
     */
    SqlWorkbenchAnalyzeResponse rewriteWorkbench(Long userId, SqlAnalyzeRequest request);

    /**
     * SQL优化工作台2.0批量分析
     *
     * @param userId   用户ID
     * @param requests 批量请求
     * @return 批量分析响应
     */
    SqlWorkbenchBatchAnalyzeResponse batchAnalyzeWorkbench(Long userId, List<SqlAnalyzeRequest> requests);

    /**
     * SQL优化工作台2.0收益对比
     *
     * @param userId  用户ID
     * @param request 对比请求
     * @return 对比结果
     */
    SqlWorkbenchCompareResponse compareWorkbench(Long userId, SqlWorkbenchCompareRequest request);

    /**
     * 分页查询工作台案例摘要
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param favorite 是否仅收藏
     * @param hasRewrite 是否包含重写结果
     * @param hasCompare 是否包含对比结果
     * @param highestSeverity 最高严重级别筛选（HIGH/MEDIUM/LOW/NONE）
     * @param sortBy 排序字段（createTime/score/severity）
     * @param sortOrder 排序方向（asc/desc）
     * @return 案例摘要分页
     */
    PageResult<SqlWorkbenchCaseSummary> getWorkbenchCases(Long userId,
                                                          int pageNum,
                                                          int pageSize,
                                                          Boolean favorite,
                                                          Boolean hasRewrite,
                                                          Boolean hasCompare,
                                                          String highestSeverity,
                                                          String sortBy,
                                                          String sortOrder);

    /**
     * 获取工作台案例详情
     *
     * @param userId 用户ID
     * @param id     记录ID
     * @return 案例详情
     */
    SqlWorkbenchCaseDetailResponse getWorkbenchCaseById(Long userId, Long id);

    /**
     * 获取分析历史
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<SqlOptimizeRecord> getHistory(Long userId, int pageNum, int pageSize);

    /**
     * 获取记录详情
     *
     * @param userId 用户ID
     * @param id     记录ID
     * @return 记录详情
     */
    SqlOptimizeRecord getById(Long userId, Long id);

    /**
     * 切换收藏状态
     *
     * @param userId 用户ID
     * @param id     记录ID
     * @return 新的收藏状态
     */
    boolean toggleFavorite(Long userId, Long id);

    /**
     * 删除记录
     *
     * @param userId 用户ID
     * @param id     记录ID
     */
    void delete(Long userId, Long id);
}
