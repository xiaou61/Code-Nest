package com.xiaou.system.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 运行时配置摘要。
 *
 * @author xiaou
 */
@Data
public class AiRuntimeConfigResponse {

    /**
     * 当前 provider。
     */
    private String provider;

    /**
     * 当前基础地址。
     */
    private String baseUrl;

    /**
     * 当前对话模型。
     */
    private String model;

    /**
     * 是否已具备完整运行配置。
     */
    private boolean configured;

    /**
     * 是否已配置 API Key。
     */
    private boolean apiKeyConfigured;

    /**
     * 脱敏后的 API Key。
     */
    private String apiKeyMasked;

    /**
     * 是否已配置成本单价。
     */
    private boolean pricingConfigured;

    /**
     * 成本币种。
     */
    private String pricingCurrency;

    /**
     * 每百万输入 Token 单价。
     */
    private BigDecimal inputPricePerMillion;

    /**
     * 每百万输出 Token 单价。
     */
    private BigDecimal outputPricePerMillion;

    /**
     * 是否已启用运行观测持久化。
     */
    private boolean metricsPersistenceEnabled;

    /**
     * 当前运行观测持久化模式。
     */
    private String metricsPersistenceMode;

    /**
     * 是否启用 RAG 检索。
     */
    private boolean ragEnabled;

    /**
     * RAG 检索服务地址。
     */
    private String ragEndpoint;

    /**
     * 是否已配置 RAG API Key。
     */
    private boolean ragApiKeyConfigured;

    /**
     * 脱敏后的 RAG API Key。
     */
    private String ragApiKeyMasked;

    /**
     * RAG 默认 TopK。
     */
    private Integer ragDefaultTopK;
}
