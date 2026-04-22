package com.xiaou.ai.scene.sql;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import com.xiaou.ai.metrics.AiMetricsRecorder;
import com.xiaou.ai.prompt.AiPromptSpec;
import com.xiaou.ai.prompt.AiPromptSections;
import com.xiaou.ai.prompt.sql.SqlOptimizePromptSpecs;
import com.xiaou.ai.structured.sql.SqlStructuredOutputSpecs;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.ai.util.AiJsonResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 场景模型调用支撑。
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlOptimizeSceneSupport {
    private final AiExecutionSupport aiExecutionSupport;
    private final AiMetricsRecorder aiMetricsRecorder;

    public SqlAnalyzeResult analyze(SqlAnalyzeMode mode,
                                    String sql,
                                    String explainResult,
                                    String explainFormat,
                                    String tableStructures,
                                    String mysqlVersion,
                                    String ragContext) {
        String sceneName = mode == SqlAnalyzeMode.WORKBENCH_V2
                ? "sql_optimize_analyze_v2"
                : "sql_optimize_analyze";
        var promptSpec = mode == SqlAnalyzeMode.WORKBENCH_V2
                ? SqlOptimizePromptSpecs.ANALYZE_V2
                : SqlOptimizePromptSpecs.ANALYZE;

        return aiExecutionSupport.chatWithFallback(
                sceneName,
                promptSpec,
                buildAnalyzePromptVariables(sql, explainResult, explainFormat, tableStructures, mysqlVersion, ragContext),
                response -> parseAnalyzeResult(sceneName, promptSpec, response, explainResult),
                () -> SqlAnalyzeResult.fallbackResult(explainResult)
        );
    }

    public SqlRewriteResult rewrite(String originalSql,
                                    String diagnoseJson,
                                    String tableStructures,
                                    String mysqlVersion,
                                    String ragContext) {
        return aiExecutionSupport.chatWithFallback(
                "sql_optimize_rewrite_v2",
                SqlOptimizePromptSpecs.REWRITE,
                buildRewritePromptVariables(originalSql, diagnoseJson, tableStructures, mysqlVersion, ragContext),
                response -> parseRewriteResult(response, originalSql),
                () -> SqlRewriteResult.fallbackResult(originalSql)
        );
    }

    public SqlCompareResult compare(String beforeSql,
                                    String afterSql,
                                    String beforeExplain,
                                    String afterExplain,
                                    String explainFormat) {
        return aiExecutionSupport.chatWithFallback(
                "sql_optimize_compare_v2",
                SqlOptimizePromptSpecs.COMPARE,
                buildComparePromptVariables(beforeSql, afterSql, beforeExplain, afterExplain, explainFormat),
                this::parseCompareResult,
                SqlCompareResult::fallbackResult
        );
    }

    private Map<String, Object> buildAnalyzePromptVariables(String sql,
                                                            String explainResult,
                                                            String explainFormat,
                                                            String tableStructures,
                                                            String mysqlVersion,
                                                            String ragContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("sql", defaultText(sql));
        variables.put("explainFormat", defaultText(explainFormat, "TABLE"));
        variables.put("explainResult", defaultText(explainResult));
        variables.put("tableStructures", defaultText(tableStructures));
        variables.put("mysqlVersion", defaultText(mysqlVersion, "8.0"));
        variables.put("ragSection", AiPromptSections.ragSection(ragContext));
        return variables;
    }

    private Map<String, Object> buildRewritePromptVariables(String originalSql,
                                                            String diagnoseJson,
                                                            String tableStructures,
                                                            String mysqlVersion,
                                                            String ragContext) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("originalSql", defaultText(originalSql));
        variables.put("diagnoseJson", defaultText(diagnoseJson));
        variables.put("tableStructures", defaultText(tableStructures));
        variables.put("mysqlVersion", defaultText(mysqlVersion, "8.0"));
        variables.put("ragSection", AiPromptSections.ragSection(ragContext));
        return variables;
    }

    private Map<String, Object> buildComparePromptVariables(String beforeSql,
                                                            String afterSql,
                                                            String beforeExplain,
                                                            String afterExplain,
                                                            String explainFormat) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("explainFormat", defaultText(explainFormat, "TABLE"));
        variables.put("beforeSql", defaultText(beforeSql));
        variables.put("beforeExplain", defaultText(beforeExplain));
        variables.put("afterSql", defaultText(afterSql));
        variables.put("afterExplain", defaultText(afterExplain));
        return variables;
    }

    private SqlAnalyzeResult parseAnalyzeResult(String sceneName,
                                                AiPromptSpec promptSpec,
                                                String response,
                                                String explainResult) {
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null || AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure(sceneName, promptSpec, "invalid_json");
            return SqlAnalyzeResult.fallbackResult(explainResult);
        }

        var validation = promptSpec == SqlOptimizePromptSpecs.ANALYZE_V2
                ? SqlStructuredOutputSpecs.ANALYZE_V2.validateObject(json)
                : SqlStructuredOutputSpecs.ANALYZE.validateObject(json);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure(sceneName, promptSpec, validation.reason());
            return SqlAnalyzeResult.fallbackResult(explainResult);
        }

        SqlAnalyzeResult result = new SqlAnalyzeResult();
        result.setScore(AiJsonResponseParser.getInt(json, "score", 50));
        result.setOptimizedSql(json.getStr("optimizedSql"));
        result.setKnowledgePoints(json.getBeanList("knowledgePoints", String.class));

        List<SqlAnalyzeResult.Problem> problems = new ArrayList<>();
        JSONArray problemsArray = json.getJSONArray("problems");
        if (problemsArray != null) {
            for (int i = 0; i < problemsArray.size(); i++) {
                JSONObject item = problemsArray.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                problems.add(new SqlAnalyzeResult.Problem()
                        .setType(item.getStr("type"))
                        .setSeverity(item.getStr("severity"))
                        .setDescription(item.getStr("description"))
                        .setLocation(item.getStr("location")));
            }
        }
        result.setProblems(problems);

        List<SqlAnalyzeResult.ExplainAnalysis> explainAnalysis = new ArrayList<>();
        JSONArray explainArray = json.getJSONArray("explainAnalysis");
        if (explainArray != null) {
            for (int i = 0; i < explainArray.size(); i++) {
                JSONObject item = explainArray.getJSONObject(i);
                if (item == null) {
                    continue;
                }
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

        List<SqlAnalyzeResult.Suggestion> suggestions = new ArrayList<>();
        JSONArray suggestionsArray = json.getJSONArray("suggestions");
        if (suggestionsArray != null) {
            for (int i = 0; i < suggestionsArray.size(); i++) {
                JSONObject item = suggestionsArray.getJSONObject(i);
                if (item == null) {
                    continue;
                }
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
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null || AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure("sql_optimize_rewrite_v2", SqlOptimizePromptSpecs.REWRITE, "invalid_json");
            return SqlRewriteResult.fallbackResult(originalSql);
        }

        var validation = SqlStructuredOutputSpecs.REWRITE.validateObject(json);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure("sql_optimize_rewrite_v2", SqlOptimizePromptSpecs.REWRITE, validation.reason());
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
        JSONObject json = AiJsonResponseParser.parse(response);
        if (json == null || AiJsonResponseParser.isErrorResponse(response)) {
            aiMetricsRecorder.recordStructuredParseFailure("sql_optimize_compare_v2", SqlOptimizePromptSpecs.COMPARE, "invalid_json");
            return SqlCompareResult.fallbackResult();
        }

        var validation = SqlStructuredOutputSpecs.COMPARE.validateObject(json);
        if (!validation.valid()) {
            aiMetricsRecorder.recordStructuredParseFailure("sql_optimize_compare_v2", SqlOptimizePromptSpecs.COMPARE, validation.reason());
            return SqlCompareResult.fallbackResult();
        }

        SqlCompareResult result = new SqlCompareResult();
        result.setImprovementScore(AiJsonResponseParser.getInt(json, "improvementScore", 0));
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

    private String defaultText(String value) {
        return defaultText(value, "");
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
