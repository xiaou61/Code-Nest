package com.xiaou.system.dto;

import lombok.Data;

/**
 * AI 配置测试请求。
 *
 * <p>字段允许为空；为空时优先回退到当前服务端运行配置。</p>
 *
 * @author xiaou
 */
@Data
public class AiConfigTestRequest {

    /**
     * OpenAI 兼容中转站地址。
     */
    private String baseUrl;

    /**
     * API Key。
     */
    private String apiKey;

    /**
     * 对话模型名称。
     */
    private String model;
}
