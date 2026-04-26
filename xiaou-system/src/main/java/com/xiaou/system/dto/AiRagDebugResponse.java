package com.xiaou.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * LlamaIndex 检索调试响应。
 *
 * @author xiaou
 */
@Data
public class AiRagDebugResponse {

    /**
     * 使用的检索画像。
     */
    private String profileId;

    /**
     * 是否启用 RAG。
     */
    private boolean ragEnabled;

    /**
     * 检索服务地址。
     */
    private String endpoint;

    /**
     * 是否已配置检索 API Key。
     */
    private boolean apiKeyConfigured;

    /**
     * 脱敏后的检索 API Key。
     */
    private String apiKeyMasked;

    /**
     * 查询文本。
     */
    private String query;

    /**
     * 检索场景。
     */
    private String scene;

    /**
     * 检索返回条数。
     */
    private Integer topK;

    /**
     * 元数据过滤 JSON。
     */
    private String metadataFiltersJson;

    /**
     * 调用耗时。
     */
    private Long latencyMs;

    /**
     * 是否为降级结果。
     */
    private boolean fallback;

    /**
     * 命中节点数量。
     */
    private int nodeCount;

    /**
     * 参考上下文片段。
     */
    private String contextSnippet;

    /**
     * 命中节点。
     */
    private List<AiRagDebugNodeResponse> nodes = new ArrayList<>();
}
