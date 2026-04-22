package com.xiaou.system.dto;

import lombok.Data;

/**
 * RAG 服务健康状态响应。
 *
 * @author xiaou
 */
@Data
public class AiRagServiceHealthResponse {

    /**
     * 是否启用 RAG。
     */
    private boolean ragEnabled;

    /**
     * RAG 服务地址。
     */
    private String endpoint;

    /**
     * 是否已配置 API Key。
     */
    private boolean apiKeyConfigured;

    /**
     * 脱敏后的 API Key。
     */
    private String apiKeyMasked;

    /**
     * 服务是否可达。
     */
    private boolean reachable;

    /**
     * 健康状态。
     */
    private String status;

    /**
     * 是否启用服务鉴权。
     */
    private boolean authEnabled;

    /**
     * 当前文档数。
     */
    private Integer documentCount;

    /**
     * 场景数。
     */
    private Integer sceneCount;

    /**
     * 数据文件路径。
     */
    private String dataFile;

    /**
     * 调用耗时。
     */
    private Long latencyMs;

    /**
     * 说明信息。
     */
    private String message;
}
