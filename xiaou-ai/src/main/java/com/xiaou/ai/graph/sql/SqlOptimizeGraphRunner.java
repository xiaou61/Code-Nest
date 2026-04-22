package com.xiaou.ai.graph.sql;

import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.sql.SqlAnalyzeResult;
import com.xiaou.ai.dto.sql.SqlAnalyzeRewriteResult;
import com.xiaou.ai.dto.sql.SqlCompareResult;
import com.xiaou.ai.dto.sql.SqlRewriteResult;
import com.xiaou.ai.prompt.AiPromptSections;
import com.xiaou.ai.prompt.sql.SqlOptimizeRagQuerySpecs;
import com.xiaou.ai.rag.AiRagRetrievalProfile;
import com.xiaou.ai.rag.LlamaIndexClient;
import com.xiaou.ai.rag.LlamaIndexRetrieveRequest;
import com.xiaou.ai.rag.LlamaIndexRetrieveResponse;
import com.xiaou.ai.rag.sql.SqlRagRetrievalProfiles;
import com.xiaou.ai.scene.sql.SqlAnalyzeMode;
import com.xiaou.ai.scene.sql.SqlOptimizeSceneSupport;
import com.xiaou.common.exception.ai.AiGraphExecutionException;
import com.xiaou.common.exception.ai.AiRetrievalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 优化图编排执行器。
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlOptimizeGraphRunner {

    private static final String START = "__START__";
    private static final String END = "__END__";

    private final SqlOptimizeSceneSupport sceneSupport;
    private final LlamaIndexClient llamaIndexClient;

    private volatile CompiledGraph<SqlOptimizeState> analyzeGraph;
    private volatile CompiledGraph<SqlOptimizeState> analyzeRewriteGraph;
    private volatile CompiledGraph<SqlOptimizeState> rewriteGraph;
    private volatile CompiledGraph<SqlOptimizeState> compareGraph;

    public SqlAnalyzeResult runAnalyze(SqlAnalyzeMode mode,
                                       String sql,
                                       String explainResult,
                                       String explainFormat,
                                       String tableStructures,
                                       String mysqlVersion) {
        Map<String, Object> input = new HashMap<>();
        input.put(SqlOptimizeState.ANALYZE_MODE, mode.name());
        input.put(SqlOptimizeState.SQL, sql);
        input.put(SqlOptimizeState.EXPLAIN_RESULT, explainResult);
        input.put(SqlOptimizeState.EXPLAIN_FORMAT, explainFormat);
        input.put(SqlOptimizeState.TABLE_STRUCTURES, tableStructures);
        input.put(SqlOptimizeState.MYSQL_VERSION, mysqlVersion);

        SqlOptimizeState state = invokeGraph(getAnalyzeGraph(), input, "sql_analyze_graph");
        return parseAnalyzeResult(state.analyzeResultJson(), explainResult);
    }

    public SqlAnalyzeRewriteResult runAnalyzeAndRewrite(SqlAnalyzeMode mode,
                                                        String sql,
                                                        String explainResult,
                                                        String explainFormat,
                                                        String tableStructures,
                                                        String mysqlVersion) {
        Map<String, Object> input = new HashMap<>();
        input.put(SqlOptimizeState.ANALYZE_MODE, mode.name());
        input.put(SqlOptimizeState.SQL, sql);
        input.put(SqlOptimizeState.ORIGINAL_SQL, sql);
        input.put(SqlOptimizeState.EXPLAIN_RESULT, explainResult);
        input.put(SqlOptimizeState.EXPLAIN_FORMAT, explainFormat);
        input.put(SqlOptimizeState.TABLE_STRUCTURES, tableStructures);
        input.put(SqlOptimizeState.MYSQL_VERSION, mysqlVersion);

        SqlOptimizeState state = invokeGraph(getAnalyzeRewriteGraph(), input, "sql_analyze_rewrite_graph");
        SqlAnalyzeResult analyzeResult = parseAnalyzeResult(state.analyzeResultJson(), explainResult);
        SqlRewriteResult rewriteResult = parseRewriteResult(state.rewriteResultJson(), sql);
        return new SqlAnalyzeRewriteResult()
                .setAnalysis(analyzeResult)
                .setRewrite(rewriteResult)
                .setFallback(analyzeResult.isFallback() || rewriteResult.isFallback());
    }

    public SqlRewriteResult runRewrite(String originalSql,
                                       String diagnoseJson,
                                       String tableStructures,
                                       String mysqlVersion) {
        Map<String, Object> input = new HashMap<>();
        input.put(SqlOptimizeState.ORIGINAL_SQL, originalSql);
        input.put(SqlOptimizeState.DIAGNOSE_JSON, diagnoseJson);
        input.put(SqlOptimizeState.TABLE_STRUCTURES, tableStructures);
        input.put(SqlOptimizeState.MYSQL_VERSION, mysqlVersion);

        SqlOptimizeState state = invokeGraph(getRewriteGraph(), input, "sql_rewrite_graph");
        return parseRewriteResult(state.rewriteResultJson(), originalSql);
    }

    public SqlCompareResult runCompare(String beforeSql,
                                       String afterSql,
                                       String beforeExplain,
                                       String afterExplain,
                                       String explainFormat) {
        Map<String, Object> input = new HashMap<>();
        input.put(SqlOptimizeState.BEFORE_SQL, beforeSql);
        input.put(SqlOptimizeState.AFTER_SQL, afterSql);
        input.put(SqlOptimizeState.BEFORE_EXPLAIN, beforeExplain);
        input.put(SqlOptimizeState.AFTER_EXPLAIN, afterExplain);
        input.put(SqlOptimizeState.EXPLAIN_FORMAT, explainFormat);

        SqlOptimizeState state = invokeGraph(getCompareGraph(), input, "sql_compare_graph");
        return parseCompareResult(state.compareResultJson());
    }

    private Map<String, Object> retrieveContext(SqlOptimizeState state) {
        if (!llamaIndexClient.isAvailable()) {
            return update(SqlOptimizeState.RAG_CONTEXT, "");
        }

        AiRagRetrievalProfile retrievalProfile = resolveRetrievalProfile(state);
        String query = buildRetrieveQuery(state);
        if (retrievalProfile == null || !StringUtils.hasText(query)) {
            return update(SqlOptimizeState.RAG_CONTEXT, "");
        }

        try {
            LlamaIndexRetrieveRequest request = retrievalProfile.buildRequest(query);
            LlamaIndexRetrieveResponse response = llamaIndexClient.retrieve(request);
            return update(SqlOptimizeState.RAG_CONTEXT, response.toContextSnippet());
        } catch (AiRetrievalException e) {
            log.warn("SQL 图编排检索增强不可用，继续执行纯模型链路: {}", e.getMessage());
            return update(SqlOptimizeState.RAG_CONTEXT, "");
        }
    }

    private AiRagRetrievalProfile resolveRetrievalProfile(SqlOptimizeState state) {
        if (StringUtils.hasText(state.sql())) {
            return SqlRagRetrievalProfiles.ANALYZE;
        }
        if (StringUtils.hasText(state.originalSql())) {
            return SqlRagRetrievalProfiles.REWRITE;
        }
        return null;
    }

    private Map<String, Object> analyze(SqlOptimizeState state) {
        SqlAnalyzeResult result = sceneSupport.analyze(
                state.analyzeMode(),
                state.sql(),
                state.explainResult(),
                state.explainFormat(),
                state.tableStructures(),
                state.mysqlVersion(),
                state.ragContext()
        );

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(SqlOptimizeState.ANALYZE_RESULT_JSON, JSONUtil.toJsonStr(result));
        updates.put(SqlOptimizeState.DIAGNOSE_JSON, JSONUtil.toJsonStr(result));
        return updates;
    }

    private Map<String, Object> rewrite(SqlOptimizeState state) {
        String originalSql = StringUtils.hasText(state.originalSql()) ? state.originalSql() : state.sql();
        String diagnoseJson = StringUtils.hasText(state.diagnoseJson()) ? state.diagnoseJson() : state.analyzeResultJson();
        SqlRewriteResult result = sceneSupport.rewrite(
                originalSql,
                diagnoseJson,
                state.tableStructures(),
                state.mysqlVersion(),
                state.ragContext()
        );
        return update(SqlOptimizeState.REWRITE_RESULT_JSON, JSONUtil.toJsonStr(result));
    }

    private Map<String, Object> riskCheck(SqlOptimizeState state) {
        SqlRewriteResult rewriteResult = parseRewriteResult(state.rewriteResultJson(), state.originalSql());
        List<String> riskWarnings = rewriteResult.getRiskWarnings() == null
                ? new ArrayList<>()
                : new ArrayList<>(rewriteResult.getRiskWarnings());

        if (riskWarnings.isEmpty()) {
            riskWarnings.add("建议先在测试环境验证执行计划、结果集一致性与索引维护成本");
        }
        if (StringUtils.hasText(rewriteResult.getOptimizedSql())
                && StringUtils.hasText(state.originalSql())
                && rewriteResult.getOptimizedSql().trim().equalsIgnoreCase(state.originalSql().trim())) {
            riskWarnings.add("当前重写结果与原 SQL 基本一致，建议重点关注索引设计和执行计划变化");
        }

        rewriteResult.setRiskWarnings(riskWarnings.stream().distinct().toList());
        return update(SqlOptimizeState.REWRITE_RESULT_JSON, JSONUtil.toJsonStr(rewriteResult));
    }

    private Map<String, Object> compare(SqlOptimizeState state) {
        SqlCompareResult result = sceneSupport.compare(
                state.beforeSql(),
                state.afterSql(),
                state.beforeExplain(),
                state.afterExplain(),
                state.explainFormat()
        );
        return update(SqlOptimizeState.COMPARE_RESULT_JSON, JSONUtil.toJsonStr(result));
    }

    private SqlOptimizeState invokeGraph(CompiledGraph<SqlOptimizeState> graph,
                                         Map<String, Object> input,
                                         String graphName) {
        try {
            return graph.invoke(input)
                    .orElseThrow(() -> new AiGraphExecutionException("图编排执行未返回最终状态: " + graphName));
        } catch (AiGraphExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new AiGraphExecutionException("图编排执行失败: " + graphName, e);
        }
    }

    private SqlAnalyzeResult parseAnalyzeResult(String json, String explainResult) {
        if (!StringUtils.hasText(json)) {
            return SqlAnalyzeResult.fallbackResult(explainResult);
        }
        try {
            return JSONUtil.toBean(json, SqlAnalyzeResult.class);
        } catch (Exception e) {
            log.warn("解析图编排分析结果失败，返回降级结果", e);
            return SqlAnalyzeResult.fallbackResult(explainResult);
        }
    }

    private SqlRewriteResult parseRewriteResult(String json, String originalSql) {
        if (!StringUtils.hasText(json)) {
            return SqlRewriteResult.fallbackResult(originalSql);
        }
        try {
            return JSONUtil.toBean(json, SqlRewriteResult.class);
        } catch (Exception e) {
            log.warn("解析图编排重写结果失败，返回降级结果", e);
            return SqlRewriteResult.fallbackResult(originalSql);
        }
    }

    private SqlCompareResult parseCompareResult(String json) {
        if (!StringUtils.hasText(json)) {
            return SqlCompareResult.fallbackResult();
        }
        try {
            return JSONUtil.toBean(json, SqlCompareResult.class);
        } catch (Exception e) {
            log.warn("解析图编排对比结果失败，返回降级结果", e);
            return SqlCompareResult.fallbackResult();
        }
    }

    private CompiledGraph<SqlOptimizeState> getAnalyzeGraph() {
        CompiledGraph<SqlOptimizeState> local = analyzeGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (analyzeGraph == null) {
                analyzeGraph = buildAnalyzeGraph();
            }
            return analyzeGraph;
        }
    }

    private CompiledGraph<SqlOptimizeState> getAnalyzeRewriteGraph() {
        CompiledGraph<SqlOptimizeState> local = analyzeRewriteGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (analyzeRewriteGraph == null) {
                analyzeRewriteGraph = buildAnalyzeRewriteGraph();
            }
            return analyzeRewriteGraph;
        }
    }

    private CompiledGraph<SqlOptimizeState> getRewriteGraph() {
        CompiledGraph<SqlOptimizeState> local = rewriteGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (rewriteGraph == null) {
                rewriteGraph = buildRewriteGraph();
            }
            return rewriteGraph;
        }
    }

    private CompiledGraph<SqlOptimizeState> getCompareGraph() {
        CompiledGraph<SqlOptimizeState> local = compareGraph;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (compareGraph == null) {
                compareGraph = buildCompareGraph();
            }
            return compareGraph;
        }
    }

    private CompiledGraph<SqlOptimizeState> buildAnalyzeGraph() {
        try {
            StateGraph<SqlOptimizeState> graph = new StateGraph<>(channels(), SqlOptimizeState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("analyze", AsyncNodeAction.node_async(this::analyze));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "analyze");
            graph.addEdge("analyze", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建 SQL 分析图失败", e);
        }
    }

    private CompiledGraph<SqlOptimizeState> buildAnalyzeRewriteGraph() {
        try {
            StateGraph<SqlOptimizeState> graph = new StateGraph<>(channels(), SqlOptimizeState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("analyze", AsyncNodeAction.node_async(this::analyze));
            graph.addNode("rewrite", AsyncNodeAction.node_async(this::rewrite));
            graph.addNode("risk_check", AsyncNodeAction.node_async(this::riskCheck));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "analyze");
            graph.addEdge("analyze", "rewrite");
            graph.addEdge("rewrite", "risk_check");
            graph.addEdge("risk_check", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建 SQL 分析重写图失败", e);
        }
    }

    private CompiledGraph<SqlOptimizeState> buildRewriteGraph() {
        try {
            StateGraph<SqlOptimizeState> graph = new StateGraph<>(channels(), SqlOptimizeState::new);
            graph.addNode("retrieve_context", AsyncNodeAction.node_async(this::retrieveContext));
            graph.addNode("rewrite", AsyncNodeAction.node_async(this::rewrite));
            graph.addNode("risk_check", AsyncNodeAction.node_async(this::riskCheck));
            graph.addEdge(START, "retrieve_context");
            graph.addEdge("retrieve_context", "rewrite");
            graph.addEdge("rewrite", "risk_check");
            graph.addEdge("risk_check", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建 SQL 重写图失败", e);
        }
    }

    private CompiledGraph<SqlOptimizeState> buildCompareGraph() {
        try {
            StateGraph<SqlOptimizeState> graph = new StateGraph<>(channels(), SqlOptimizeState::new);
            graph.addNode("compare", AsyncNodeAction.node_async(this::compare));
            graph.addEdge(START, "compare");
            graph.addEdge("compare", END);
            return graph.compile();
        } catch (Exception e) {
            throw new AiGraphExecutionException("构建 SQL 对比图失败", e);
        }
    }

    private Map<String, Channel<?>> channels() {
        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put(SqlOptimizeState.ANALYZE_MODE, Channels.base(() -> SqlAnalyzeMode.STANDARD.name()));
        channels.put(SqlOptimizeState.SQL, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.EXPLAIN_RESULT, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.EXPLAIN_FORMAT, Channels.base(() -> "TABLE"));
        channels.put(SqlOptimizeState.TABLE_STRUCTURES, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.MYSQL_VERSION, Channels.base(() -> "8.0"));
        channels.put(SqlOptimizeState.RAG_CONTEXT, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.DIAGNOSE_JSON, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.ANALYZE_RESULT_JSON, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.REWRITE_RESULT_JSON, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.COMPARE_RESULT_JSON, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.ORIGINAL_SQL, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.BEFORE_SQL, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.AFTER_SQL, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.BEFORE_EXPLAIN, Channels.base(() -> ""));
        channels.put(SqlOptimizeState.AFTER_EXPLAIN, Channels.base(() -> ""));
        return channels;
    }

    private Map<String, Object> update(String key, Object value) {
        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(key, value);
        return updates;
    }

    private String buildRetrieveQuery(SqlOptimizeState state) {
        if (StringUtils.hasText(state.sql())) {
            return SqlOptimizeRagQuerySpecs.ANALYZE.render(Map.of(
                    "sql", AiPromptSections.text(state.sql()),
                    "explainResult", AiPromptSections.text(state.explainResult())
            ));
        }
        if (StringUtils.hasText(state.originalSql())) {
            return SqlOptimizeRagQuerySpecs.REWRITE.render(Map.of(
                    "originalSql", AiPromptSections.text(state.originalSql()),
                    "diagnoseJson", AiPromptSections.text(state.diagnoseJson())
            ));
        }
        return "";
    }

}
