package com.xiaou.system.service;

import com.xiaou.system.dto.AiConfigTestRequest;
import com.xiaou.system.dto.AiConfigTestResponse;
import com.xiaou.system.dto.AiPromptDebugRequest;
import com.xiaou.system.dto.AiPromptDebugResponse;
import com.xiaou.system.dto.AiRegressionCaseCatalogResponse;
import com.xiaou.system.dto.AiRegressionRunHistoryResponse;
import com.xiaou.system.dto.AiRegressionRunRequest;
import com.xiaou.system.dto.AiRegressionRunResponse;
import com.xiaou.system.dto.AiRagDocumentBatchDeleteRequest;
import com.xiaou.system.dto.AiRagDocumentBatchDeleteResponse;
import com.xiaou.system.dto.AiRagDocumentImportRequest;
import com.xiaou.system.dto.AiRagDocumentImportResponse;
import com.xiaou.system.dto.AiRagDocumentDeleteResponse;
import com.xiaou.system.dto.AiRagDocumentExportResponse;
import com.xiaou.system.dto.AiRagDebugRequest;
import com.xiaou.system.dto.AiRagDebugResponse;
import com.xiaou.system.dto.AiRagSampleImportRequest;
import com.xiaou.system.dto.AiRagSampleImportResponse;
import com.xiaou.system.dto.AiRagServiceDocumentListResponse;
import com.xiaou.system.dto.AiRagServiceHealthResponse;
import com.xiaou.system.dto.AiRuntimeConfigResponse;
import com.xiaou.system.dto.AiRuntimeMetricsResponse;
import com.xiaou.system.dto.AiSchemaCatalogResponse;

/**
 * 管理后台 AI 配置服务。
 *
 * @author xiaou
 */
public interface SysAiConfigService {

    /**
     * 获取当前运行时 AI 配置摘要。
     */
    AiRuntimeConfigResponse getRuntimeConfig();

    /**
     * 获取 Prompt / RAG / Schema 调试清单。
     */
    AiSchemaCatalogResponse getSchemaCatalog();

    /**
     * 获取 AI 回归用例目录。
     */
    AiRegressionCaseCatalogResponse getRegressionCaseCatalog();

    /**
     * 执行 AI 回归。
     */
    AiRegressionRunResponse runRegression(AiRegressionRunRequest request);

    /**
     * 获取最近一次 AI 回归结果。
     */
    AiRegressionRunResponse getLatestRegressionRun();

    /**
     * 获取 AI 回归执行历史。
     */
    AiRegressionRunHistoryResponse getRegressionRunHistory(Integer limit);

    /**
     * Prompt 在线调试。
     */
    AiPromptDebugResponse debugPrompt(AiPromptDebugRequest request);

    /**
     * LlamaIndex 检索调试。
     */
    AiRagDebugResponse debugRag(AiRagDebugRequest request);

    /**
     * 获取 RAG 服务健康状态。
     */
    AiRagServiceHealthResponse getRagServiceHealth();

    /**
     * 获取 RAG 服务文档列表。
     */
    AiRagServiceDocumentListResponse getRagServiceDocuments(String scene, Integer limit);

    /**
     * 导出 RAG 服务完整文档。
     */
    AiRagDocumentExportResponse exportRagDocuments(String scene);

    /**
     * 导入 RAG 样例知识。
     */
    AiRagSampleImportResponse importRagSampleDocuments(AiRagSampleImportRequest request);

    /**
     * 导入自定义 RAG 文档。
     */
    AiRagDocumentImportResponse importRagDocuments(AiRagDocumentImportRequest request);

    /**
     * 删除指定 RAG 文档。
     */
    AiRagDocumentDeleteResponse deleteRagDocument(String documentId);

    /**
     * 批量删除 RAG 文档。
     */
    AiRagDocumentBatchDeleteResponse batchDeleteRagDocuments(AiRagDocumentBatchDeleteRequest request);

    /**
     * 测试 OpenAI 兼容中转站配置。
     */
    AiConfigTestResponse testConfig(AiConfigTestRequest request);

    /**
     * 获取 AI 运行观测数据。
     */
    AiRuntimeMetricsResponse getRuntimeMetrics(String scene, String outcome, String model, Integer recentLimit);

    /**
     * 清空 AI 运行观测数据。
     */
    void clearRuntimeMetrics();
}
