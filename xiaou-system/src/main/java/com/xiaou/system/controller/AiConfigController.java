package com.xiaou.system.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
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
import com.xiaou.system.service.SysAiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台 AI 配置控制器。
 *
 * @author xiaou
 */
@RestController
@RequestMapping("/admin/ai/config")
@RequiredArgsConstructor
@Tag(name = "管理后台 AI 配置", description = "OpenAI 兼容中转站配置摘要与连通性测试")
public class AiConfigController {

    private final SysAiConfigService aiConfigService;

    /**
     * 获取当前运行时 AI 配置摘要。
     */
    @Operation(summary = "获取当前运行时 AI 配置摘要")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查看 AI 配置需要管理员权限")
    @GetMapping("/runtime")
    public Result<AiRuntimeConfigResponse> getRuntimeConfig() {
        return Result.success("获取 AI 运行时配置成功", aiConfigService.getRuntimeConfig());
    }

    /**
     * 获取 Prompt / RAG / Schema 调试清单。
     */
    @Operation(summary = "获取 Prompt / RAG / Schema 调试清单")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查看 AI 调试清单需要管理员权限")
    @GetMapping("/schema-catalog")
    public Result<AiSchemaCatalogResponse> getSchemaCatalog() {
        return Result.success("获取 AI 调试清单成功", aiConfigService.getSchemaCatalog());
    }

    /**
     * 获取 AI 回归用例目录。
     */
    @Operation(summary = "获取 AI 回归用例目录")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查看 AI 回归用例需要管理员权限")
    @GetMapping("/regression/cases")
    public Result<AiRegressionCaseCatalogResponse> getRegressionCaseCatalog() {
        return Result.success("获取 AI 回归用例成功", aiConfigService.getRegressionCaseCatalog());
    }

    /**
     * 获取最近一次 AI 回归结果。
     */
    @Operation(summary = "获取最近一次 AI 回归结果")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查看 AI 回归结果需要管理员权限")
    @GetMapping("/regression/latest")
    public Result<AiRegressionRunResponse> getLatestRegressionRun() {
        return Result.success("获取最近一次 AI 回归结果成功", aiConfigService.getLatestRegressionRun());
    }

    /**
     * 获取 AI 回归执行历史。
     */
    @Operation(summary = "获取 AI 回归执行历史")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查看 AI 回归历史需要管理员权限")
    @GetMapping("/regression/history")
    public Result<AiRegressionRunHistoryResponse> getRegressionRunHistory(@RequestParam(required = false) Integer limit) {
        return Result.success("获取 AI 回归执行历史成功", aiConfigService.getRegressionRunHistory(limit));
    }

    /**
     * 执行 AI 回归。
     */
    @Operation(summary = "执行 AI 回归")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "执行 AI 回归需要管理员权限")
    @PostMapping("/regression/run")
    @Log(module = "系统管理", type = Log.OperationType.OTHER, description = "执行 AI 黄金样例回归",
            saveRequestData = false, saveResponseData = false)
    public Result<AiRegressionRunResponse> runRegression(@RequestBody(required = false) AiRegressionRunRequest request) {
        return Result.success("执行 AI 回归完成", aiConfigService.runRegression(request));
    }

    /**
     * Prompt 在线调试。
     */
    @Operation(summary = "Prompt 在线调试")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "调试 AI Prompt 需要管理员权限")
    @PostMapping("/prompt-debug")
    @Log(module = "系统管理", type = Log.OperationType.OTHER, description = "AI Prompt 在线调试",
            saveRequestData = false, saveResponseData = false)
    public Result<AiPromptDebugResponse> debugPrompt(@RequestBody AiPromptDebugRequest request) {
        return Result.success("Prompt 在线调试完成", aiConfigService.debugPrompt(request));
    }

    /**
     * LlamaIndex 检索调试。
     */
    @Operation(summary = "LlamaIndex 检索调试")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "调试知识库检索需要管理员权限")
    @PostMapping("/rag-debug")
    @Log(module = "系统管理", type = Log.OperationType.OTHER, description = "LlamaIndex 检索调试",
            saveRequestData = false, saveResponseData = false)
    public Result<AiRagDebugResponse> debugRag(@RequestBody AiRagDebugRequest request) {
        return Result.success("LlamaIndex 检索调试完成", aiConfigService.debugRag(request));
    }

    /**
     * 获取 RAG 服务健康状态。
     */
    @Operation(summary = "获取 RAG 服务健康状态")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查看 RAG 服务状态需要管理员权限")
    @GetMapping("/rag-service/health")
    public Result<AiRagServiceHealthResponse> getRagServiceHealth() {
        return Result.success("获取 RAG 服务状态成功", aiConfigService.getRagServiceHealth());
    }

    /**
     * 获取 RAG 服务文档列表。
     */
    @Operation(summary = "获取 RAG 服务文档列表")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查看 RAG 服务文档需要管理员权限")
    @GetMapping("/rag-service/documents")
    public Result<AiRagServiceDocumentListResponse> getRagServiceDocuments(@RequestParam(required = false) String scene,
                                                                           @RequestParam(required = false) Integer limit) {
        return Result.success("获取 RAG 服务文档成功", aiConfigService.getRagServiceDocuments(scene, limit));
    }

    /**
     * 导出 RAG 服务完整文档。
     */
    @Operation(summary = "导出 RAG 服务完整文档")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "导出 RAG 服务文档需要管理员权限")
    @GetMapping("/rag-service/documents/export")
    @Log(module = "系统管理", type = Log.OperationType.EXPORT, description = "导出 RAG 服务文档",
            saveRequestData = false, saveResponseData = false)
    public Result<AiRagDocumentExportResponse> exportRagDocuments(@RequestParam(required = false) String scene) {
        return Result.success("导出 RAG 服务文档成功", aiConfigService.exportRagDocuments(scene));
    }

    /**
     * 导入 RAG 样例知识。
     */
    @Operation(summary = "导入 RAG 样例知识")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "导入 RAG 样例知识需要管理员权限")
    @PostMapping("/rag-service/sample-import")
    @Log(module = "系统管理", type = Log.OperationType.IMPORT, description = "导入 RAG 样例知识",
            saveRequestData = false, saveResponseData = false)
    public Result<AiRagSampleImportResponse> importRagSampleDocuments(@RequestBody(required = false) AiRagSampleImportRequest request) {
        return Result.success("RAG 样例知识导入完成", aiConfigService.importRagSampleDocuments(request));
    }

    /**
     * 导入自定义 RAG 文档。
     */
    @Operation(summary = "导入自定义 RAG 文档")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "导入自定义 RAG 文档需要管理员权限")
    @PostMapping("/rag-service/documents/import")
    @Log(module = "系统管理", type = Log.OperationType.IMPORT, description = "导入自定义 RAG 文档",
            saveRequestData = false, saveResponseData = false)
    public Result<AiRagDocumentImportResponse> importRagDocuments(@RequestBody AiRagDocumentImportRequest request) {
        return Result.success("RAG 自定义文档导入完成", aiConfigService.importRagDocuments(request));
    }

    /**
     * 删除指定 RAG 文档。
     */
    @Operation(summary = "删除指定 RAG 文档")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "删除 RAG 文档需要管理员权限")
    @DeleteMapping("/rag-service/documents")
    @Log(module = "系统管理", type = Log.OperationType.DELETE, description = "删除指定 RAG 文档",
            saveRequestData = false, saveResponseData = false)
    public Result<AiRagDocumentDeleteResponse> deleteRagDocument(@RequestParam String documentId) {
        return Result.success("删除 RAG 文档成功", aiConfigService.deleteRagDocument(documentId));
    }

    /**
     * 批量删除 RAG 文档。
     */
    @Operation(summary = "批量删除 RAG 文档")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "批量删除 RAG 文档需要管理员权限")
    @PostMapping("/rag-service/documents/batch-delete")
    @Log(module = "系统管理", type = Log.OperationType.DELETE, description = "批量删除 RAG 文档",
            saveRequestData = false, saveResponseData = false)
    public Result<AiRagDocumentBatchDeleteResponse> batchDeleteRagDocuments(@RequestBody AiRagDocumentBatchDeleteRequest request) {
        return Result.success("批量删除 RAG 文档成功", aiConfigService.batchDeleteRagDocuments(request));
    }

    /**
     * 获取 AI 运行观测数据。
     */
    @Operation(summary = "获取 AI 运行观测数据")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "查看 AI 运行观测需要管理员权限")
    @GetMapping("/metrics")
    public Result<AiRuntimeMetricsResponse> getRuntimeMetrics(@RequestParam(required = false) String scene,
                                                              @RequestParam(required = false) String outcome,
                                                              @RequestParam(required = false) String model,
                                                              @RequestParam(required = false) Integer recentLimit) {
        return Result.success("获取 AI 运行观测成功", aiConfigService.getRuntimeMetrics(scene, outcome, model, recentLimit));
    }

    /**
     * 清空 AI 运行观测数据。
     */
    @Operation(summary = "清空 AI 运行观测数据")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "清空 AI 运行观测需要管理员权限")
    @DeleteMapping("/metrics")
    @Log(module = "系统管理", type = Log.OperationType.CLEAN, description = "清空 AI 运行观测",
            saveRequestData = false, saveResponseData = false)
    public Result<Void> clearRuntimeMetrics() {
        aiConfigService.clearRuntimeMetrics();
        return Result.success("AI 运行观测已清空", null);
    }

    /**
     * 测试 OpenAI 兼容中转站配置。
     */
    @Operation(summary = "测试 OpenAI 兼容中转站配置")
    @SecurityRequirement(name = "Bearer Token")
    @RequireAdmin(message = "测试 AI 配置需要管理员权限")
    @PostMapping("/test")
    @Log(module = "系统管理", type = Log.OperationType.OTHER, description = "测试 AI 配置",
            saveRequestData = false, saveResponseData = false)
    public Result<AiConfigTestResponse> testConfig(@RequestBody(required = false) AiConfigTestRequest request) {
        return Result.success("AI 配置测试完成", aiConfigService.testConfig(request));
    }
}
