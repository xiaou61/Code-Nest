package com.xiaou.ai.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import com.xiaou.ai.service.AiSqlOptimizeService;
import com.xiaou.ai.util.CozeResponseParser;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.enums.CozeWorkflowEnum;
import com.xiaou.common.utils.CozeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL优化AI服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSqlOptimizeServiceImpl implements AiSqlOptimizeService {

    private final CozeUtils cozeUtils;

    @Override
    public SqlAnalyzeResult analyzeSql(String sql, String explainResult, String explainFormat,
                                        String tableStructures, String mysqlVersion) {
        return analyzeByWorkflow(
                CozeWorkflowEnum.SQL_OPTIMIZE_ANALYZE,
                sql,
                explainResult,
                explainFormat,
                tableStructures,
                mysqlVersion
        );
    }

    @Override
    public SqlAnalyzeResult analyzeSqlV2(String sql, String explainResult, String explainFormat,
                                          String tableStructures, String mysqlVersion) {
        SqlAnalyzeResult v2Result = analyzeByWorkflow(
                CozeWorkflowEnum.SQL_OPTIMIZE_ANALYZE_V2,
                sql,
                explainResult,
                explainFormat,
                tableStructures,
                mysqlVersion
        );
        if (!v2Result.isFallback()) {
            return v2Result;
        }

        log.warn("SQL_OPTIMIZE_ANALYZE_V2调用失败，自动回退到SQL_OPTIMIZE_ANALYZE");
        return analyzeByWorkflow(
                CozeWorkflowEnum.SQL_OPTIMIZE_ANALYZE,
                sql,
                explainResult,
                explainFormat,
                tableStructures,
                mysqlVersion
        );
    }

    @Override
    public SqlRewriteResult rewriteSqlV2(String originalSql, String diagnoseJson, String tableStructures, String mysqlVersion) {
        try {
            if (!cozeUtils.isClientAvailable()) {
                log.warn("Coze客户端不可用，返回降级重写结果");
                return SqlRewriteResult.fallbackResult(originalSql);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("originalSql", originalSql);
            params.put("diagnoseJson", diagnoseJson);
            params.put("tableStructures", tableStructures);
            params.put("mysqlVersion", mysqlVersion != null ? mysqlVersion : "8.0");

            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.SQL_OPTIMIZE_REWRITE_V2, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                log.warn("Coze重写工作流调用失败: {}", result.getMessage());
                return SqlRewriteResult.fallbackResult(originalSql);
            }

            return parseRewriteResult(result.getData(), originalSql);
        } catch (Exception e) {
            log.error("SQL重写建议生成失败", e);
            return SqlRewriteResult.fallbackResult(originalSql);
        }
    }

    @Override
    public SqlCompareResult compareSqlV2(String beforeSql, String afterSql, String beforeExplain, String afterExplain, String explainFormat) {
        try {
            if (!cozeUtils.isClientAvailable()) {
                log.warn("Coze客户端不可用，返回降级对比结果");
                return SqlCompareResult.fallbackResult();
            }

            Map<String, Object> params = new HashMap<>();
            params.put("beforeSql", beforeSql);
            params.put("afterSql", afterSql);
            params.put("beforeExplain", beforeExplain);
            params.put("afterExplain", afterExplain);
            params.put("explainFormat", StringUtils.hasText(explainFormat) ? explainFormat : "TABLE");

            Result<String> result = cozeUtils.runWorkflow(CozeWorkflowEnum.SQL_OPTIMIZE_COMPARE_V2, params);
            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                log.warn("Coze对比工作流调用失败: {}", result.getMessage());
                return SqlCompareResult.fallbackResult();
            }

            return parseCompareResult(result.getData());
        } catch (Exception e) {
            log.error("SQL收益对比失败", e);
            return SqlCompareResult.fallbackResult();
        }
    }

    private SqlAnalyzeResult analyzeByWorkflow(CozeWorkflowEnum workflowEnum,
                                                String sql,
                                                String explainResult,
                                                String explainFormat,
                                                String tableStructures,
                                                String mysqlVersion) {
        try {
            if (!cozeUtils.isClientAvailable()) {
                log.warn("Coze客户端不可用，使用降级分析");
                return SqlAnalyzeResult.fallbackResult(explainResult);
            }

            Map<String, Object> params = buildParams(sql, explainResult, explainFormat, tableStructures, mysqlVersion);

            Result<String> result = cozeUtils.runWorkflow(workflowEnum, params);

            if (!result.isSuccess() || CozeResponseParser.isErrorResponse(result.getData())) {
                log.warn("Coze工作流调用失败: workflow={}, message={}", workflowEnum.name(), result.getMessage());
                return SqlAnalyzeResult.fallbackResult(explainResult);
            }

            return parseAnalyzeResult(result.getData(), explainResult);

        } catch (Exception e) {
            log.error("SQL分析失败: workflow={}", workflowEnum.name(), e);
            return SqlAnalyzeResult.fallbackResult(explainResult);
        }
    }

    private Map<String, Object> buildParams(String sql,
                                             String explainResult,
                                             String explainFormat,
                                             String tableStructures,
                                             String mysqlVersion) {
        Map<String, Object> params = new HashMap<>();
        params.put("sql", sql);
        params.put("explainResult", explainResult);
        params.put("explainFormat", explainFormat != null ? explainFormat : "TABLE");
        params.put("tableStructures", tableStructures);
        params.put("mysqlVersion", mysqlVersion != null ? mysqlVersion : "8.0");
        return params;
    }

    /**
     * 解析分析结果
     */
    private SqlAnalyzeResult parseAnalyzeResult(String response, String explainResult) {
        JSONObject json = CozeResponseParser.parse(response);
        if (json == null) {
            return SqlAnalyzeResult.fallbackResult(explainResult);
        }

        SqlAnalyzeResult result = new SqlAnalyzeResult();
        result.setScore(CozeResponseParser.getInt(json, "score", 50));
        result.setOptimizedSql(json.getStr("optimizedSql"));
        result.setKnowledgePoints(json.getBeanList("knowledgePoints", String.class));

        // 解析问题列表
        List<SqlAnalyzeResult.Problem> problems = new ArrayList<>();
        JSONArray problemsArray = json.getJSONArray("problems");
        if (problemsArray != null) {
            for (int i = 0; i < problemsArray.size(); i++) {
                JSONObject item = problemsArray.getJSONObject(i);
                problems.add(new SqlAnalyzeResult.Problem()
                        .setType(item.getStr("type"))
                        .setSeverity(item.getStr("severity"))
                        .setDescription(item.getStr("description"))
                        .setLocation(item.getStr("location")));
            }
        }
        result.setProblems(problems);

        // 解析EXPLAIN分析
        List<SqlAnalyzeResult.ExplainAnalysis> explainAnalysis = new ArrayList<>();
        JSONArray explainArray = json.getJSONArray("explainAnalysis");
        if (explainArray != null) {
            for (int i = 0; i < explainArray.size(); i++) {
                JSONObject item = explainArray.getJSONObject(i);
                explainAnalysis.add(new SqlAnalyzeResult.ExplainAnalysis()
                        .setTable(item.getStr("table"))
                        .setType(item.getStr("type"))
                        .setTypeExplain(item.getStr("typeExplain"))
                        .setKey(item.getStr("key"))
                        .setKeyExplain(item.getStr("keyExplain"))
                        .setRows(item.getInt("rows"))
                        .setExtra(item.getStr("extra"))
                        .setExtraExplain(item.getStr("extraExplain"))
                        .setIssue(item.getStr("issue")));
            }
        }
        result.setExplainAnalysis(explainAnalysis);

        // 解析优化建议
        List<SqlAnalyzeResult.Suggestion> suggestions = new ArrayList<>();
        JSONArray suggestionsArray = json.getJSONArray("suggestions");
        if (suggestionsArray != null) {
            for (int i = 0; i < suggestionsArray.size(); i++) {
                JSONObject item = suggestionsArray.getJSONObject(i);
                suggestions.add(new SqlAnalyzeResult.Suggestion()
                        .setType(item.getStr("type"))
                        .setTitle(item.getStr("title"))
                        .setDdl(item.getStr("ddl"))
                        .setSql(item.getStr("sql"))
                        .setReason(item.getStr("reason"))
                        .setExpectedImprovement(item.getStr("expectedImprovement")));
            }
        }
        result.setSuggestions(suggestions);

        result.setFallback(false);
        return result;
    }

    private SqlRewriteResult parseRewriteResult(String response, String originalSql) {
        JSONObject json = CozeResponseParser.parse(response);
        if (json == null) {
            return SqlRewriteResult.fallbackResult(originalSql);
        }

        SqlRewriteResult result = new SqlRewriteResult();
        String optimizedSql = json.getStr("optimizedSql");
        result.setOptimizedSql(StringUtils.hasText(optimizedSql) ? optimizedSql : originalSql);
        result.setIndexDdls(json.getBeanList("indexDdls", String.class));
        result.setRewriteReason(json.getStr("rewriteReason"));
        result.setRiskWarnings(json.getBeanList("riskWarnings", String.class));
        result.setExpectedImprovement(json.getStr("expectedImprovement"));
        result.setFallback(false);

        if (result.getIndexDdls() == null) {
            result.setIndexDdls(new ArrayList<>());
        }
        if (result.getRiskWarnings() == null) {
            result.setRiskWarnings(new ArrayList<>());
        }

        return result;
    }

    private SqlCompareResult parseCompareResult(String response) {
        JSONObject json = CozeResponseParser.parse(response);
        if (json == null) {
            return SqlCompareResult.fallbackResult();
        }

        SqlCompareResult result = new SqlCompareResult();
        result.setImprovementScore(CozeResponseParser.getInt(json, "improvementScore", 0));
        result.setDeltaRows(json.getStr("deltaRows"));
        result.setDeltaType(json.getStr("deltaType"));
        result.setDeltaExtra(json.getStr("deltaExtra"));
        result.setSummary(json.getStr("summary"));
        result.setAttention(json.getBeanList("attention", String.class));
        result.setFallback(false);
        if (result.getAttention() == null) {
            result.setAttention(new ArrayList<>());
        }
        return result;
    }
}
