package com.xiaou.system.service.impl;

import com.xiaou.ai.client.AiChatResult;
import com.xiaou.ai.metrics.AiRuntimeMetricsCollector;
import com.xiaou.ai.metrics.AiRuntimeModelMetrics;
import com.xiaou.ai.metrics.AiRuntimeMetricsSnapshot;
import com.xiaou.ai.regression.AiRegressionCaseCatalogItem;
import com.xiaou.ai.regression.AiRegressionCaseResult;
import com.xiaou.ai.regression.AiRegressionRunSummary;
import com.xiaou.ai.regression.AiRegressionService;
import com.xiaou.ai.rag.LlamaIndexDocumentBatchDeleteResponse;
import com.xiaou.ai.rag.LlamaIndexDocumentDeleteResponse;
import com.xiaou.ai.rag.LlamaIndexDocumentExportResponse;
import com.xiaou.ai.rag.LlamaIndexDocumentImportResponse;
import com.xiaou.ai.rag.LlamaIndexDocumentListItem;
import com.xiaou.ai.rag.LlamaIndexDocumentListResponse;
import com.xiaou.ai.rag.LlamaIndexClient;
import com.xiaou.ai.rag.LlamaIndexHealthResponse;
import com.xiaou.ai.rag.LlamaIndexKnowledgeDocument;
import com.xiaou.ai.rag.LlamaIndexRetrieveResponse;
import com.xiaou.ai.support.AiExecutionSupport;
import com.xiaou.common.config.AiProperties;
import com.xiaou.system.dto.AiConfigTestRequest;
import com.xiaou.system.dto.AiConfigTestResponse;
import com.xiaou.system.dto.AiPromptDebugRequest;
import com.xiaou.system.dto.AiPromptDebugResponse;
import com.xiaou.system.dto.AiRegressionCaseCatalogResponse;
import com.xiaou.system.dto.AiRegressionCaseResultResponse;
import com.xiaou.system.dto.AiRegressionRunHistoryResponse;
import com.xiaou.system.dto.AiRegressionRunRequest;
import com.xiaou.system.dto.AiRegressionRunResponse;
import com.xiaou.system.dto.AiRegressionScenarioHealthResponse;
import com.xiaou.system.dto.AiRagDocumentBatchDeleteRequest;
import com.xiaou.system.dto.AiRagDocumentBatchDeleteResponse;
import com.xiaou.system.dto.AiRagDocumentDeleteResponse;
import com.xiaou.system.dto.AiRagDocumentExportResponse;
import com.xiaou.system.dto.AiRagDocumentImportRequest;
import com.xiaou.system.dto.AiRagDocumentImportResponse;
import com.xiaou.system.dto.AiRagDebugRequest;
import com.xiaou.system.dto.AiRagDebugResponse;
import com.xiaou.system.dto.AiRagSampleImportRequest;
import com.xiaou.system.dto.AiRagSampleImportResponse;
import com.xiaou.system.dto.AiRagServiceDocumentListResponse;
import com.xiaou.system.dto.AiRagServiceHealthResponse;
import com.xiaou.system.dto.AiRuntimeConfigResponse;
import com.xiaou.system.dto.AiRuntimeMetricsResponse;
import com.xiaou.system.dto.AiSchemaCatalogResponse;
import com.xiaou.system.dto.AiStructuredSchemaResponse;
import com.xiaou.system.service.impl.support.AiRegressionRunStateRepository;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysAiConfigServiceImplTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private AiExecutionSupport aiExecutionSupport;

    @Mock
    private LlamaIndexClient llamaIndexClient;

    @Mock
    private AiRegressionService aiRegressionService;

    @Mock
    private AiRegressionRunStateRepository aiRegressionRunStateRepository;

    private AiProperties aiProperties;
    private AiRuntimeMetricsCollector runtimeMetricsCollector;

    private SysAiConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.setProvider("openai-compatible");
        aiProperties.setBaseUrl("https://proxy.example.com/v1");
        aiProperties.setApiKey("sk-1234567890abcdef");
        aiProperties.getModel().setChat("gpt-5.4");
        aiProperties.getTimeout().setReadMs(30000);
        aiProperties.getRetry().setMaxAttempts(1);
        aiProperties.getPricing().setCurrency("usd");
        aiProperties.getPricing().setInputPerMillion(2.5D);
        aiProperties.getPricing().setOutputPerMillion(10D);
        aiProperties.getRag().setEnabled(true);
        aiProperties.getRag().setEndpoint("http://localhost:18080/");
        aiProperties.getRag().setApiKey("rag-test-key");
        aiProperties.getRag().setDefaultTopK(5);

        runtimeMetricsCollector = mock(AiRuntimeMetricsCollector.class);
        lenient().when(runtimeMetricsCollector.isPersistenceEnabled()).thenReturn(true);
        lenient().when(runtimeMetricsCollector.persistenceMode()).thenReturn("redis");
        service = spy(new SysAiConfigServiceImpl(
                aiProperties,
                runtimeMetricsCollector,
                aiExecutionSupport,
                aiRegressionService,
                llamaIndexClient,
                aiRegressionRunStateRepository
        ));
    }

    @Test
    void shouldReturnRuntimeSummaryWithMaskedKey() {
        AiRuntimeConfigResponse response = service.getRuntimeConfig();

        assertTrue(response.isConfigured());
        assertTrue(response.isApiKeyConfigured());
        assertEquals("openai-compatible", response.getProvider());
        assertEquals("https://proxy.example.com/v1", response.getBaseUrl());
        assertEquals("gpt-5.4", response.getModel());
        assertEquals("sk-123******cdef", response.getApiKeyMasked());
        assertTrue(response.isPricingConfigured());
        assertEquals("USD", response.getPricingCurrency());
        assertTrue(response.isMetricsPersistenceEnabled());
        assertEquals("redis", response.getMetricsPersistenceMode());
        assertTrue(response.isRagEnabled());
        assertEquals("http://localhost:18080", response.getRagEndpoint());
        assertTrue(response.isRagApiKeyConfigured());
        assertEquals("rag-te******-key", response.getRagApiKeyMasked());
        assertEquals(5, response.getRagDefaultTopK());
    }

    @Test
    void shouldReturnSchemaCatalogForAdminDebug() {
        AiSchemaCatalogResponse response = service.getSchemaCatalog();

        assertNotNull(response);
        assertTrue(response.getDomains().contains("community"));
        assertTrue(response.getDomains().contains("mock_interview"));
        assertTrue(response.getDomains().contains("job_battle"));
        assertTrue(response.getDomains().contains("sql_optimize"));
        assertTrue(response.getPrompts().stream().anyMatch(item ->
                "mock_interview.evaluate_answer:v1".equals(item.getPromptId())
                        && item.getTemplateVariables().contains("direction")
        ));
        assertTrue(response.getRagQueries().stream().anyMatch(item ->
                "job_battle.retrieve.generate_plan:v1".equals(item.getQueryId())
                        && item.getTemplateVariables().contains("targetDays")
        ));
        assertTrue(response.getRetrievalProfiles().stream().anyMatch(item ->
                "sql_optimize.retrieve.analyze:v1".equals(item.getProfileId())
                        && item.getTopK() > 0
        ));

        AiStructuredSchemaResponse schema = response.getStructuredSchemas().stream()
                .filter(item -> "community.post_summary:v1".equals(item.getSpecId()))
                .findFirst()
                .orElseThrow();

        assertEquals("xiaou://ai/structured-output/community.post_summary:v1", schema.getSchemaId());
        assertEquals("community.post_summary:v1", schema.getSpecId());
        assertTrue(schema.getSchemaJson().contains("\"type\""));
        assertTrue(schema.getSchemaJson().contains("\"summary\""));
    }

    @Test
    void shouldReturnRegressionCaseCatalog() {
        when(aiRegressionService.listCases()).thenReturn(java.util.List.of(
                new AiRegressionCaseCatalogItem()
                        .setCaseId("community-summary-success")
                        .setScenario("community_summary")
                        .setDescription("社区摘要成功用例")
                        .setExpectedFallback(false)
                        .setInputKeys(java.util.List.of("title", "content")),
                new AiRegressionCaseCatalogItem()
                        .setCaseId("sql-analyze-success")
                        .setScenario("sql_optimize_analyze")
                        .setDescription("SQL 分析成功用例")
                        .setExpectedFallback(false)
                        .setInputKeys(java.util.List.of("sql", "explainResult"))
        ));

        AiRegressionCaseCatalogResponse response = service.getRegressionCaseCatalog();

        assertEquals(2, response.getTotalCount());
        assertEquals(2, response.getCases().size());
        assertTrue(response.getScenarios().contains("community_summary"));
        assertTrue(response.getScenarios().contains("sql_optimize_analyze"));
        assertEquals("community-summary-success", response.getCases().get(0).getCaseId());
        verify(aiRegressionService).listCases();
    }

    @Test
    void shouldRunRegressionThroughAiService() {
        when(aiRegressionService.run("community_summary", "community-summary-success")).thenReturn(
                new AiRegressionRunSummary()
                        .setScenario("community_summary")
                        .setCaseId("community-summary-success")
                        .setTotalCount(1)
                        .setPassedCount(1)
                        .setFailedCount(0)
                        .setDurationMs(128L)
                        .setExecutedAt(1713740800000L)
                        .setCaseResults(java.util.List.of(
                                new AiRegressionCaseResult()
                                        .setCaseId("community-summary-success")
                                        .setScenario("community_summary")
                                        .setDescription("社区摘要成功用例")
                                        .setPassed(true)
                                        .setExpectedFallback(false)
                                        .setActualFallback(false)
                                        .setDurationMs(128L)
                        ))
        );

        AiRegressionRunRequest request = new AiRegressionRunRequest();
        request.setScenario("community_summary");
        request.setCaseId("community-summary-success");

        AiRegressionRunResponse response = service.runRegression(request);

        assertEquals("community_summary", response.getScenario());
        assertEquals("community-summary-success", response.getCaseId());
        assertEquals(1, response.getTotalCount());
        assertEquals(1, response.getPassedCount());
        assertEquals(0, response.getFailedCount());
        assertEquals(1, response.getCaseResults().size());
        assertEquals(1713740800000L, response.getExecutedAt());
        assertTrue(response.getCaseResults().get(0).isPassed());
        verify(aiRegressionService).run("community_summary", "community-summary-success");
        verify(aiRegressionRunStateRepository).saveLatest(any(AiRegressionRunResponse.class));
    }

    @Test
    void shouldReturnLatestRegressionRunFromStateRepository() {
        AiRegressionRunResponse stored = new AiRegressionRunResponse();
        stored.setScenario("community_summary");
        stored.setCaseId("community-summary-success");
        stored.setTotalCount(1);
        stored.setPassedCount(1);
        stored.setFailedCount(0);
        stored.setDurationMs(128L);
        stored.setExecutedAt(1713740800000L);
        when(aiRegressionRunStateRepository.loadLatest()).thenReturn(stored);

        AiRegressionRunResponse response = service.getLatestRegressionRun();

        assertNotNull(response);
        assertEquals("community_summary", response.getScenario());
        assertEquals("community-summary-success", response.getCaseId());
        assertEquals(1713740800000L, response.getExecutedAt());
        verify(aiRegressionRunStateRepository).loadLatest();
    }

    @Test
    void shouldReturnRegressionRunHistory() {
        AiRegressionRunResponse latest = new AiRegressionRunResponse();
        latest.setScenario("community_summary");
        latest.setCaseId("community-summary-success");
        latest.setFailedCount(0);
        latest.setExecutedAt(1713740800000L);

        AiRegressionRunResponse previous = new AiRegressionRunResponse();
        previous.setScenario("sql_optimize_analyze");
        previous.setCaseId("sql-analyze-success");
        previous.setFailedCount(1);
        previous.setExecutedAt(1713654400000L);

        when(aiRegressionRunStateRepository.loadHistory(5)).thenReturn(java.util.List.of(latest, previous));

        AiRegressionRunHistoryResponse response = service.getRegressionRunHistory(5);

        assertNotNull(response);
        assertEquals(5, response.getLimit());
        assertEquals(2, response.getTotalCount());
        assertEquals(2, response.getRuns().size());
        assertEquals("community-summary-success", response.getRuns().get(0).getCaseId());
        assertEquals("sql-analyze-success", response.getRuns().get(1).getCaseId());
        verify(aiRegressionRunStateRepository).loadHistory(5);
    }

    @Test
    void shouldReturnRegressionScenarioHealth() {
        AiRegressionCaseResultResponse communityFailed = new AiRegressionCaseResultResponse();
        communityFailed.setCaseId("community-summary-failed");
        communityFailed.setScenario("community_summary");
        communityFailed.setPassed(false);
        communityFailed.setFailureReasons(java.util.List.of("摘要为空"));

        AiRegressionCaseResultResponse communityPassed = new AiRegressionCaseResultResponse();
        communityPassed.setCaseId("community-summary-success");
        communityPassed.setScenario("community_summary");
        communityPassed.setPassed(true);

        AiRegressionCaseResultResponse sqlPassed = new AiRegressionCaseResultResponse();
        sqlPassed.setCaseId("sql-analyze-success");
        sqlPassed.setScenario("sql_optimize_analyze");
        sqlPassed.setPassed(true);

        AiRegressionRunResponse latest = new AiRegressionRunResponse();
        latest.setExecutedAt(1713740800000L);
        latest.setCaseResults(java.util.List.of(communityFailed, communityPassed, sqlPassed));

        AiRegressionCaseResultResponse communityHistoricalPassed = new AiRegressionCaseResultResponse();
        communityHistoricalPassed.setCaseId("community-summary-success");
        communityHistoricalPassed.setScenario("community_summary");
        communityHistoricalPassed.setPassed(true);

        AiRegressionCaseResultResponse communityHistoricalFailed = new AiRegressionCaseResultResponse();
        communityHistoricalFailed.setCaseId("community-summary-failed");
        communityHistoricalFailed.setScenario("community_summary");
        communityHistoricalFailed.setPassed(false);
        communityHistoricalFailed.setFailureReasons(java.util.List.of("摘要为空", "标题未命中"));

        AiRegressionCaseResultResponse sqlHistoricalFailed = new AiRegressionCaseResultResponse();
        sqlHistoricalFailed.setCaseId("sql-analyze-failed");
        sqlHistoricalFailed.setScenario("sql_optimize_analyze");
        sqlHistoricalFailed.setPassed(false);
        sqlHistoricalFailed.setFailureReasons(java.util.List.of("未命中索引建议"));

        AiRegressionRunResponse previous = new AiRegressionRunResponse();
        previous.setExecutedAt(1713654400000L);
        previous.setCaseResults(java.util.List.of(communityHistoricalPassed, communityHistoricalFailed, sqlHistoricalFailed));

        when(aiRegressionRunStateRepository.loadHistory(10)).thenReturn(java.util.List.of(latest, previous));

        AiRegressionScenarioHealthResponse response = service.getRegressionScenarioHealth(10);

        assertNotNull(response);
        assertEquals(10, response.getLimit());
        assertEquals(2, response.getTotalCount());
        assertEquals("community_summary", response.getScenarios().get(0).getScenario());
        assertFalse(Boolean.TRUE.equals(response.getScenarios().get(0).getLatestPassed()));
        assertEquals(2, response.getScenarios().get(0).getRunCount());
        assertEquals(2, response.getScenarios().get(0).getFailedRunCount());
        assertEquals(4, response.getScenarios().get(0).getTotalCaseCount());
        assertEquals(2, response.getScenarios().get(0).getPassedCaseCount());
        assertEquals(2, response.getScenarios().get(0).getFailedCaseCount());
        assertEquals(1, response.getScenarios().get(0).getLatestFailedCaseCount());
        assertTrue(response.getScenarios().get(0).getLatestFailedCaseIds().contains("community-summary-failed"));
        assertEquals("community-summary-failed", response.getScenarios().get(0).getTopFailedCases().get(0).getLabel());
        assertEquals(2, response.getScenarios().get(0).getTopFailedCases().get(0).getCount());
        assertEquals("摘要为空", response.getScenarios().get(0).getTopFailureReasons().get(0).getLabel());
        assertEquals(2, response.getScenarios().get(0).getTopFailureReasons().get(0).getCount());
        assertEquals("标题未命中", response.getScenarios().get(0).getTopFailureReasons().get(1).getLabel());
        assertEquals(1, response.getScenarios().get(0).getTopFailureReasons().get(1).getCount());

        assertEquals("sql_optimize_analyze", response.getScenarios().get(1).getScenario());
        assertTrue(Boolean.TRUE.equals(response.getScenarios().get(1).getLatestPassed()));
        assertEquals(1713654400000L, response.getScenarios().get(1).getLastFailedAt());
        assertEquals("sql-analyze-failed", response.getScenarios().get(1).getTopFailedCases().get(0).getLabel());
        assertEquals(1, response.getScenarios().get(1).getTopFailureReasons().get(0).getCount());
        verify(aiRegressionRunStateRepository).loadHistory(10);
    }

    @Test
    void shouldRejectInvalidRegressionHistoryLimit() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getRegressionRunHistory(0)
        );

        assertTrue(exception.getMessage().contains("limit"));
    }

    @Test
    void shouldRenderPromptDebugWithoutExecutingModel() {
        AiPromptDebugRequest request = new AiPromptDebugRequest();
        request.setPromptId("community.post_summary:v1");
        request.setVariablesJson("""
                {
                  "title": "Java 面试经验",
                  "content": "分享一下我最近的后端面试经历"
                }
                """);
        request.setExecute(false);

        AiPromptDebugResponse response = service.debugPrompt(request);

        assertEquals("community.post_summary:v1", response.getPromptId());
        assertEquals("community", response.getDomain());
        assertFalse(response.isExecuted());
        assertTrue(response.isStructuredOutputBound());
        assertTrue(response.getRenderedUserPrompt().contains("Java 面试经验"));
        assertTrue(response.getVariablesJson().contains("\"title\""));
        assertNull(response.getRawResponse());
        verify(aiExecutionSupport, never()).chatResult(anyString(), any(), any());
    }

    @Test
    void shouldExecutePromptDebugAndValidateStructuredOutput() {
        AiChatResult chatResult = new AiChatResult()
                .setContent("""
                        {
                          "summary": "这是一段摘要",
                          "keywords": ["Java", "面试"]
                        }
                        """)
                .setModelName("gpt-5.4");
        when(aiExecutionSupport.chatResult(anyString(), any(), any())).thenReturn(chatResult);

        AiPromptDebugRequest request = new AiPromptDebugRequest();
        request.setPromptId("community.post_summary:v1");
        request.setVariablesJson("""
                {
                  "title": "Java 面试经验",
                  "content": "分享一下我最近的后端面试经历"
                }
                """);
        request.setExecute(true);

        AiPromptDebugResponse response = service.debugPrompt(request);

        assertTrue(response.isExecuted());
        assertEquals("gpt-5.4", response.getModelName());
        assertTrue(response.isStructuredOutputBound());
        assertEquals(Boolean.TRUE, response.getStructuredValid());
        assertTrue(response.getParsedResponseJson().contains("\"summary\""));
        assertEquals("", response.getStructuredValidationReason());
        verify(aiExecutionSupport).chatResult(anyString(), any(), any());
    }

    @Test
    void shouldDebugRagWithProfileDefaults() {
        LlamaIndexRetrieveResponse retrieveResponse = new LlamaIndexRetrieveResponse()
                .setQuery("缓存一致性")
                .setFallback(false)
                .setNodes(java.util.List.of(new LlamaIndexRetrieveResponse.Node()
                        .setId("doc-1")
                        .setScore(0.92)
                        .setText("缓存一致性可以从延迟双删和消息最终一致性展开")
                        .setMetadata(java.util.Map.of("source", "kb"))
                        .setMatchedTerms(java.util.List.of("缓存一致性", "延迟双删"))
                        .setScoreBreakdown(java.util.Map.of("textTermScore", 3.2D, "termCoverage", 1.4D))
                        .setBestMatchField("text")
                        .setBestSnippet("缓存一致性可以从延迟双删和消息最终一致性展开")));
        when(llamaIndexClient.retrieve(any())).thenReturn(retrieveResponse);

        AiRagDebugRequest request = new AiRagDebugRequest();
        request.setProfileId("mock_interview.retrieve.generate_questions:v1");
        request.setQuery("缓存一致性");

        AiRagDebugResponse response = service.debugRag(request);

        assertEquals("mock_interview.retrieve.generate_questions:v1", response.getProfileId());
        assertTrue(response.isRagEnabled());
        assertEquals("http://localhost:18080", response.getEndpoint());
        assertTrue(response.isApiKeyConfigured());
        assertEquals("rag-te******-key", response.getApiKeyMasked());
        assertEquals("缓存一致性", response.getQuery());
        assertEquals("mock_interview", response.getScene());
        assertEquals(3, response.getTopK());
        assertEquals(1, response.getNodeCount());
        assertFalse(response.isFallback());
        assertTrue(response.getContextSnippet().contains("缓存一致性"));
        assertEquals(1, response.getNodes().size());
        assertEquals("doc-1", response.getNodes().get(0).getId());
        assertTrue(response.getNodes().get(0).getMatchedTerms().contains("缓存一致性"));
        assertTrue(response.getNodes().get(0).getScoreBreakdown().containsKey("textTermScore"));
        assertEquals("text", response.getNodes().get(0).getBestMatchField());
        assertTrue(response.getNodes().get(0).getBestSnippet().contains("缓存一致性"));
        verify(llamaIndexClient).retrieve(any());
    }

    @Test
    void shouldRejectInvalidRagMetadataJson() {
        AiRagDebugRequest request = new AiRagDebugRequest();
        request.setQuery("缓存一致性");
        request.setMetadataFiltersJson("[]");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.debugRag(request));

        assertTrue(exception.getMessage().contains("metadataFiltersJson"));
    }

    @Test
    void shouldReturnRagServiceHealthSummary() {
        when(llamaIndexClient.hasEndpointConfigured()).thenReturn(true);
        when(llamaIndexClient.health()).thenReturn(new LlamaIndexHealthResponse()
                .setStatus("ok")
                .setAuthEnabled(true)
                .setDocumentCount(8)
                .setSceneCount(3)
                .setDataFile("data/knowledge-base.json"));

        AiRagServiceHealthResponse response = service.getRagServiceHealth();

        assertTrue(response.isReachable());
        assertEquals("ok", response.getStatus());
        assertEquals(8, response.getDocumentCount());
        assertEquals(3, response.getSceneCount());
        assertEquals("http://localhost:18080", response.getEndpoint());
        assertEquals("rag-te******-key", response.getApiKeyMasked());
        assertTrue(response.isApiKeyConfigured());
        verify(llamaIndexClient).health();
    }

    @Test
    void shouldReturnUnavailableRagServiceHealthWhenEndpointMissing() {
        when(llamaIndexClient.hasEndpointConfigured()).thenReturn(false);

        AiRagServiceHealthResponse response = service.getRagServiceHealth();

        assertFalse(response.isReachable());
        assertTrue(response.getMessage().contains("endpoint"));
        verify(llamaIndexClient, never()).health();
    }

    @Test
    void shouldListRagServiceDocuments() {
        when(llamaIndexClient.hasEndpointConfigured()).thenReturn(true);
        when(llamaIndexClient.listDocuments("mock_interview", 20)).thenReturn(new LlamaIndexDocumentListResponse()
                .setTotalCount(1)
                .setDocuments(java.util.List.of(new LlamaIndexDocumentListItem()
                        .setId("doc-1")
                        .setScene("mock_interview")
                        .setTextPreview("缓存一致性常见方案...")
                        .setMetadata(java.util.Map.of("source", "kb")))));

        AiRagServiceDocumentListResponse response = service.getRagServiceDocuments(" mock_interview ", null);

        assertEquals("http://localhost:18080", response.getEndpoint());
        assertEquals("mock_interview", response.getScene());
        assertEquals(20, response.getLimit());
        assertEquals(1, response.getTotalCount());
        assertEquals(1, response.getDocuments().size());
        assertEquals("doc-1", response.getDocuments().get(0).getId());
        verify(llamaIndexClient).listDocuments("mock_interview", 20);
    }

    @Test
    void shouldExportRagServiceDocuments() {
        when(llamaIndexClient.hasEndpointConfigured()).thenReturn(true);
        when(llamaIndexClient.exportDocuments("community")).thenReturn(new LlamaIndexDocumentExportResponse()
                .setTotalCount(1)
                .setDocuments(java.util.List.of(new LlamaIndexKnowledgeDocument()
                        .setId("doc-export-1")
                        .setScene("community")
                        .setText("社区内容总结需要兼顾观点和风险边界")
                        .setMetadata(java.util.Map.of("source", "kb")))));

        AiRagDocumentExportResponse response = service.exportRagDocuments(" community ");

        assertEquals("http://localhost:18080", response.getEndpoint());
        assertEquals("community", response.getScene());
        assertEquals(1, response.getTotalCount());
        assertEquals(1, response.getDocuments().size());
        assertEquals("doc-export-1", response.getDocuments().get(0).getId());
        assertEquals("社区内容总结需要兼顾观点和风险边界", response.getDocuments().get(0).getText());
        verify(llamaIndexClient).exportDocuments("community");
    }

    @Test
    void shouldDeleteRagServiceDocumentById() {
        when(llamaIndexClient.hasEndpointConfigured()).thenReturn(true);
        when(llamaIndexClient.deleteDocument("doc-delete-1")).thenReturn(new LlamaIndexDocumentDeleteResponse()
                .setDocumentId("doc-delete-1")
                .setDeletedCount(1)
                .setTotalCount(3));

        AiRagDocumentDeleteResponse response = service.deleteRagDocument(" doc-delete-1 ");

        assertEquals("http://localhost:18080", response.getEndpoint());
        assertEquals("doc-delete-1", response.getDocumentId());
        assertEquals(1, response.getDeletedCount());
        assertEquals(3, response.getTotalCount());
        verify(llamaIndexClient).deleteDocument("doc-delete-1");
    }

    @Test
    void shouldBatchDeleteRagServiceDocuments() {
        when(llamaIndexClient.hasEndpointConfigured()).thenReturn(true);
        when(llamaIndexClient.deleteDocuments(any())).thenReturn(new LlamaIndexDocumentBatchDeleteResponse()
                .setRequestedCount(3)
                .setDeletedCount(2)
                .setDeletedDocumentIds(java.util.List.of("doc-delete-1", "doc-delete-2"))
                .setMissingCount(1)
                .setMissingDocumentIds(java.util.List.of("missing-doc"))
                .setTotalCount(4));

        AiRagDocumentBatchDeleteRequest request = new AiRagDocumentBatchDeleteRequest();
        request.setDocumentIds(java.util.List.of(" doc-delete-1 ", "missing-doc", "doc-delete-2", "doc-delete-1"));

        AiRagDocumentBatchDeleteResponse response = service.batchDeleteRagDocuments(request);

        assertEquals("http://localhost:18080", response.getEndpoint());
        assertEquals(3, response.getRequestedCount());
        assertEquals(2, response.getDeletedCount());
        assertEquals(java.util.List.of("doc-delete-1", "doc-delete-2"), response.getDeletedDocumentIds());
        assertEquals(1, response.getMissingCount());
        assertEquals(java.util.List.of("missing-doc"), response.getMissingDocumentIds());
        assertEquals(4, response.getTotalCount());

        ArgumentCaptor<com.xiaou.ai.rag.LlamaIndexDocumentBatchDeleteRequest> captor =
                ArgumentCaptor.forClass(com.xiaou.ai.rag.LlamaIndexDocumentBatchDeleteRequest.class);
        verify(llamaIndexClient).deleteDocuments(captor.capture());
        assertEquals(java.util.List.of("doc-delete-1", "missing-doc", "doc-delete-2"), captor.getValue().getDocumentIds());
    }

    @Test
    void shouldImportSampleRagDocuments() {
        when(llamaIndexClient.hasEndpointConfigured()).thenReturn(true);
        when(llamaIndexClient.importDocuments(any())).thenReturn(new LlamaIndexDocumentImportResponse()
                .setImportedCount(4)
                .setTotalCount(4));

        AiRagSampleImportRequest request = new AiRagSampleImportRequest();
        request.setReplace(true);

        AiRagSampleImportResponse response = service.importRagSampleDocuments(request);

        assertEquals("http://localhost:18080", response.getEndpoint());
        assertTrue(response.isReplace());
        assertEquals(4, response.getSampleDocumentCount());
        assertEquals(4, response.getImportedCount());
        assertEquals(4, response.getTotalCount());
        verify(llamaIndexClient).importDocuments(any());
    }

    @Test
    void shouldImportCustomRagDocumentsWithGeneratedIdsAndSceneOverride() {
        when(llamaIndexClient.hasEndpointConfigured()).thenReturn(true);
        when(llamaIndexClient.importDocuments(any())).thenReturn(new LlamaIndexDocumentImportResponse()
                .setImportedCount(2)
                .setTotalCount(6));

        AiRagDocumentImportRequest request = new AiRagDocumentImportRequest();
        request.setReplace(false);
        request.setDefaultScene("sql_optimize");
        request.setDocumentsJson("""
                [
                  {
                    "text": "Using filesort 往往意味着排序未命中索引",
                    "metadata": {
                      "source": "mysql-manual"
                    }
                  },
                  {
                    "id": "doc-custom-2",
                    "scene": "mock_interview",
                    "text": "缓存一致性可以从旁路缓存、延迟双删和消息补偿展开"
                  }
                ]
                """);

        AiRagDocumentImportResponse response = service.importRagDocuments(request);

        assertEquals("http://localhost:18080", response.getEndpoint());
        assertFalse(response.isReplace());
        assertEquals("sql_optimize", response.getDefaultScene());
        assertEquals(2, response.getRequestedDocumentCount());
        assertEquals(2, response.getImportedCount());
        assertEquals(6, response.getTotalCount());
        ArgumentCaptor<com.xiaou.ai.rag.LlamaIndexDocumentImportRequest> captor = ArgumentCaptor.forClass(com.xiaou.ai.rag.LlamaIndexDocumentImportRequest.class);
        verify(llamaIndexClient).importDocuments(captor.capture());
        assertEquals(2, captor.getValue().getDocuments().size());
        assertTrue(captor.getValue().getDocuments().get(0).getId().startsWith("custom-sql_optimize-"));
        assertEquals("sql_optimize", captor.getValue().getDocuments().get(0).getScene());
        assertEquals("doc-custom-2", captor.getValue().getDocuments().get(1).getId());
    }

    @Test
    void shouldUseRuntimeApiKeyWhenRequestKeyMissing() {
        doReturn(chatModel).when(service).createChatModel(any());
        when(chatModel.chat(anyString())).thenReturn("OK");

        AiConfigTestRequest request = new AiConfigTestRequest();
        request.setBaseUrl("https://proxy.example.com/v1/");
        request.setModel("gpt-5.4");

        AiConfigTestResponse response = service.testConfig(request);

        assertTrue(response.isAvailable());
        assertTrue(response.isUsedConfiguredApiKey());
        assertEquals("https://proxy.example.com/v1", response.getBaseUrl());
        assertEquals("gpt-5.4", response.getModel());
        assertEquals("OK", response.getPreview());
        assertEquals("sk-123******cdef", response.getApiKeyMasked());
        assertNotNull(response.getLatencyMs());
    }

    @Test
    void shouldReturnFailureWhenConfigMissing() {
        aiProperties.setApiKey(null);
        AiConfigTestRequest request = new AiConfigTestRequest();
        request.setBaseUrl("https://proxy.example.com/v1");
        request.setModel("gpt-5.4");

        AiConfigTestResponse response = service.testConfig(request);

        assertFalse(response.isAvailable());
        assertTrue(response.getMessage().contains("缺少 API Key"));
    }

    @Test
    void shouldSanitizeErrorMessageWhenInvocationFailed() {
        doReturn(chatModel).when(service).createChatModel(any());
        when(chatModel.chat(anyString())).thenThrow(new RuntimeException("invalid key sk-1234567890abcdef"));

        AiConfigTestRequest request = new AiConfigTestRequest();
        request.setBaseUrl("https://proxy.example.com/v1");
        request.setApiKey("sk-1234567890abcdef");
        request.setModel("gpt-5.4");

        AiConfigTestResponse response = service.testConfig(request);

        assertFalse(response.isAvailable());
        assertTrue(response.getMessage().contains("连通性测试失败"));
        assertFalse(response.getMessage().contains("sk-1234567890abcdef"));
    }

    @Test
    void shouldReturnRuntimeMetricsSnapshot() {
        AiRuntimeMetricsSnapshot snapshot = new AiRuntimeMetricsSnapshot();
        snapshot.getOverview()
                .setTotalInvocations(3)
                .setSuccessCount(2)
                .setErrorCount(1)
                .setTotalTokens(1200);
        snapshot.getModelStats().add(new AiRuntimeModelMetrics()
                .setModelName("gpt-5.4")
                .setInvocations(3)
                .setSuccessCount(2)
                .setErrorCount(1)
                .setTotalTokens(1200));
        when(runtimeMetricsCollector.snapshot(null, null, null, 50)).thenReturn(snapshot);

        AiRuntimeMetricsResponse response = service.getRuntimeMetrics(null, null, null, null);

        assertNotNull(response);
        assertEquals(3, response.getOverview().getTotalInvocations());
        assertEquals(2, response.getOverview().getSuccessCount());
        assertEquals(1, response.getOverview().getErrorCount());
        assertEquals(1200, response.getOverview().getTotalTokens());
        assertEquals("USD", response.getOverview().getCurrency());
        assertTrue(response.getOverview().isPricingConfigured());
        assertEquals(1, response.getModelStats().size());
        assertEquals("gpt-5.4", response.getModelStats().get(0).getModelName());
    }

    @Test
    void shouldNormalizeMetricsFilterBeforeQueryingCollector() {
        AiRuntimeMetricsSnapshot snapshot = new AiRuntimeMetricsSnapshot();
        snapshot.getOverview().setTotalInvocations(1).setErrorCount(1);
        when(runtimeMetricsCollector.snapshot("resume", "error", "gpt-5", 20)).thenReturn(snapshot);

        AiRuntimeMetricsResponse response = service.getRuntimeMetrics(" resume ", "ERROR", " GPT-5 ", 20);

        assertEquals(1, response.getOverview().getTotalInvocations());
        assertEquals(1, response.getOverview().getErrorCount());
        verify(runtimeMetricsCollector).snapshot("resume", "error", "gpt-5", 20);
    }

    @Test
    void shouldRejectUnsupportedOutcomeFilter() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getRuntimeMetrics(null, "fallback", null, null)
        );

        assertTrue(exception.getMessage().contains("outcome"));
    }

    @Test
    void shouldRejectInvalidRecentLimit() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getRuntimeMetrics(null, null, null, 0)
        );

        assertTrue(exception.getMessage().contains("recentLimit"));
    }

    @Test
    void shouldClearRuntimeMetricsCollector() {
        service.clearRuntimeMetrics();

        verify(runtimeMetricsCollector).clear();
    }
}
