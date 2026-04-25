package com.xiaou.system.dto;

import lombok.Data;

/**
 * AI 配置测试响应。
 *
 * @author xiaou
 */
@Data
public class AiConfigTestResponse {

    /**
     * 是否连通成功。
     */
    private boolean available;

    /**
     * provider 名称。
     */
    private String provider;

    /**
     * 测试使用的基础地址。
     */
    private String baseUrl;

    /**
     * 测试使用的模型名称。
     */
    private String model;

    /**
     * 接口耗时，单位毫秒。
     */
    private Long latencyMs;

    /**
     * 简要返回预览。
     */
    private String preview;

    /**
     * 测试说明。
     */
    private String message;

    /**
     * 是否沿用了服务端当前配置的 API Key。
     */
    private boolean usedConfiguredApiKey;

    /**
     * 脱敏后的 API Key。
     */
    private String apiKeyMasked;
}
