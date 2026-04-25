package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Prompt / RAG / Schema 调试清单响应。
 *
 * @author xiaou
 */
@Data
public class AiSchemaCatalogResponse {

    /**
     * 当前系统中可筛选的业务域。
     */
    private List<String> domains = new ArrayList<>();

    /**
     * Prompt 清单。
     */
    private List<AiPromptCatalogItemResponse> prompts = new ArrayList<>();

    /**
     * RAG 查询模板清单。
     */
    private List<AiRagQueryCatalogItemResponse> ragQueries = new ArrayList<>();

    /**
     * RAG 检索画像清单。
     */
    private List<AiRetrievalProfileResponse> retrievalProfiles = new ArrayList<>();

    /**
     * 结构化输出 Schema 清单。
     */
    private List<AiStructuredSchemaResponse> structuredSchemas = new ArrayList<>();
}
