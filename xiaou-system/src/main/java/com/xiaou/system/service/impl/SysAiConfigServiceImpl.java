package com.xiaou.system.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaou.ai.client.AiChatResult;
import com.xiaou.ai.prompt.AiPromptCatalog;
import com.xiaou.ai.prompt.AiPromptSpec;
import com.xiaou.ai.prompt.AiRagQueryCatalog;
import com.xiaou.ai.prompt.AiRagQuerySpec;
import com.xiaou.ai.rag.AiRagRetrievalCatalog;
import com.xiaou.ai.rag.AiRagRetrievalProfile;
import com.xiaou.ai.regression.AiRegressionCaseCatalogItem;
import com.xiaou.ai.regression.AiRegressionCaseResult;
import com.xiaou.ai.regression.AiRegressionRunSummary;
import com.xiaou.ai.regression.AiRegressionService;
import com.xiaou.ai.rag.LlamaIndexDocumentBatchDeleteRequest;
import com.xiaou.ai.rag.LlamaIndexDocumentBatchDeleteResponse;
import com.xiaou.ai.rag.LlamaIndexDocumentDeleteResponse;
import com.xiaou.ai.rag.LlamaIndexDocumentExportResponse;
import com.xiaou.ai.rag.LlamaIndexDocumentImportRequest;
import com.xiaou.ai.rag.LlamaIndexDocumentImportResponse;
import com.xiaou.ai.rag.LlamaIndexDocumentListItem;
import com.xiaou.ai.rag.LlamaIndexDocumentListResponse;
import com.xiaou.ai.rag.LlamaIndexHealthResponse;
import com.xiaou.ai.rag.LlamaIndexKnowledgeDocument;
import com.xiaou.ai.rag.LlamaIndexClient;
import com.xiaou.ai.rag.LlamaIndexRetrieveRequest;
import com.xiaou.ai.rag.LlamaIndexRetrieveResponse;
import com.xiaou.ai.structured.AiStructuredOutputCatalog;
import com.xiaou.ai.structured.AiStructuredOutputSpec;
import com.xiaou.ai.structured.AiStructuredOutputValidator;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.ai.util.AiJsonResponseParser;
import com.xiaou.ai.metrics.AiRuntimeMetricsCollector;
import com.xiaou.ai.metrics.AiRuntimeModelMetrics;
import com.xiaou.ai.metrics.AiRuntimeMetricsSnapshot;
import com.xiaou.ai.metrics.AiRuntimeRecentCall;
import com.xiaou.ai.metrics.AiRuntimeSceneMetrics;
import com.xiaou.common.config.AiProperties;
import com.xiaou.system.dto.AiConfigTestRequest;
import com.xiaou.system.dto.AiConfigTestResponse;
import com.xiaou.system.dto.AiPromptDebugRequest;
import com.xiaou.system.dto.AiPromptDebugResponse;
import com.xiaou.system.dto.AiRegressionCaseCatalogItemResponse;
import com.xiaou.system.dto.AiRegressionCaseCatalogResponse;
import com.xiaou.system.dto.AiRegressionCaseResultResponse;
import com.xiaou.system.dto.AiRegressionInsightItemResponse;
import com.xiaou.system.dto.AiRegressionRunHistoryResponse;
import com.xiaou.system.dto.AiRegressionRunRequest;
import com.xiaou.system.dto.AiRegressionRunResponse;
import com.xiaou.system.dto.AiRegressionScenarioHealthItemResponse;
import com.xiaou.system.dto.AiRegressionScenarioHealthResponse;
import com.xiaou.system.dto.AiRagDocumentBatchDeleteRequest;
import com.xiaou.system.dto.AiRagDocumentBatchDeleteResponse;
import com.xiaou.system.dto.AiRagDocumentDeleteResponse;
import com.xiaou.system.dto.AiRagDocumentExportItemResponse;
import com.xiaou.system.dto.AiRagDocumentExportResponse;
import com.xiaou.system.dto.AiRagDocumentImportRequest;
import com.xiaou.system.dto.AiRagDocumentImportResponse;
import com.xiaou.system.dto.AiPromptCatalogItemResponse;
import com.xiaou.system.dto.AiRagDebugNodeResponse;
import com.xiaou.system.dto.AiRagDebugRequest;
import com.xiaou.system.dto.AiRagDebugResponse;
import com.xiaou.system.dto.AiRagSampleImportRequest;
import com.xiaou.system.dto.AiRagSampleImportResponse;
import com.xiaou.system.dto.AiRagServiceDocumentItemResponse;
import com.xiaou.system.dto.AiRagServiceDocumentListResponse;
import com.xiaou.system.dto.AiRagServiceHealthResponse;
import com.xiaou.system.dto.AiRagQueryCatalogItemResponse;
import com.xiaou.system.dto.AiRetrievalProfileResponse;
import com.xiaou.system.dto.AiRuntimeConfigResponse;
import com.xiaou.system.dto.AiRuntimeModelMetricsResponse;
import com.xiaou.system.dto.AiRuntimeMetricsOverviewResponse;
import com.xiaou.system.dto.AiRuntimeMetricsResponse;
import com.xiaou.system.dto.AiRuntimeRecentCallResponse;
import com.xiaou.system.dto.AiRuntimeSceneMetricsResponse;
import com.xiaou.system.dto.AiSchemaCatalogResponse;
import com.xiaou.system.dto.AiStructuredSchemaResponse;
import com.xiaou.system.service.SysAiConfigService;
import com.xiaou.system.service.impl.support.AiRegressionRunStateRepository;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 管理后台 AI 配置服务实现。
 *
 * <p>当前仅支持 OpenAI 兼容中转站能力测试，不进行密钥持久化。</p>
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAiConfigServiceImpl implements SysAiConfigService {

    private static final String DEFAULT_PROVIDER = "openai-compatible";
    private static final String PROMPT_DEBUG_SCENE = "admin.prompt_debug";
    private static final String DEFAULT_SUCCESS_MESSAGE = "连通性测试成功";
    private static final String DEFAULT_FAILURE_MESSAGE = "连通性测试失败";
    private static final String TEST_PROMPT = "你是连通性测试助手，请只返回 OK。";
    private static final int PREVIEW_MAX_LENGTH = 160;
    private static final int ERROR_MAX_LENGTH = 240;
    private static final int API_KEY_PREFIX_LENGTH = 6;
    private static final int API_KEY_SUFFIX_LENGTH = 4;
    private static final int DEFAULT_RECENT_LIMIT = 50;
    private static final int MAX_RECENT_LIMIT = 50;
    private static final int DEFAULT_REGRESSION_HISTORY_LIMIT = 10;
    private static final int MAX_REGRESSION_HISTORY_LIMIT = 20;
    private static final int SCENARIO_HEALTH_INSIGHT_LIMIT = 5;
    private static final int DEFAULT_RAG_DOCUMENT_LIMIT = 20;
    private static final int MAX_RAG_DOCUMENT_LIMIT = 200;
    private static final String SAMPLE_RAG_DOCUMENTS_RESOURCE = "ai/rag/sample-documents.json";
    private static final String DEFAULT_FAILURE_REASON_LABEL = "未提供失败原因";
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._\\-]+");
    private static final Set<String> SUPPORTED_OUTCOMES = Set.of("success", "error");

    private final AiProperties aiProperties;
    private final AiRuntimeMetricsCollector runtimeMetricsCollector;
    private final AiExecutionSupport aiExecutionSupport;
    private final AiRegressionService aiRegressionService;
    private final LlamaIndexClient llamaIndexClient;
    private final AiRegressionRunStateRepository aiRegressionRunStateRepository;

    @Override
    public AiRuntimeConfigResponse getRuntimeConfig() {
        AiRuntimeConfigResponse response = new AiRuntimeConfigResponse();
        String provider = normalizeProvider(aiProperties.getProvider());
        String baseUrl = normalizeBaseUrl(aiProperties.getBaseUrl());
        String model = resolveModelName(null);

        response.setProvider(provider);
        response.setBaseUrl(baseUrl);
        response.setModel(model);
        response.setApiKeyConfigured(aiProperties.hasApiKey());
        response.setApiKeyMasked(maskApiKey(aiProperties.getApiKey()));
        response.setConfigured(StringUtils.hasText(baseUrl)
                && aiProperties.hasApiKey()
                && StringUtils.hasText(model));
        response.setPricingConfigured(aiProperties.getPricing() != null && aiProperties.getPricing().configured());
        response.setPricingCurrency(aiProperties.getPricing() == null ? "USD" : normalizeCurrency(aiProperties.getPricing().getCurrency()));
        response.setInputPricePerMillion(decimal(aiProperties.getPricing() == null ? null : aiProperties.getPricing().getInputPerMillion()));
        response.setOutputPricePerMillion(decimal(aiProperties.getPricing() == null ? null : aiProperties.getPricing().getOutputPerMillion()));
        response.setMetricsPersistenceEnabled(runtimeMetricsCollector.isPersistenceEnabled());
        response.setMetricsPersistenceMode(runtimeMetricsCollector.persistenceMode());
        response.setRagEnabled(aiProperties.getRag() != null && aiProperties.getRag().isEnabled());
        response.setRagEndpoint(normalizeBaseUrl(aiProperties.getRag() == null ? null : aiProperties.getRag().getEndpoint()));
        response.setRagApiKeyConfigured(aiProperties.getRag() != null && StringUtils.hasText(aiProperties.getRag().getApiKey()));
        response.setRagApiKeyMasked(maskApiKey(aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()));
        response.setRagDefaultTopK(aiProperties.getRag() == null ? 0 : aiProperties.getRag().getDefaultTopK());
        return response;
    }

    @Override
    public AiSchemaCatalogResponse getSchemaCatalog() {
        LinkedHashSet<String> domains = new LinkedHashSet<>();
        AiSchemaCatalogResponse response = new AiSchemaCatalogResponse();
        response.setPrompts(mapPromptCatalog(domains));
        response.setRagQueries(mapRagQueryCatalog(domains));
        response.setRetrievalProfiles(mapRetrievalProfiles(domains));
        response.setStructuredSchemas(mapStructuredSchemas(domains));
        response.setDomains(new ArrayList<>(domains));
        return response;
    }

    @Override
    public AiRegressionCaseCatalogResponse getRegressionCaseCatalog() {
        List<AiRegressionCaseCatalogItem> cases = aiRegressionService.listCases();
        AiRegressionCaseCatalogResponse response = new AiRegressionCaseCatalogResponse();
        response.setCases(mapRegressionCatalogItems(cases));
        response.setTotalCount(response.getCases().size());
        response.setScenarios(cases.stream()
                .map(AiRegressionCaseCatalogItem::getScenario)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));
        return response;
    }

    @Override
    public AiRegressionRunResponse runRegression(AiRegressionRunRequest request) {
        AiRegressionRunSummary summary = aiRegressionService.run(
                request == null ? null : request.getScenario(),
                request == null ? null : request.getCaseId()
        );
        AiRegressionRunResponse response = mapRegressionRun(summary);
        aiRegressionRunStateRepository.saveLatest(response);
        return response;
    }

    @Override
    public AiRegressionRunResponse getLatestRegressionRun() {
        return aiRegressionRunStateRepository.loadLatest();
    }

    @Override
    public AiRegressionRunHistoryResponse getRegressionRunHistory(Integer limit) {
        int resolvedLimit = resolveRegressionHistoryLimit(limit);
        List<AiRegressionRunResponse> runs = aiRegressionRunStateRepository.loadHistory(resolvedLimit);
        AiRegressionRunHistoryResponse response = new AiRegressionRunHistoryResponse();
        response.setLimit(resolvedLimit);
        response.setTotalCount(runs.size());
        response.setRuns(runs);
        return response;
    }

    @Override
    public AiRegressionScenarioHealthResponse getRegressionScenarioHealth(Integer limit) {
        int resolvedLimit = resolveRegressionHistoryLimit(limit);
        List<AiRegressionRunResponse> runs = aiRegressionRunStateRepository.loadHistory(resolvedLimit);
        AiRegressionScenarioHealthResponse response = new AiRegressionScenarioHealthResponse();
        response.setLimit(resolvedLimit);
        response.setScenarios(mapRegressionScenarioHealth(runs));
        response.setTotalCount(response.getScenarios().size());
        return response;
    }

    @Override
    public AiPromptDebugResponse debugPrompt(AiPromptDebugRequest request) {
        if (request == null || !StringUtils.hasText(request.getPromptId())) {
            throw new IllegalArgumentException("promptId 不能为空");
        }

        AiPromptSpec promptSpec = findPromptSpec(request.getPromptId());
        Map<String, Object> variables = parseVariables(request.getVariablesJson());
        AiStructuredOutputSpec structuredSpec = findStructuredOutputSpec(promptSpec.promptId());

        AiPromptDebugResponse response = new AiPromptDebugResponse();
        response.setDomain(resolveDomain(promptSpec.key()));
        response.setPromptId(promptSpec.promptId());
        response.setPromptKey(promptSpec.key());
        response.setPromptVersion(promptSpec.version());
        response.setTemplateVariables(toSortedList(promptSpec.templateVariables()));
        response.setVariablesJson(JSONUtil.toJsonPrettyStr(variables));
        response.setSystemPrompt(promptSpec.systemPrompt());
        response.setRenderedUserPrompt(promptSpec.renderUser(variables));
        response.setStructuredOutputBound(structuredSpec != null);

        if (structuredSpec != null) {
            response.setSchemaRootType(structuredSpec.rootType().name());
            response.setSchemaId(structuredSpec.schemaId());
            response.setSchemaJson(JSONUtil.toJsonPrettyStr(structuredSpec.jsonSchema()));
        }

        if (!Boolean.TRUE.equals(request.getExecute())) {
            response.setExecuted(false);
            return response;
        }

        AiChatResult result = aiExecutionSupport.chatResult(PROMPT_DEBUG_SCENE, promptSpec, variables);
        response.setExecuted(true);
        response.setModelName(result.getModelName());
        response.setRawResponse(result.getContent());
        response.setInputTokens(result.getTokenUsage() == null ? null : result.getTokenUsage().inputTokenCount());
        response.setOutputTokens(result.getTokenUsage() == null ? null : result.getTokenUsage().outputTokenCount());
        response.setTotalTokens(result.getTokenUsage() == null ? null : result.getTokenUsage().totalTokenCount());

        if (structuredSpec != null) {
            validateStructuredResponse(response, structuredSpec, result.getContent());
        }

        return response;
    }

    @Override
    public AiRagDebugResponse debugRag(AiRagDebugRequest request) {
        if (request == null || !StringUtils.hasText(request.getQuery())) {
            throw new IllegalArgumentException("query 不能为空");
        }

        AiRagRetrievalProfile retrievalProfile = findRetrievalProfile(request.getProfileId());
        Map<String, Object> metadataFilters = resolveMetadataFilters(request.getMetadataFiltersJson(), retrievalProfile);
        String scene = resolveRagScene(request.getScene(), retrievalProfile);
        Integer topK = resolveRagTopK(request.getTopK(), retrievalProfile);

        AiRagDebugResponse response = new AiRagDebugResponse();
        response.setProfileId(retrievalProfile == null ? null : retrievalProfile.profileId());
        response.setRagEnabled(aiProperties.getRag() != null && aiProperties.getRag().isEnabled());
        response.setEndpoint(normalizeBaseUrl(aiProperties.getRag() == null ? null : aiProperties.getRag().getEndpoint()));
        response.setApiKeyConfigured(aiProperties.getRag() != null && StringUtils.hasText(aiProperties.getRag().getApiKey()));
        response.setApiKeyMasked(maskApiKey(aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()));
        response.setQuery(request.getQuery().trim());
        response.setScene(scene);
        response.setTopK(topK);
        response.setMetadataFiltersJson(JSONUtil.toJsonPrettyStr(metadataFilters));

        long startNanos = System.nanoTime();
        try {
            LlamaIndexRetrieveResponse retrieveResponse = llamaIndexClient.retrieve(new LlamaIndexRetrieveRequest()
                    .setQuery(request.getQuery().trim())
                    .setScene(scene)
                    .setTopK(topK)
                    .setMetadataFilters(new LinkedHashMap<>(metadataFilters)));

            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            response.setFallback(retrieveResponse.isFallback());
            response.setContextSnippet(retrieveResponse.toContextSnippet());
            response.setNodes(mapRagNodes(retrieveResponse));
            response.setNodeCount(response.getNodes().size());
            return response;
        } catch (Exception e) {
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            throw new IllegalArgumentException("LlamaIndex 检索调试失败：" + sanitizeSensitiveMessage(
                    e.getMessage(),
                    aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()
            ));
        }
    }

    @Override
    public AiRagServiceHealthResponse getRagServiceHealth() {
        AiRagServiceHealthResponse response = new AiRagServiceHealthResponse();
        response.setRagEnabled(aiProperties.getRag() != null && aiProperties.getRag().isEnabled());
        response.setEndpoint(normalizeBaseUrl(aiProperties.getRag() == null ? null : aiProperties.getRag().getEndpoint()));
        response.setApiKeyConfigured(aiProperties.getRag() != null && StringUtils.hasText(aiProperties.getRag().getApiKey()));
        response.setApiKeyMasked(maskApiKey(aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()));

        if (!llamaIndexClient.hasEndpointConfigured()) {
            response.setReachable(false);
            response.setMessage("RAG endpoint 未配置");
            return response;
        }

        long startNanos = System.nanoTime();
        try {
            LlamaIndexHealthResponse healthResponse = llamaIndexClient.health();
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            response.setReachable("ok".equalsIgnoreCase(healthResponse.getStatus()));
            response.setStatus(healthResponse.getStatus());
            response.setAuthEnabled(healthResponse.isAuthEnabled());
            response.setDocumentCount(healthResponse.getDocumentCount());
            response.setSceneCount(healthResponse.getSceneCount());
            response.setDataFile(healthResponse.getDataFile());
            response.setMessage(response.isReachable() ? "RAG 服务可达" : "RAG 服务返回异常状态");
            return response;
        } catch (Exception e) {
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            response.setReachable(false);
            response.setMessage("RAG 服务健康检查失败：" + sanitizeSensitiveMessage(
                    e.getMessage(),
                    aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()
            ));
            return response;
        }
    }

    @Override
    public AiRagServiceDocumentListResponse getRagServiceDocuments(String scene, Integer limit) {
        ensureRagEndpointConfigured();

        AiRagServiceDocumentListResponse response = new AiRagServiceDocumentListResponse();
        response.setEndpoint(normalizeBaseUrl(aiProperties.getRag() == null ? null : aiProperties.getRag().getEndpoint()));
        response.setScene(StringUtils.hasText(scene) ? scene.trim() : null);
        response.setLimit(resolveRagDocumentLimit(limit));

        long startNanos = System.nanoTime();
        try {
            LlamaIndexDocumentListResponse listResponse = llamaIndexClient.listDocuments(response.getScene(), response.getLimit());
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            response.setTotalCount(listResponse.getTotalCount());
            response.setDocuments(mapRagServiceDocuments(listResponse));
            return response;
        } catch (Exception e) {
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            throw new IllegalArgumentException("获取 RAG 文档列表失败：" + sanitizeSensitiveMessage(
                    e.getMessage(),
                    aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()
            ));
        }
    }

    @Override
    public AiRagDocumentExportResponse exportRagDocuments(String scene) {
        ensureRagEndpointConfigured();

        AiRagDocumentExportResponse response = new AiRagDocumentExportResponse();
        response.setEndpoint(normalizeBaseUrl(aiProperties.getRag() == null ? null : aiProperties.getRag().getEndpoint()));
        response.setScene(StringUtils.hasText(scene) ? scene.trim() : null);

        long startNanos = System.nanoTime();
        try {
            LlamaIndexDocumentExportResponse exportResponse = llamaIndexClient.exportDocuments(response.getScene());
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            response.setTotalCount(exportResponse.getTotalCount());
            response.setDocuments(mapRagExportDocuments(exportResponse));
            return response;
        } catch (Exception e) {
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            throw new IllegalArgumentException("导出 RAG 文档失败：" + sanitizeSensitiveMessage(
                    e.getMessage(),
                    aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()
            ));
        }
    }

    @Override
    public AiRagSampleImportResponse importRagSampleDocuments(AiRagSampleImportRequest request) {
        ensureRagEndpointConfigured();

        boolean replace = request != null && Boolean.TRUE.equals(request.getReplace());
        List<LlamaIndexKnowledgeDocument> sampleDocuments = loadSampleRagDocuments();

        AiRagSampleImportResponse response = new AiRagSampleImportResponse();
        response.setEndpoint(normalizeBaseUrl(aiProperties.getRag() == null ? null : aiProperties.getRag().getEndpoint()));
        response.setReplace(replace);
        response.setSampleDocumentCount(sampleDocuments.size());

        long startNanos = System.nanoTime();
        try {
            LlamaIndexDocumentImportResponse importResponse = llamaIndexClient.importDocuments(new LlamaIndexDocumentImportRequest()
                    .setReplace(replace)
                    .setDocuments(sampleDocuments));
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            response.setImportedCount(importResponse.getImportedCount());
            response.setTotalCount(importResponse.getTotalCount());
            return response;
        } catch (Exception e) {
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            throw new IllegalArgumentException("导入 RAG 样例知识失败：" + sanitizeSensitiveMessage(
                    e.getMessage(),
                    aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()
            ));
        }
    }

    @Override
    public AiRagDocumentImportResponse importRagDocuments(AiRagDocumentImportRequest request) {
        ensureRagEndpointConfigured();

        boolean replace = request != null && Boolean.TRUE.equals(request.getReplace());
        String defaultScene = StringUtils.hasText(request == null ? null : request.getDefaultScene())
                ? request.getDefaultScene().trim()
                : null;
        List<LlamaIndexKnowledgeDocument> documents = parseCustomRagDocuments(request, defaultScene);

        AiRagDocumentImportResponse response = new AiRagDocumentImportResponse();
        response.setEndpoint(normalizeBaseUrl(aiProperties.getRag() == null ? null : aiProperties.getRag().getEndpoint()));
        response.setReplace(replace);
        response.setDefaultScene(defaultScene);
        response.setRequestedDocumentCount(documents.size());

        long startNanos = System.nanoTime();
        try {
            LlamaIndexDocumentImportResponse importResponse = llamaIndexClient.importDocuments(new LlamaIndexDocumentImportRequest()
                    .setReplace(replace)
                    .setDocuments(documents));
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            response.setImportedCount(importResponse.getImportedCount());
            response.setTotalCount(importResponse.getTotalCount());
            return response;
        } catch (Exception e) {
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            throw new IllegalArgumentException("导入自定义 RAG 文档失败：" + sanitizeSensitiveMessage(
                    e.getMessage(),
                    aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()
            ));
        }
    }

    @Override
    public AiRagDocumentDeleteResponse deleteRagDocument(String documentId) {
        ensureRagEndpointConfigured();

        AiRagDocumentDeleteResponse response = new AiRagDocumentDeleteResponse();
        response.setEndpoint(normalizeBaseUrl(aiProperties.getRag() == null ? null : aiProperties.getRag().getEndpoint()));
        response.setDocumentId(normalizeDocumentId(documentId));

        long startNanos = System.nanoTime();
        try {
            LlamaIndexDocumentDeleteResponse deleteResponse = llamaIndexClient.deleteDocument(response.getDocumentId());
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            response.setDeletedCount(deleteResponse.getDeletedCount());
            response.setTotalCount(deleteResponse.getTotalCount());
            return response;
        } catch (Exception e) {
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            throw new IllegalArgumentException("删除 RAG 文档失败：" + sanitizeSensitiveMessage(
                    e.getMessage(),
                    aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()
            ));
        }
    }

    @Override
    public AiRagDocumentBatchDeleteResponse batchDeleteRagDocuments(AiRagDocumentBatchDeleteRequest request) {
        ensureRagEndpointConfigured();

        List<String> documentIds = normalizeDocumentIds(request == null ? null : request.getDocumentIds());

        AiRagDocumentBatchDeleteResponse response = new AiRagDocumentBatchDeleteResponse();
        response.setEndpoint(normalizeBaseUrl(aiProperties.getRag() == null ? null : aiProperties.getRag().getEndpoint()));

        long startNanos = System.nanoTime();
        try {
            LlamaIndexDocumentBatchDeleteResponse deleteResponse = llamaIndexClient.deleteDocuments(
                    new LlamaIndexDocumentBatchDeleteRequest().setDocumentIds(documentIds)
            );
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            response.setRequestedCount(deleteResponse.getRequestedCount());
            response.setDeletedCount(deleteResponse.getDeletedCount());
            response.setDeletedDocumentIds(deleteResponse.getDeletedDocumentIds() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(deleteResponse.getDeletedDocumentIds()));
            response.setMissingCount(deleteResponse.getMissingCount());
            response.setMissingDocumentIds(deleteResponse.getMissingDocumentIds() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(deleteResponse.getMissingDocumentIds()));
            response.setTotalCount(deleteResponse.getTotalCount());
            return response;
        } catch (Exception e) {
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            throw new IllegalArgumentException("批量删除 RAG 文档失败：" + sanitizeSensitiveMessage(
                    e.getMessage(),
                    aiProperties.getRag() == null ? null : aiProperties.getRag().getApiKey()
            ));
        }
    }

    @Override
    public AiConfigTestResponse testConfig(AiConfigTestRequest request) {
        long startNanos = System.nanoTime();
        AiConfigTestResponse response = new AiConfigTestResponse();
        ResolvedAiTestConfig resolvedConfig = null;

        try {
            resolvedConfig = resolveConfig(request);
            populateResolvedConfig(response, resolvedConfig);

            String content = createChatModel(resolvedConfig).chat(TEST_PROMPT);
            boolean available = StringUtils.hasText(content);

            response.setAvailable(available);
            response.setMessage(available ? DEFAULT_SUCCESS_MESSAGE : "模型返回空响应");
            response.setPreview(compactPreview(content));
        } catch (IllegalArgumentException e) {
            response.setAvailable(false);
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            response.setAvailable(false);
            response.setMessage(DEFAULT_FAILURE_MESSAGE + "：" + sanitizeErrorMessage(
                    e.getMessage(),
                    resolvedConfig == null ? null : resolvedConfig.apiKey()
            ));
            log.warn("AI 配置测试失败: {}", sanitizeErrorMessage(
                    e.getMessage(),
                    resolvedConfig == null ? null : resolvedConfig.apiKey()
            ));
        } finally {
            response.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
        }

        return response;
    }

    @Override
    public AiRuntimeMetricsResponse getRuntimeMetrics(String scene, String outcome, String model, Integer recentLimit) {
        AiRuntimeMetricsSnapshot snapshot = runtimeMetricsCollector.snapshot(
                normalizeSceneKeyword(scene),
                normalizeOutcome(outcome),
                normalizeModelKeyword(model),
                resolveRecentLimit(recentLimit)
        );
        AiRuntimeMetricsResponse response = new AiRuntimeMetricsResponse();

        AiRuntimeMetricsOverviewResponse overview = new AiRuntimeMetricsOverviewResponse();
        overview.setTotalInvocations(snapshot.getOverview().getTotalInvocations());
        overview.setSuccessCount(snapshot.getOverview().getSuccessCount());
        overview.setErrorCount(snapshot.getOverview().getErrorCount());
        overview.setFallbackCount(snapshot.getOverview().getFallbackCount());
        overview.setStructuredParseFailureCount(snapshot.getOverview().getStructuredParseFailureCount());
        overview.setTotalInputTokens(snapshot.getOverview().getTotalInputTokens());
        overview.setTotalOutputTokens(snapshot.getOverview().getTotalOutputTokens());
        overview.setTotalTokens(snapshot.getOverview().getTotalTokens());
        overview.setEstimatedCost(snapshot.getOverview().getEstimatedCost());
        overview.setAverageLatencyMs(snapshot.getOverview().getAverageLatencyMs());
        overview.setLastInvocationAt(snapshot.getOverview().getLastInvocationAt());
        overview.setCurrency(aiProperties.getPricing() == null ? "USD" : normalizeCurrency(aiProperties.getPricing().getCurrency()));
        overview.setPricingConfigured(aiProperties.getPricing() != null && aiProperties.getPricing().configured());
        response.setOverview(overview);

        response.setSceneStats(mapSceneStats(snapshot));
        response.setModelStats(mapModelStats(snapshot));
        response.setRecentCalls(mapRecentCalls(snapshot));
        return response;
    }

    @Override
    public void clearRuntimeMetrics() {
        runtimeMetricsCollector.clear();
    }

    ChatModel createChatModel(ResolvedAiTestConfig resolvedConfig) {
        return OpenAiChatModel.builder()
                .baseUrl(resolvedConfig.baseUrl())
                .apiKey(resolvedConfig.apiKey())
                .modelName(resolvedConfig.model())
                .timeout(Duration.ofMillis(resolveReadTimeoutMs()))
                .maxRetries(resolveMaxRetries())
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private void populateResolvedConfig(AiConfigTestResponse response, ResolvedAiTestConfig resolvedConfig) {
        response.setProvider(resolvedConfig.provider());
        response.setBaseUrl(resolvedConfig.baseUrl());
        response.setModel(resolvedConfig.model());
        response.setUsedConfiguredApiKey(resolvedConfig.usedConfiguredApiKey());
        response.setApiKeyMasked(maskApiKey(resolvedConfig.apiKey()));
    }

    private AiPromptSpec findPromptSpec(String promptId) {
        for (AiPromptSpec spec : AiPromptCatalog.all()) {
            if (spec.promptId().equals(promptId.trim())) {
                return spec;
            }
        }
        throw new IllegalArgumentException("未找到对应的 PromptSpec: " + promptId);
    }

    private AiStructuredOutputSpec findStructuredOutputSpec(String promptId) {
        for (AiStructuredOutputSpec spec : AiStructuredOutputCatalog.all()) {
            if (spec.specId().equals(promptId)) {
                return spec;
            }
        }
        return null;
    }

    private AiRagRetrievalProfile findRetrievalProfile(String profileId) {
        if (!StringUtils.hasText(profileId)) {
            return null;
        }
        for (AiRagRetrievalProfile profile : AiRagRetrievalCatalog.all()) {
            if (profile.profileId().equals(profileId.trim())) {
                return profile;
            }
        }
        throw new IllegalArgumentException("未找到对应的检索画像: " + profileId);
    }

    private Map<String, Object> parseVariables(String variablesJson) {
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        if (!StringUtils.hasText(variablesJson)) {
            return variables;
        }

        try {
            JSONObject jsonObject = JSONUtil.parseObj(variablesJson.trim());
            for (String key : jsonObject.keySet()) {
                variables.put(key, jsonObject.get(key));
            }
            return variables;
        } catch (Exception e) {
            throw new IllegalArgumentException("variablesJson 必须是合法的 JSON 对象");
        }
    }

    private Map<String, Object> parseJsonObject(String jsonText, String fieldName) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        if (!StringUtils.hasText(jsonText)) {
            return values;
        }

        try {
            JSONObject jsonObject = JSONUtil.parseObj(jsonText.trim());
            for (String key : jsonObject.keySet()) {
                values.put(key, jsonObject.get(key));
            }
            return values;
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " 必须是合法的 JSON 对象");
        }
    }

    private Map<String, Object> resolveMetadataFilters(String metadataFiltersJson, AiRagRetrievalProfile retrievalProfile) {
        if (StringUtils.hasText(metadataFiltersJson)) {
            return parseJsonObject(metadataFiltersJson, "metadataFiltersJson");
        }
        if (retrievalProfile != null && retrievalProfile.metadataFilters() != null) {
            return new LinkedHashMap<>(retrievalProfile.metadataFilters());
        }
        return new LinkedHashMap<>();
    }

    private String resolveRagScene(String scene, AiRagRetrievalProfile retrievalProfile) {
        if (StringUtils.hasText(scene)) {
            return scene.trim();
        }
        if (retrievalProfile != null) {
            return retrievalProfile.scene();
        }
        return null;
    }

    private Integer resolveRagTopK(Integer topK, AiRagRetrievalProfile retrievalProfile) {
        if (topK != null && topK > 0) {
            return topK;
        }
        if (retrievalProfile != null) {
            return retrievalProfile.topK();
        }
        if (aiProperties.getRag() != null && aiProperties.getRag().getDefaultTopK() != null) {
            return aiProperties.getRag().getDefaultTopK();
        }
        return 5;
    }

    private Integer resolveRagDocumentLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_RAG_DOCUMENT_LIMIT;
        }
        if (limit <= 0 || limit > MAX_RAG_DOCUMENT_LIMIT) {
            throw new IllegalArgumentException("limit 必须在 1 到 " + MAX_RAG_DOCUMENT_LIMIT + " 之间");
        }
        return limit;
    }

    private String normalizeDocumentId(String documentId) {
        if (!StringUtils.hasText(documentId)) {
            throw new IllegalArgumentException("documentId 不能为空");
        }
        return documentId.trim();
    }

    private List<String> normalizeDocumentIds(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            throw new IllegalArgumentException("documentIds 不能为空，且至少需要一条有效 documentId");
        }

        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>();
        for (String documentId : documentIds) {
            normalizedIds.add(normalizeDocumentId(documentId));
        }
        return new ArrayList<>(normalizedIds);
    }

    private ArrayList<AiRagDebugNodeResponse> mapRagNodes(LlamaIndexRetrieveResponse retrieveResponse) {
        ArrayList<AiRagDebugNodeResponse> nodes = new ArrayList<>();
        if (retrieveResponse == null || retrieveResponse.getNodes() == null) {
            return nodes;
        }
        for (LlamaIndexRetrieveResponse.Node node : retrieveResponse.getNodes()) {
            AiRagDebugNodeResponse item = new AiRagDebugNodeResponse();
            item.setId(node.getId());
            item.setScore(node.getScore());
            item.setText(node.getText());
            item.setMetadata(node.getMetadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(node.getMetadata()));
            item.setMatchedTerms(node.getMatchedTerms() == null ? new ArrayList<>() : new ArrayList<>(node.getMatchedTerms()));
            item.setScoreBreakdown(node.getScoreBreakdown() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(node.getScoreBreakdown()));
            item.setBestMatchField(node.getBestMatchField());
            item.setBestSnippet(node.getBestSnippet());
            nodes.add(item);
        }
        return nodes;
    }

    private ArrayList<AiRagServiceDocumentItemResponse> mapRagServiceDocuments(LlamaIndexDocumentListResponse listResponse) {
        ArrayList<AiRagServiceDocumentItemResponse> documents = new ArrayList<>();
        if (listResponse == null || listResponse.getDocuments() == null) {
            return documents;
        }
        for (LlamaIndexDocumentListItem item : listResponse.getDocuments()) {
            AiRagServiceDocumentItemResponse response = new AiRagServiceDocumentItemResponse();
            response.setId(item.getId());
            response.setScene(item.getScene());
            response.setTextPreview(item.getTextPreview());
            response.setMetadata(item.getMetadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item.getMetadata()));
            documents.add(response);
        }
        return documents;
    }

    private ArrayList<AiRagDocumentExportItemResponse> mapRagExportDocuments(LlamaIndexDocumentExportResponse exportResponse) {
        ArrayList<AiRagDocumentExportItemResponse> documents = new ArrayList<>();
        if (exportResponse == null || exportResponse.getDocuments() == null) {
            return documents;
        }
        for (LlamaIndexKnowledgeDocument item : exportResponse.getDocuments()) {
            AiRagDocumentExportItemResponse response = new AiRagDocumentExportItemResponse();
            response.setId(item.getId());
            response.setScene(item.getScene());
            response.setText(item.getText());
            response.setMetadata(item.getMetadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item.getMetadata()));
            documents.add(response);
        }
        return documents;
    }

    private List<LlamaIndexKnowledgeDocument> loadSampleRagDocuments() {
        try {
            ClassPathResource resource = new ClassPathResource(SAMPLE_RAG_DOCUMENTS_RESOURCE);
            String jsonText = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            JSONArray array = JSONUtil.parseArray(jsonText);
            ArrayList<LlamaIndexKnowledgeDocument> documents = new ArrayList<>();
            for (Object item : array) {
                JSONObject jsonObject = JSONUtil.parseObj(item);
                LlamaIndexKnowledgeDocument document = new LlamaIndexKnowledgeDocument()
                        .setId(jsonObject.getStr("id"))
                        .setScene(jsonObject.getStr("scene"))
                        .setText(jsonObject.getStr("text"));
                JSONObject metadata = jsonObject.getJSONObject("metadata");
                if (metadata != null) {
                    LinkedHashMap<String, Object> metadataMap = new LinkedHashMap<>();
                    for (String key : metadata.keySet()) {
                        metadataMap.put(key, metadata.get(key));
                    }
                    document.setMetadata(metadataMap);
                }
                documents.add(document);
            }
            if (documents.isEmpty()) {
                throw new IllegalArgumentException("RAG 样例知识为空");
            }
            return documents;
        } catch (IOException e) {
            throw new IllegalArgumentException("读取 RAG 样例知识失败");
        }
    }

    private List<LlamaIndexKnowledgeDocument> parseCustomRagDocuments(AiRagDocumentImportRequest request, String defaultScene) {
        if (request == null || !StringUtils.hasText(request.getDocumentsJson())) {
            throw new IllegalArgumentException("documentsJson 不能为空，请输入 JSON 数组");
        }

        Object parsed;
        try {
            parsed = JSONUtil.parse(request.getDocumentsJson().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("documentsJson 不是合法 JSON，请输入 JSON 数组或 {\"documents\": [...]}");
        }

        JSONArray array = null;
        if (parsed instanceof JSONArray jsonArray) {
            array = jsonArray;
        } else if (parsed instanceof JSONObject jsonObject) {
            JSONArray documents = jsonObject.getJSONArray("documents");
            if (documents != null) {
                array = documents;
            }
        }

        if (array == null || array.isEmpty()) {
            throw new IllegalArgumentException("documentsJson 至少需要包含一条文档记录");
        }

        ArrayList<LlamaIndexKnowledgeDocument> documents = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            JSONObject item = JSONUtil.parseObj(array.get(index));
            documents.add(mapKnowledgeDocument(item, defaultScene, index));
        }
        return documents;
    }

    private LlamaIndexKnowledgeDocument mapKnowledgeDocument(JSONObject jsonObject, String defaultScene, int index) {
        if (jsonObject == null) {
            throw new IllegalArgumentException("documentsJson 第 " + (index + 1) + " 条文档不能为空");
        }

        String text = jsonObject.getStr("text");
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("documentsJson 第 " + (index + 1) + " 条文档缺少 text");
        }

        String scene = StringUtils.hasText(jsonObject.getStr("scene"))
                ? jsonObject.getStr("scene").trim()
                : defaultScene;

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        Object metadataObject = jsonObject.get("metadata");
        if (metadataObject != null) {
            if (!(metadataObject instanceof JSONObject metadataJson)) {
                throw new IllegalArgumentException("documentsJson 第 " + (index + 1) + " 条文档的 metadata 必须是 JSON 对象");
            }
            for (String key : metadataJson.keySet()) {
                metadata.put(key, metadataJson.get(key));
            }
        }

        String id = StringUtils.hasText(jsonObject.getStr("id"))
                ? jsonObject.getStr("id").trim()
                : buildGeneratedDocumentId(scene, text);

        return new LlamaIndexKnowledgeDocument()
                .setId(id)
                .setScene(scene)
                .setText(text.trim())
                .setMetadata(metadata);
    }

    private String buildGeneratedDocumentId(String scene, String text) {
        String normalizedScene = StringUtils.hasText(scene) ? scene.trim() : "general";
        String normalizedText = text == null ? "" : text.trim();
        String digest = sha1(normalizedScene + "\n" + normalizedText);
        return "custom-" + normalizedScene + "-" + digest.substring(0, 12);
    }

    private String sha1(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] encoded = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : encoded) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 算法不可用", e);
        }
    }

    private void ensureRagEndpointConfigured() {
        if (!llamaIndexClient.hasEndpointConfigured()) {
            throw new IllegalArgumentException("RAG endpoint 未配置");
        }
    }

    private void validateStructuredResponse(AiPromptDebugResponse response,
                                            AiStructuredOutputSpec structuredSpec,
                                            String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            response.setStructuredValid(false);
            response.setStructuredValidationReason("response_empty");
            return;
        }

        if (structuredSpec.rootType() == AiStructuredOutputSpec.RootType.OBJECT) {
            JSONObject jsonObject = AiJsonResponseParser.parse(rawResponse);
            if (jsonObject == null) {
                response.setStructuredValid(false);
                response.setStructuredValidationReason("response_not_json_object");
                return;
            }
            response.setParsedResponseJson(JSONUtil.toJsonPrettyStr(jsonObject));
            AiStructuredOutputValidator.ValidationResult validationResult = structuredSpec.validateObject(jsonObject);
            response.setStructuredValid(validationResult.valid());
            response.setStructuredValidationReason(validationResult.valid() ? "" : validationResult.reason());
            return;
        }

        JSONArray jsonArray = parseJsonArray(rawResponse);
        if (jsonArray == null) {
            response.setStructuredValid(false);
            response.setStructuredValidationReason("response_not_json_array");
            return;
        }
        response.setParsedResponseJson(JSONUtil.toJsonPrettyStr(jsonArray));
        AiStructuredOutputValidator.ValidationResult validationResult = structuredSpec.validateArray(jsonArray);
        response.setStructuredValid(validationResult.valid());
        response.setStructuredValidationReason(validationResult.valid() ? "" : validationResult.reason());
    }

    private JSONArray parseJsonArray(String rawResponse) {
        String normalized = normalizeJsonText(rawResponse);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        try {
            return JSONUtil.parseArray(normalized);
        } catch (Exception ignored) {
            // ignore
        }

        int start = normalized.indexOf('[');
        int end = normalized.lastIndexOf(']');
        if (start >= 0 && end > start) {
            try {
                return JSONUtil.parseArray(normalized.substring(start, end + 1));
            } catch (Exception ignored) {
                // ignore
            }
        }
        return null;
    }

    private String normalizeJsonText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private ArrayList<AiPromptCatalogItemResponse> mapPromptCatalog(LinkedHashSet<String> domains) {
        ArrayList<AiPromptCatalogItemResponse> prompts = new ArrayList<>();
        for (AiPromptSpec spec : AiPromptCatalog.all()) {
            String domain = resolveDomain(spec.key());
            domains.add(domain);

            AiPromptCatalogItemResponse item = new AiPromptCatalogItemResponse();
            item.setDomain(domain);
            item.setKey(spec.key());
            item.setVersion(spec.version());
            item.setPromptId(spec.promptId());
            item.setSystemPrompt(spec.systemPrompt());
            item.setUserTemplate(spec.userTemplate());
            item.setTemplateVariables(toSortedList(spec.templateVariables()));
            prompts.add(item);
        }
        return prompts;
    }

    private ArrayList<AiRagQueryCatalogItemResponse> mapRagQueryCatalog(LinkedHashSet<String> domains) {
        ArrayList<AiRagQueryCatalogItemResponse> ragQueries = new ArrayList<>();
        for (AiRagQuerySpec spec : AiRagQueryCatalog.all()) {
            String domain = resolveDomain(spec.key());
            domains.add(domain);

            AiRagQueryCatalogItemResponse item = new AiRagQueryCatalogItemResponse();
            item.setDomain(domain);
            item.setKey(spec.key());
            item.setVersion(spec.version());
            item.setQueryId(spec.queryId());
            item.setTemplate(spec.template());
            item.setTemplateVariables(toSortedList(spec.templateVariables()));
            ragQueries.add(item);
        }
        return ragQueries;
    }

    private ArrayList<AiRetrievalProfileResponse> mapRetrievalProfiles(LinkedHashSet<String> domains) {
        ArrayList<AiRetrievalProfileResponse> retrievalProfiles = new ArrayList<>();
        for (AiRagRetrievalProfile profile : AiRagRetrievalCatalog.all()) {
            String domain = resolveDomain(profile.querySpec().key());
            domains.add(domain);

            AiRetrievalProfileResponse item = new AiRetrievalProfileResponse();
            item.setDomain(domain);
            item.setProfileId(profile.profileId());
            item.setQueryKey(profile.querySpec().key());
            item.setQueryVersion(profile.querySpec().version());
            item.setQueryId(profile.querySpec().queryId());
            item.setScene(profile.scene());
            item.setTopK(profile.topK());
            item.setMetadataFilters(new LinkedHashMap<>(profile.metadataFilters()));
            retrievalProfiles.add(item);
        }
        return retrievalProfiles;
    }

    private ArrayList<AiStructuredSchemaResponse> mapStructuredSchemas(LinkedHashSet<String> domains) {
        ArrayList<AiStructuredSchemaResponse> structuredSchemas = new ArrayList<>();
        for (AiStructuredOutputSpec spec : AiStructuredOutputCatalog.all()) {
            String domain = resolveDomain(spec.promptSpec().key());
            domains.add(domain);

            AiStructuredSchemaResponse item = new AiStructuredSchemaResponse();
            item.setDomain(domain);
            item.setSpecId(spec.specId());
            item.setPromptKey(spec.promptSpec().key());
            item.setPromptVersion(spec.promptSpec().version());
            item.setRootType(spec.rootType().name());
            item.setSchemaId(spec.schemaId());
            item.setSchemaFileName(spec.schemaFileName());
            item.setSchemaJson(JSONUtil.toJsonPrettyStr(spec.jsonSchema()));
            structuredSchemas.add(item);
        }
        return structuredSchemas;
    }

    private ArrayList<AiRegressionCaseCatalogItemResponse> mapRegressionCatalogItems(List<AiRegressionCaseCatalogItem> items) {
        ArrayList<AiRegressionCaseCatalogItemResponse> responses = new ArrayList<>();
        if (items == null) {
            return responses;
        }
        for (AiRegressionCaseCatalogItem item : items) {
            AiRegressionCaseCatalogItemResponse response = new AiRegressionCaseCatalogItemResponse();
            response.setCaseId(item.getCaseId());
            response.setScenario(item.getScenario());
            response.setDescription(item.getDescription());
            response.setExpectedFallback(item.getExpectedFallback());
            response.setInputKeys(item.getInputKeys() == null ? new ArrayList<>() : new ArrayList<>(item.getInputKeys()));
            responses.add(response);
        }
        return responses;
    }

    private ArrayList<AiRegressionCaseResultResponse> mapRegressionCaseResults(List<AiRegressionCaseResult> items) {
        ArrayList<AiRegressionCaseResultResponse> responses = new ArrayList<>();
        if (items == null) {
            return responses;
        }
        for (AiRegressionCaseResult item : items) {
            AiRegressionCaseResultResponse response = new AiRegressionCaseResultResponse();
            response.setCaseId(item.getCaseId());
            response.setScenario(item.getScenario());
            response.setDescription(item.getDescription());
            response.setPassed(item.isPassed());
            response.setExpectedFallback(item.getExpectedFallback());
            response.setActualFallback(item.getActualFallback());
            response.setDurationMs(item.getDurationMs());
            response.setModelName(item.getModelName());
            response.setGraphName(item.getGraphName());
            response.setPromptIds(item.getPromptIds() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(item.getPromptIds()));
            response.setFailureReasons(item.getFailureReasons() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(item.getFailureReasons()));
            responses.add(response);
        }
        return responses;
    }

    private AiRegressionRunResponse mapRegressionRun(AiRegressionRunSummary summary) {
        AiRegressionRunResponse response = new AiRegressionRunResponse();
        if (summary == null) {
            return response;
        }
        response.setScenario(summary.getScenario());
        response.setCaseId(summary.getCaseId());
        response.setTotalCount(summary.getTotalCount());
        response.setPassedCount(summary.getPassedCount());
        response.setFailedCount(summary.getFailedCount());
        response.setDurationMs(summary.getDurationMs());
        response.setExecutedAt(summary.getExecutedAt());
        response.setCaseResults(mapRegressionCaseResults(summary.getCaseResults()));
        return response;
    }

    private ArrayList<AiRegressionScenarioHealthItemResponse> mapRegressionScenarioHealth(List<AiRegressionRunResponse> runs) {
        LinkedHashMap<String, RegressionScenarioHealthAccumulator> accumulators = new LinkedHashMap<>();
        if (runs == null) {
            return new ArrayList<>();
        }
        ArrayList<AiRegressionRunResponse> sortedRuns = new ArrayList<>(runs);
        sortedRuns.sort(Comparator.comparing(AiRegressionRunResponse::getExecutedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        for (AiRegressionRunResponse run : sortedRuns) {
            if (run == null) {
                continue;
            }
            LinkedHashMap<String, List<AiRegressionCaseResultResponse>> grouped = new LinkedHashMap<>();
            for (AiRegressionCaseResultResponse item : run.getCaseResults() == null ? List.<AiRegressionCaseResultResponse>of() : run.getCaseResults()) {
                if (item == null) {
                    continue;
                }
                String scenario = firstNonBlank(item.getScenario(), run.getScenario());
                if (!StringUtils.hasText(scenario)) {
                    continue;
                }
                grouped.computeIfAbsent(scenario, key -> new ArrayList<>()).add(item);
            }
            if (grouped.isEmpty() && StringUtils.hasText(run.getScenario())) {
                grouped.put(run.getScenario().trim(), new ArrayList<>());
            }
            for (Map.Entry<String, List<AiRegressionCaseResultResponse>> entry : grouped.entrySet()) {
                accumulators.computeIfAbsent(entry.getKey(), RegressionScenarioHealthAccumulator::new)
                        .accept(run, entry.getValue());
            }
        }
        ArrayList<AiRegressionScenarioHealthItemResponse> responses = new ArrayList<>();
        for (RegressionScenarioHealthAccumulator accumulator : accumulators.values()) {
            responses.add(accumulator.toResponse());
        }
        responses.sort(Comparator
                .comparing((AiRegressionScenarioHealthItemResponse item) -> Boolean.TRUE.equals(item.getLatestPassed()))
                .thenComparing(AiRegressionScenarioHealthItemResponse::getLastFailedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AiRegressionScenarioHealthItemResponse::getLatestExecutedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> item.getScenario() == null ? "" : item.getScenario()));
        return responses;
    }

    private ResolvedAiTestConfig resolveConfig(AiConfigTestRequest request) {
        AiConfigTestRequest safeRequest = request == null ? new AiConfigTestRequest() : request;

        String provider = normalizeProvider(aiProperties.getProvider());
        String baseUrl = normalizeBaseUrl(firstNonBlank(safeRequest.getBaseUrl(), aiProperties.getBaseUrl()));
        String apiKey = firstNonBlank(safeRequest.getApiKey(), aiProperties.getApiKey());
        String model = resolveModelName(safeRequest.getModel());
        boolean usedConfiguredApiKey = !StringUtils.hasText(safeRequest.getApiKey()) && aiProperties.hasApiKey();

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("缺少 Base URL，请先在后台填写，或在 application-sec.yml / 环境变量中配置");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("缺少 API Key，请先在后台填写，或在 application-sec.yml / 环境变量中配置");
        }
        if (!StringUtils.hasText(model)) {
            throw new IllegalArgumentException("缺少模型名称，请先在后台填写，或在 application-sec.yml / 环境变量中配置");
        }

        return new ResolvedAiTestConfig(provider, baseUrl, apiKey.trim(), model.trim(), usedConfiguredApiKey);
    }

    private String resolveModelName(String requestModel) {
        String runtimeModel = aiProperties.getModel() == null ? null : aiProperties.getModel().getChat();
        return firstNonBlank(requestModel, runtimeModel);
    }

    private String normalizeProvider(String provider) {
        return StringUtils.hasText(provider)
                ? provider.trim().toLowerCase(Locale.ROOT)
                : DEFAULT_PROVIDER;
    }

    private String normalizeSceneKeyword(String scene) {
        return StringUtils.hasText(scene) ? scene.trim() : null;
    }

    private String normalizeModelKeyword(String model) {
        return StringUtils.hasText(model) ? model.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeOutcome(String outcome) {
        if (!StringUtils.hasText(outcome)) {
            return null;
        }
        String normalized = outcome.trim().toLowerCase(Locale.ROOT);
        if ("all".equals(normalized)) {
            return null;
        }
        if (!SUPPORTED_OUTCOMES.contains(normalized)) {
            throw new IllegalArgumentException("outcome 仅支持 success 或 error");
        }
        return normalized;
    }

    private String normalizeCurrency(String currency) {
        return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String resolveDomain(String key) {
        if (!StringUtils.hasText(key)) {
            return "unknown";
        }
        int separatorIndex = key.indexOf('.');
        String domain = separatorIndex > 0 ? key.substring(0, separatorIndex) : key;
        return domain.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private int resolveReadTimeoutMs() {
        if (aiProperties.getTimeout() == null || aiProperties.getTimeout().getReadMs() == null) {
            return 60000;
        }
        return aiProperties.getTimeout().getReadMs();
    }

    private int resolveMaxRetries() {
        if (aiProperties.getRetry() == null || aiProperties.getRetry().getMaxAttempts() == null) {
            return 2;
        }
        return aiProperties.getRetry().getMaxAttempts();
    }

    private int resolveRecentLimit(Integer recentLimit) {
        if (recentLimit == null) {
            return DEFAULT_RECENT_LIMIT;
        }
        if (recentLimit < 1 || recentLimit > MAX_RECENT_LIMIT) {
            throw new IllegalArgumentException("recentLimit 取值范围为 1 到 50");
        }
        return recentLimit;
    }

    private int resolveRegressionHistoryLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_REGRESSION_HISTORY_LIMIT;
        }
        if (limit < 1 || limit > MAX_REGRESSION_HISTORY_LIMIT) {
            throw new IllegalArgumentException("limit 取值范围为 1 到 20");
        }
        return limit;
    }

    private String compactPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String compacted = content.trim().replaceAll("\\s+", " ");
        if (compacted.length() <= PREVIEW_MAX_LENGTH) {
            return compacted;
        }
        return compacted.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }

    private String sanitizeErrorMessage(String message, String currentApiKey) {
        if (!StringUtils.hasText(message)) {
            return "未返回详细错误信息";
        }
        String sanitized = message.trim();
        if (StringUtils.hasText(currentApiKey)) {
            sanitized = sanitized.replace(currentApiKey, maskApiKey(currentApiKey));
        }
        if (StringUtils.hasText(aiProperties.getApiKey())) {
            sanitized = sanitized.replace(aiProperties.getApiKey(), maskApiKey(aiProperties.getApiKey()));
        }
        sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("Bearer ******");
        if (sanitized.length() <= ERROR_MAX_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, ERROR_MAX_LENGTH) + "...";
    }

    private String sanitizeSensitiveMessage(String message, String currentApiKey) {
        if (!StringUtils.hasText(message)) {
            return "未返回详细错误信息";
        }
        String sanitized = message.trim();
        if (StringUtils.hasText(currentApiKey)) {
            sanitized = sanitized.replace(currentApiKey, maskApiKey(currentApiKey));
        }
        if (StringUtils.hasText(aiProperties.getApiKey())) {
            sanitized = sanitized.replace(aiProperties.getApiKey(), maskApiKey(aiProperties.getApiKey()));
        }
        if (aiProperties.getRag() != null && StringUtils.hasText(aiProperties.getRag().getApiKey())) {
            sanitized = sanitized.replace(aiProperties.getRag().getApiKey(), maskApiKey(aiProperties.getRag().getApiKey()));
        }
        sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("Bearer ******");
        if (sanitized.length() <= ERROR_MAX_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, ERROR_MAX_LENGTH) + "...";
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= API_KEY_PREFIX_LENGTH + API_KEY_SUFFIX_LENGTH) {
            return "******";
        }
        return trimmed.substring(0, API_KEY_PREFIX_LENGTH)
                + "******"
                + trimmed.substring(trimmed.length() - API_KEY_SUFFIX_LENGTH);
    }

    private ArrayList<AiRuntimeSceneMetricsResponse> mapSceneStats(AiRuntimeMetricsSnapshot snapshot) {
        ArrayList<AiRuntimeSceneMetricsResponse> sceneStats = new ArrayList<>();
        if (snapshot.getSceneStats() == null) {
            return sceneStats;
        }
        for (AiRuntimeSceneMetrics item : snapshot.getSceneStats()) {
            sceneStats.add(new AiRuntimeSceneMetricsResponse()
                    .setScene(item.getScene())
                    .setPromptKey(item.getPromptKey())
                    .setPromptVersion(item.getPromptVersion())
                    .setLastModelName(item.getLastModelName())
                    .setInvocations(item.getInvocations())
                    .setSuccessCount(item.getSuccessCount())
                    .setErrorCount(item.getErrorCount())
                    .setFallbackCount(item.getFallbackCount())
                    .setStructuredParseFailureCount(item.getStructuredParseFailureCount())
                    .setTotalInputTokens(item.getTotalInputTokens())
                    .setTotalOutputTokens(item.getTotalOutputTokens())
                    .setTotalTokens(item.getTotalTokens())
                    .setEstimatedCost(item.getEstimatedCost())
                    .setAverageLatencyMs(item.getAverageLatencyMs())
                    .setLastInvocationAt(item.getLastInvocationAt()));
        }
        return sceneStats;
    }

    private ArrayList<AiRuntimeRecentCallResponse> mapRecentCalls(AiRuntimeMetricsSnapshot snapshot) {
        ArrayList<AiRuntimeRecentCallResponse> recentCalls = new ArrayList<>();
        if (snapshot.getRecentCalls() == null) {
            return recentCalls;
        }
        for (AiRuntimeRecentCall item : snapshot.getRecentCalls()) {
            recentCalls.add(new AiRuntimeRecentCallResponse()
                    .setScene(item.getScene())
                    .setPromptKey(item.getPromptKey())
                    .setPromptVersion(item.getPromptVersion())
                    .setModelName(item.getModelName())
                    .setOutcome(item.getOutcome())
                    .setLatencyMs(item.getLatencyMs())
                    .setInputTokens(item.getInputTokens())
                    .setOutputTokens(item.getOutputTokens())
                    .setTotalTokens(item.getTotalTokens())
                    .setEstimatedCost(item.getEstimatedCost())
                    .setTimestamp(item.getTimestamp()));
        }
        return recentCalls;
    }

    private ArrayList<AiRuntimeModelMetricsResponse> mapModelStats(AiRuntimeMetricsSnapshot snapshot) {
        ArrayList<AiRuntimeModelMetricsResponse> modelStats = new ArrayList<>();
        if (snapshot.getModelStats() == null) {
            return modelStats;
        }
        for (AiRuntimeModelMetrics item : snapshot.getModelStats()) {
            modelStats.add(new AiRuntimeModelMetricsResponse()
                    .setModelName(item.getModelName())
                    .setInvocations(item.getInvocations())
                    .setSuccessCount(item.getSuccessCount())
                    .setErrorCount(item.getErrorCount())
                    .setTotalInputTokens(item.getTotalInputTokens())
                    .setTotalOutputTokens(item.getTotalOutputTokens())
                    .setTotalTokens(item.getTotalTokens())
                    .setEstimatedCost(item.getEstimatedCost())
                    .setAverageLatencyMs(item.getAverageLatencyMs())
                    .setLastInvocationAt(item.getLastInvocationAt()));
        }
        return modelStats;
    }

    private List<String> toSortedList(Set<String> values) {
        ArrayList<String> sortedValues = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            return sortedValues;
        }
        sortedValues.addAll(values);
        sortedValues.sort(String::compareTo);
        return sortedValues;
    }

    private BigDecimal decimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private ArrayList<AiRegressionInsightItemResponse> mapRegressionInsightItems(Map<String, Integer> counters) {
        ArrayList<AiRegressionInsightItemResponse> responses = new ArrayList<>();
        if (counters == null || counters.isEmpty()) {
            return responses;
        }
        counters.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getKey()) && entry.getValue() != null && entry.getValue() > 0)
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(SCENARIO_HEALTH_INSIGHT_LIMIT)
                .forEach(entry -> {
                    AiRegressionInsightItemResponse response = new AiRegressionInsightItemResponse();
                    response.setLabel(entry.getKey());
                    response.setCount(entry.getValue());
                    responses.add(response);
                });
        return responses;
    }

    private void incrementCounter(Map<String, Integer> counters, String label) {
        if (!StringUtils.hasText(label)) {
            return;
        }
        counters.merge(label.trim(), 1, Integer::sum);
    }

    private LinkedHashSet<String> normalizeFailureReasons(List<String> failureReasons) {
        LinkedHashSet<String> normalizedReasons = new LinkedHashSet<>();
        if (failureReasons == null || failureReasons.isEmpty()) {
            normalizedReasons.add(DEFAULT_FAILURE_REASON_LABEL);
            return normalizedReasons;
        }
        for (String failureReason : failureReasons) {
            if (!StringUtils.hasText(failureReason)) {
                continue;
            }
            String normalized = failureReason.trim().replaceAll("\\s+", " ");
            if (StringUtils.hasText(normalized)) {
                normalizedReasons.add(normalized);
            }
        }
        if (normalizedReasons.isEmpty()) {
            normalizedReasons.add(DEFAULT_FAILURE_REASON_LABEL);
        }
        return normalizedReasons;
    }

    private final class RegressionScenarioHealthAccumulator {

        private final String scenario;
        private int runCount;
        private int failedRunCount;
        private int totalCaseCount;
        private int passedCaseCount;
        private int failedCaseCount;
        private Long latestExecutedAt;
        private Boolean latestPassed;
        private Long lastFailedAt;
        private int latestFailedCaseCount;
        private final LinkedHashSet<String> latestFailedCaseIds = new LinkedHashSet<>();
        private final LinkedHashMap<String, Integer> failedCaseCounters = new LinkedHashMap<>();
        private final LinkedHashMap<String, Integer> failureReasonCounters = new LinkedHashMap<>();
        private final LinkedHashMap<String, Integer> modelCounters = new LinkedHashMap<>();
        private final LinkedHashMap<String, Integer> graphCounters = new LinkedHashMap<>();
        private final LinkedHashMap<String, Integer> promptCounters = new LinkedHashMap<>();

        private RegressionScenarioHealthAccumulator(String scenario) {
            this.scenario = scenario;
        }

        private void accept(AiRegressionRunResponse run, List<AiRegressionCaseResultResponse> results) {
            List<AiRegressionCaseResultResponse> safeResults = results == null ? List.of() : results;
            int currentTotalCaseCount = safeResults.isEmpty() ? safeInteger(run.getTotalCount()) : safeResults.size();
            int currentPassedCaseCount = safeResults.isEmpty()
                    ? safeInteger(run.getPassedCount())
                    : (int) safeResults.stream().filter(AiRegressionCaseResultResponse::isPassed).count();
            int currentFailedCaseCount = safeResults.isEmpty()
                    ? safeInteger(run.getFailedCount())
                    : (int) safeResults.stream().filter(item -> !item.isPassed()).count();
            boolean currentPassed = currentFailedCaseCount == 0;

            runCount += 1;
            totalCaseCount += currentTotalCaseCount;
            passedCaseCount += currentPassedCaseCount;
            failedCaseCount += currentFailedCaseCount;
            if (!currentPassed) {
                failedRunCount += 1;
                if (lastFailedAt == null || compareTimestamp(run.getExecutedAt(), lastFailedAt) > 0) {
                    lastFailedAt = run.getExecutedAt();
                }
            }
            boolean latestRun = latestExecutedAt == null || compareTimestamp(run.getExecutedAt(), latestExecutedAt) > 0;
            if (latestRun) {
                latestExecutedAt = run.getExecutedAt();
                latestPassed = currentPassed;
                latestFailedCaseCount = currentFailedCaseCount;
                latestFailedCaseIds.clear();
                if (safeResults.isEmpty()) {
                    if (!currentPassed && StringUtils.hasText(run.getCaseId())) {
                        String normalizedCaseId = run.getCaseId().trim();
                        latestFailedCaseIds.add(normalizedCaseId);
                        incrementCounter(failedCaseCounters, normalizedCaseId);
                    }
                    if (!currentPassed) {
                        incrementCounter(failureReasonCounters, DEFAULT_FAILURE_REASON_LABEL);
                    }
                    return;
                }
            }
            if (safeResults.isEmpty()) {
                if (!currentPassed && StringUtils.hasText(run.getCaseId())) {
                    incrementCounter(failedCaseCounters, run.getCaseId().trim());
                }
                if (!currentPassed) {
                    incrementCounter(failureReasonCounters, DEFAULT_FAILURE_REASON_LABEL);
                }
                return;
            }
            for (AiRegressionCaseResultResponse item : safeResults) {
                if (item == null || item.isPassed()) {
                    continue;
                }
                if (StringUtils.hasText(item.getCaseId())) {
                    String caseId = item.getCaseId().trim();
                    incrementCounter(failedCaseCounters, caseId);
                    if (latestRun) {
                        latestFailedCaseIds.add(caseId);
                    }
                }
                for (String failureReason : normalizeFailureReasons(item.getFailureReasons())) {
                    incrementCounter(failureReasonCounters, failureReason);
                }
                incrementCounter(modelCounters, item.getModelName());
                incrementCounter(graphCounters, item.getGraphName());
                for (String promptId : item.getPromptIds() == null ? List.<String>of() : item.getPromptIds()) {
                    incrementCounter(promptCounters, promptId);
                }
            }
        }

        private AiRegressionScenarioHealthItemResponse toResponse() {
            AiRegressionScenarioHealthItemResponse response = new AiRegressionScenarioHealthItemResponse();
            response.setScenario(scenario);
            response.setRunCount(runCount);
            response.setFailedRunCount(failedRunCount);
            response.setTotalCaseCount(totalCaseCount);
            response.setPassedCaseCount(passedCaseCount);
            response.setFailedCaseCount(failedCaseCount);
            response.setLatestExecutedAt(latestExecutedAt);
            response.setLatestPassed(latestPassed);
            response.setLastFailedAt(lastFailedAt);
            response.setLatestFailedCaseCount(latestFailedCaseCount);
            response.setLatestFailedCaseIds(new ArrayList<>(latestFailedCaseIds));
            response.setTopFailedCases(mapRegressionInsightItems(failedCaseCounters));
            response.setTopFailureReasons(mapRegressionInsightItems(failureReasonCounters));
            response.setTopModelNames(mapRegressionInsightItems(modelCounters));
            response.setTopGraphNames(mapRegressionInsightItems(graphCounters));
            response.setTopPromptIds(mapRegressionInsightItems(promptCounters));
            return response;
        }

        private int compareTimestamp(Long left, Long right) {
            if (left == null && right == null) {
                return 0;
            }
            if (left == null) {
                return -1;
            }
            if (right == null) {
                return 1;
            }
            return Long.compare(left, right);
        }
    }

    record ResolvedAiTestConfig(
            String provider,
            String baseUrl,
            String apiKey,
            String model,
            boolean usedConfiguredApiKey
    ) {
    }
}
