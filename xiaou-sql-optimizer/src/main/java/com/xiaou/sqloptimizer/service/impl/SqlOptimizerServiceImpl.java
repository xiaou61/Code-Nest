package com.xiaou.sqloptimizer.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import com.xiaou.ai.service.AiSqlOptimizeService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.sqloptimizer.domain.SqlOptimizeRecord;
import com.xiaou.sqloptimizer.dto.SqlAnalyzeRequest;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchBatchAnalyzeResponse;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchAnalyzeResponse;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCaseDetailResponse;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCaseSummary;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCompareRequest;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchCompareResponse;
import com.xiaou.sqloptimizer.dto.SqlWorkbenchRecordPayload;
import com.xiaou.sqloptimizer.mapper.SqlOptimizeRecordMapper;
import com.xiaou.sqloptimizer.service.SqlOptimizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * SQL优化服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlOptimizerServiceImpl implements SqlOptimizerService {

    private static final int MAX_BATCH_SIZE = 20;
    private static final int SQL_PREVIEW_MAX_LEN = 120;

    private final AiSqlOptimizeService aiSqlOptimizeService;
    private final SqlOptimizeRecordMapper recordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SqlAnalyzeResult analyze(Long userId, SqlAnalyzeRequest request) {
        log.info("用户{}开始分析SQL", userId);

        String tableStructuresJson = JSONUtil.toJsonStr(request.getTableStructures());
        SqlAnalyzeResult result = aiSqlOptimizeService.analyzeSql(
                request.getSql(),
                request.getExplainResult(),
                request.getExplainFormat(),
                tableStructuresJson,
                request.getMysqlVersion()
        );

        SqlOptimizeRecord record = saveRecord(
                userId,
                request,
                tableStructuresJson,
                JSONUtil.toJsonStr(result),
                result.getScore()
        );
        log.info("SQL分析完成，记录ID: {}, 评分: {}", record.getId(), result.getScore());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SqlWorkbenchAnalyzeResponse analyzeWorkbench(Long userId, SqlAnalyzeRequest request) {
        return analyzeWorkbenchInternal(userId, request, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SqlWorkbenchAnalyzeResponse rewriteWorkbench(Long userId, SqlAnalyzeRequest request) {
        return analyzeWorkbenchInternal(userId, request, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SqlWorkbenchBatchAnalyzeResponse batchAnalyzeWorkbench(Long userId, List<SqlAnalyzeRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("批量分析项不能为空");
        }
        if (requests.size() > MAX_BATCH_SIZE) {
            throw new BusinessException("批量分析最多支持20条");
        }

        int successCount = 0;
        int fallbackCount = 0;
        int failedCount = 0;
        List<SqlWorkbenchBatchAnalyzeResponse.Item> items = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            SqlAnalyzeRequest request = requests.get(i);
            try {
                SqlWorkbenchAnalyzeResponse result = analyzeWorkbenchInternal(userId, request, false);
                successCount++;
                if (Boolean.TRUE.equals(result.getFallback())) {
                    fallbackCount++;
                }
                items.add(new SqlWorkbenchBatchAnalyzeResponse.Item()
                        .setIndex(i)
                        .setSuccess(true)
                        .setMessage("分析成功")
                        .setResult(result));
            } catch (Exception e) {
                failedCount++;
                log.warn("用户{}批量分析第{}项失败", userId, i, e);
                items.add(new SqlWorkbenchBatchAnalyzeResponse.Item()
                        .setIndex(i)
                        .setSuccess(false)
                        .setMessage(e.getMessage())
                        .setResult(null));
            }
        }

        return new SqlWorkbenchBatchAnalyzeResponse()
                .setTotal(requests.size())
                .setSuccessCount(successCount)
                .setFallbackCount(fallbackCount)
                .setFailedCount(failedCount)
                .setItems(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SqlWorkbenchCompareResponse compareWorkbench(Long userId, SqlWorkbenchCompareRequest request) {
        log.info("用户{}开始SQL优化工作台2.0收益对比", userId);
        SqlCompareResult compare = aiSqlOptimizeService.compareSqlV2(
                request.getBeforeSql(),
                request.getAfterSql(),
                request.getBeforeExplain(),
                request.getAfterExplain(),
                request.getExplainFormat()
        );

        if (request.getRecordId() != null) {
            appendCompareToRecord(userId, request.getRecordId(), compare);
        }

        return new SqlWorkbenchCompareResponse()
                .setWorkflowVersion("v2")
                .setFallback(compare.isFallback())
                .setCompare(compare);
    }

    @Override
    public PageResult<SqlWorkbenchCaseSummary> getWorkbenchCases(Long userId,
                                                                 int pageNum,
                                                                 int pageSize,
                                                                 Boolean favorite,
                                                                 Boolean hasRewrite,
                                                                 Boolean hasCompare,
                                                                 String highestSeverity,
                                                                 String sortBy,
                                                                 String sortOrder) {
        pageNum = Math.max(pageNum, 1);
        pageSize = Math.max(Math.min(pageSize, 50), 1);

        List<SqlOptimizeRecord> allRecords = recordMapper.selectAllByUserId(userId);
        List<SqlWorkbenchCaseSummary> summaries = new ArrayList<>(allRecords.size());
        for (SqlOptimizeRecord record : allRecords) {
            summaries.add(toCaseSummary(record));
        }

        String targetSeverity = normalizeSeverity(highestSeverity);
        List<SqlWorkbenchCaseSummary> filtered = summaries.stream()
                .filter(summary -> favorite == null || favorite.equals(summary.getFavorite()))
                .filter(summary -> hasRewrite == null || hasRewrite.equals(summary.getHasRewrite()))
                .filter(summary -> hasCompare == null || hasCompare.equals(summary.getHasCompare()))
                .filter(summary -> isSeverityMatched(summary, highestSeverity, targetSeverity))
                .collect(Collectors.toList());

        filtered.sort(buildCaseSummaryComparator(sortBy, sortOrder));

        int fromIndex = Math.min((pageNum - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<SqlWorkbenchCaseSummary> pageRecords = filtered.subList(fromIndex, toIndex);

        return PageResult.of(pageNum, pageSize, (long) filtered.size(), pageRecords);
    }

    @Override
    public SqlWorkbenchCaseDetailResponse getWorkbenchCaseById(Long userId, Long id) {
        SqlOptimizeRecord record = getById(userId, id);
        return toCaseDetail(record);
    }

    private SqlWorkbenchAnalyzeResponse analyzeWorkbenchInternal(Long userId, SqlAnalyzeRequest request, boolean withRewrite) {
        log.info("用户{}开始SQL优化工作台2.0{}流程", userId, withRewrite ? "重写" : "分析");
        String tableStructuresJson = JSONUtil.toJsonStr(request.getTableStructures());
        SqlAnalyzeResult result = aiSqlOptimizeService.analyzeSqlV2(
                request.getSql(),
                request.getExplainResult(),
                request.getExplainFormat(),
                tableStructuresJson,
                request.getMysqlVersion()
        );

        SqlRewriteResult rewrite = null;
        if (withRewrite) {
            rewrite = aiSqlOptimizeService.rewriteSqlV2(
                    request.getSql(),
                    JSONUtil.toJsonStr(result),
                    tableStructuresJson,
                    request.getMysqlVersion()
            );
            applyRewriteToAnalyzeResult(result, rewrite);
        }

        boolean fallback = result.isFallback() || (rewrite != null && rewrite.isFallback());
        SqlWorkbenchRecordPayload payload = new SqlWorkbenchRecordPayload()
                .setWorkflowVersion("v2")
                .setFallback(fallback)
                .setAnalysis(result)
                .setRewrite(rewrite)
                .setCompare(null);

        SqlOptimizeRecord record = saveRecord(
                userId,
                request,
                tableStructuresJson,
                JSONUtil.toJsonStr(payload),
                result.getScore()
        );
        log.info("SQL优化工作台2.0流程完成，记录ID: {}, 评分: {}, fallback: {}", record.getId(), result.getScore(), fallback);

        return new SqlWorkbenchAnalyzeResponse()
                .setRecordId(record.getId())
                .setWorkflowVersion("v2")
                .setFallback(fallback)
                .setAnalysis(result)
                .setRewrite(rewrite);
    }

    @Override
    public PageResult<SqlOptimizeRecord> getHistory(Long userId, int pageNum, int pageSize) {
        pageNum = Math.max(pageNum, 1);
        pageSize = Math.max(Math.min(pageSize, 50), 1);
        int offset = (pageNum - 1) * pageSize;
        List<SqlOptimizeRecord> records = recordMapper.selectByUserId(userId, offset, pageSize);
        long total = recordMapper.countByUserId(userId);

        return PageResult.of(pageNum, pageSize, total, records);
    }

    @Override
    public SqlOptimizeRecord getById(Long userId, Long id) {
        SqlOptimizeRecord record = recordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("记录不存在");
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long userId, Long id) {
        SqlOptimizeRecord record = getById(userId, id);
        int newFavorite = record.getIsFavorite() == 1 ? 0 : 1;
        recordMapper.updateFavorite(id, userId, newFavorite);
        return newFavorite == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getById(userId, id); // 验证权限
        recordMapper.deleteById(id, userId);
        log.info("用户{}删除SQL分析记录{}", userId, id);
    }

    private SqlOptimizeRecord saveRecord(Long userId,
                                         SqlAnalyzeRequest request,
                                         String tableStructuresJson,
                                         String analysisResultJson,
                                         Integer score) {
        SqlOptimizeRecord record = new SqlOptimizeRecord()
                .setUserId(userId)
                .setOriginalSql(request.getSql())
                .setExplainResult(request.getExplainResult())
                .setExplainFormat(request.getExplainFormat())
                .setTableStructures(tableStructuresJson)
                .setMysqlVersion(request.getMysqlVersion())
                .setAnalysisResult(analysisResultJson)
                .setScore(score)
                .setIsFavorite(0)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());

        recordMapper.insert(record);
        return record;
    }

    private void applyRewriteToAnalyzeResult(SqlAnalyzeResult analysis, SqlRewriteResult rewrite) {
        if (analysis == null || rewrite == null) {
            return;
        }

        if (StringUtils.hasText(rewrite.getOptimizedSql())) {
            analysis.setOptimizedSql(rewrite.getOptimizedSql());
        }

        List<SqlAnalyzeResult.Suggestion> suggestions = analysis.getSuggestions() == null
                ? new ArrayList<>()
                : new ArrayList<>(analysis.getSuggestions());

        if (StringUtils.hasText(rewrite.getRewriteReason())) {
            suggestions.add(new SqlAnalyzeResult.Suggestion()
                    .setType("REWRITE_SQL")
                    .setTitle("SQL重写建议")
                    .setSql(rewrite.getOptimizedSql())
                    .setReason(rewrite.getRewriteReason())
                    .setExpectedImprovement(rewrite.getExpectedImprovement()));
        }

        if (rewrite.getIndexDdls() != null) {
            for (String ddl : rewrite.getIndexDdls()) {
                if (!StringUtils.hasText(ddl)) {
                    continue;
                }
                suggestions.add(new SqlAnalyzeResult.Suggestion()
                        .setType("ADD_INDEX")
                        .setTitle("索引建议")
                        .setDdl(ddl)
                        .setReason("根据重写工作流给出的索引优化建议")
                        .setExpectedImprovement(rewrite.getExpectedImprovement()));
            }
        }

        analysis.setSuggestions(suggestions);
    }

    private void appendCompareToRecord(Long userId, Long recordId, SqlCompareResult compare) {
        SqlOptimizeRecord record = getById(userId, recordId);
        SqlWorkbenchRecordPayload payload = parseRecordPayload(record.getAnalysisResult());
        payload.setWorkflowVersion(StringUtils.hasText(payload.getWorkflowVersion()) ? payload.getWorkflowVersion() : "v2");
        payload.setCompare(compare);
        payload.setFallback(Boolean.TRUE.equals(payload.getFallback()) || compare.isFallback());

        recordMapper.updateAnalysisResult(recordId, userId, JSONUtil.toJsonStr(payload));
    }

    private SqlWorkbenchRecordPayload parseRecordPayload(String analysisResult) {
        if (!StringUtils.hasText(analysisResult)) {
            return new SqlWorkbenchRecordPayload()
                    .setWorkflowVersion("v1")
                    .setFallback(true)
                    .setAnalysis(null)
                    .setRewrite(null)
                    .setCompare(null);
        }

        try {
            JSONObject root = JSONUtil.parseObj(analysisResult);
            boolean isWorkbenchPayload = root.containsKey("analysis")
                    || root.containsKey("rewrite")
                    || root.containsKey("compare")
                    || root.containsKey("workflowVersion");

            if (!isWorkbenchPayload) {
                SqlAnalyzeResult analysis = root.toBean(SqlAnalyzeResult.class);
                return new SqlWorkbenchRecordPayload()
                        .setWorkflowVersion("v1")
                        .setFallback(analysis != null && analysis.isFallback())
                        .setAnalysis(analysis)
                        .setRewrite(null)
                        .setCompare(null);
            }

            SqlWorkbenchRecordPayload payload = new SqlWorkbenchRecordPayload();
            payload.setWorkflowVersion(root.getStr("workflowVersion", "v2"));

            JSONObject analysisObj = root.getJSONObject("analysis");
            if (analysisObj != null) {
                payload.setAnalysis(analysisObj.toBean(SqlAnalyzeResult.class));
            }

            JSONObject rewriteObj = root.getJSONObject("rewrite");
            if (rewriteObj != null) {
                payload.setRewrite(rewriteObj.toBean(SqlRewriteResult.class));
            }

            JSONObject compareObj = root.getJSONObject("compare");
            if (compareObj != null) {
                payload.setCompare(compareObj.toBean(SqlCompareResult.class));
            }

            boolean fallback = root.getBool("fallback", false)
                    || (payload.getAnalysis() != null && payload.getAnalysis().isFallback())
                    || (payload.getRewrite() != null && payload.getRewrite().isFallback())
                    || (payload.getCompare() != null && payload.getCompare().isFallback());
            payload.setFallback(fallback);
            return payload;
        } catch (Exception e) {
            log.warn("解析SQL工作台记录载荷失败，按v1兜底处理", e);
            try {
                SqlAnalyzeResult analysis = JSONUtil.toBean(analysisResult, SqlAnalyzeResult.class);
                return new SqlWorkbenchRecordPayload()
                        .setWorkflowVersion("v1")
                        .setFallback(analysis != null && analysis.isFallback())
                        .setAnalysis(analysis)
                        .setRewrite(null)
                        .setCompare(null);
            } catch (Exception ignored) {
                return new SqlWorkbenchRecordPayload()
                        .setWorkflowVersion("v1")
                        .setFallback(true)
                        .setAnalysis(null)
                        .setRewrite(null)
                        .setCompare(null);
            }
        }
    }

    private SqlWorkbenchCaseSummary toCaseSummary(SqlOptimizeRecord record) {
        SqlWorkbenchRecordPayload payload = parseRecordPayload(record.getAnalysisResult());
        SqlAnalyzeResult analysis = payload.getAnalysis();

        int problemCount = 0;
        if (analysis != null && analysis.getProblems() != null) {
            problemCount = analysis.getProblems().size();
        }
        String highestSeverity = resolveHighestSeverity(analysis);

        return new SqlWorkbenchCaseSummary()
                .setId(record.getId())
                .setScore(record.getScore())
                .setWorkflowVersion(payload.getWorkflowVersion())
                .setFallback(Boolean.TRUE.equals(payload.getFallback()))
                .setHasRewrite(payload.getRewrite() != null)
                .setHasCompare(payload.getCompare() != null)
                .setFavorite(record.getIsFavorite() != null && record.getIsFavorite() == 1)
                .setProblemCount(problemCount)
                .setHighestSeverity(highestSeverity)
                .setOriginalSqlPreview(buildSqlPreview(record.getOriginalSql()))
                .setCreateTime(record.getCreateTime());
    }

    private SqlWorkbenchCaseDetailResponse toCaseDetail(SqlOptimizeRecord record) {
        SqlWorkbenchRecordPayload payload = parseRecordPayload(record.getAnalysisResult());

        return new SqlWorkbenchCaseDetailResponse()
                .setId(record.getId())
                .setWorkflowVersion(payload.getWorkflowVersion())
                .setFallback(Boolean.TRUE.equals(payload.getFallback()))
                .setOriginalSql(record.getOriginalSql())
                .setExplainResult(record.getExplainResult())
                .setExplainFormat(record.getExplainFormat())
                .setMysqlVersion(record.getMysqlVersion())
                .setTableStructures(parseTableStructures(record.getTableStructures()))
                .setAnalysis(payload.getAnalysis())
                .setRewrite(payload.getRewrite())
                .setCompare(payload.getCompare())
                .setScore(record.getScore())
                .setFavorite(record.getIsFavorite() != null && record.getIsFavorite() == 1)
                .setCreateTime(record.getCreateTime())
                .setUpdateTime(record.getUpdateTime());
    }

    private List<SqlAnalyzeRequest.TableStructure> parseTableStructures(String tableStructuresJson) {
        if (!StringUtils.hasText(tableStructuresJson)) {
            return Collections.emptyList();
        }

        try {
            JSONArray array = JSONUtil.parseArray(tableStructuresJson);
            return array.toList(SqlAnalyzeRequest.TableStructure.class);
        } catch (Exception e) {
            log.warn("解析表结构JSON失败，返回空列表", e);
            return Collections.emptyList();
        }
    }

    private String buildSqlPreview(String originalSql) {
        if (!StringUtils.hasText(originalSql)) {
            return "";
        }
        String normalized = originalSql.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= SQL_PREVIEW_MAX_LEN) {
            return normalized;
        }
        return normalized.substring(0, SQL_PREVIEW_MAX_LEN) + "...";
    }

    private String resolveHighestSeverity(SqlAnalyzeResult analysis) {
        if (analysis == null || analysis.getProblems() == null || analysis.getProblems().isEmpty()) {
            return "NONE";
        }

        int highestLevel = 0;
        String highestSeverity = "NONE";

        for (SqlAnalyzeResult.Problem problem : analysis.getProblems()) {
            int level = severityLevel(problem == null ? null : problem.getSeverity());
            if (level > highestLevel) {
                highestLevel = level;
                highestSeverity = normalizeSeverity(problem == null ? null : problem.getSeverity());
            }
        }

        return highestSeverity;
    }

    private int severityLevel(String severity) {
        String normalized = normalizeSeverity(severity);
        return switch (normalized) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private String normalizeSeverity(String severity) {
        if (!StringUtils.hasText(severity)) {
            return "NONE";
        }
        String normalized = severity.trim().toUpperCase();
        if (!"HIGH".equals(normalized) && !"MEDIUM".equals(normalized) && !"LOW".equals(normalized)) {
            return "NONE";
        }
        return normalized;
    }

    private boolean isSeverityMatched(SqlWorkbenchCaseSummary summary, String rawSeverityFilter, String normalizedFilter) {
        if (!StringUtils.hasText(rawSeverityFilter)) {
            return true;
        }
        return normalizedFilter.equals(normalizeSeverity(summary.getHighestSeverity()));
    }

    private Comparator<SqlWorkbenchCaseSummary> buildCaseSummaryComparator(String sortBy, String sortOrder) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        boolean asc = "asc".equalsIgnoreCase(sortOrder);

        Comparator<SqlWorkbenchCaseSummary> baseComparator = switch (normalizedSortBy) {
            case "score" -> Comparator.comparing(
                    summary -> summary.getScore() == null ? Integer.MIN_VALUE : summary.getScore()
            );
            case "severity" -> Comparator.comparing(
                    summary -> severityLevel(summary == null ? null : summary.getHighestSeverity())
            );
            default -> Comparator.comparing(
                    summary -> summary.getCreateTime() == null ? LocalDateTime.MIN : summary.getCreateTime()
            );
        };

        if (asc) {
            return baseComparator.thenComparing(SqlWorkbenchCaseSummary::getId, Comparator.nullsLast(Long::compareTo));
        }

        return baseComparator
                .reversed()
                .thenComparing(SqlWorkbenchCaseSummary::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private String normalizeSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "createtime";
        }

        String normalized = sortBy.trim().toLowerCase(Locale.ROOT);
        if ("score".equals(normalized) || "severity".equals(normalized) || "createtime".equals(normalized)) {
            return normalized;
        }
        return "createtime";
    }
}
