package com.xiaou.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 统一 AI 配置。
 *
 * <p>该配置用于承载模型提供商、模型名称、超时和检索服务等通用能力，
 * 避免在公共模块继续保留特定平台 SDK 配置。</p>
 *
 * @author xiaou
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "xiaou.ai")
public class AiProperties {

    /**
     * 是否启用统一 AI 运行时。
     */
    private boolean enabled = true;

    /**
     * 提供商类型，当前优先支持 OpenAI 兼容协议。
     */
    private String provider = "openai-compatible";

    /**
     * 兼容 OpenAI 的基础地址。
     */
    private String baseUrl;

    /**
     * 模型访问密钥。
     */
    private String apiKey;

    /**
     * 模型配置。
     */
    private Model model = new Model();

    /**
     * 超时配置。
     */
    private Timeout timeout = new Timeout();

    /**
     * 重试配置。
     */
    private Retry retry = new Retry();

    /** Maximum number of in-flight model calls per application instance. */
    private Integer maxConcurrentCalls = 8;

    /** Time to wait for an AI concurrency permit, in milliseconds. */
    private Integer permitAcquireTimeoutMs = 1000;

    /**
     * 检索增强配置。
     */
    private Rag rag = new Rag();

    /**
     * 成本估算配置。
     */
    private Pricing pricing = new Pricing();

    /**
     * 运行观测配置。
     */
    private Metrics metrics = new Metrics();

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }

    public boolean hasBaseUrl() {
        return StringUtils.hasText(baseUrl);
    }

    @Data
    public static class Model {
        /**
         * 对话模型名称。
         */
        private String chat = "gpt-4o-mini";

        /**
         * 单次对话允许消耗的最大 completion token 数。
         *
         * <p>对支持推理的模型，这个上限同时约束推理和最终输出，
         * 防止复杂 Prompt 在没有边界时持续占用请求连接。</p>
         */
        private Integer maxCompletionTokens = 2048;

        /**
         * 向量模型名称。
         */
        private String embedding = "text-embedding-3-small";
    }

    @Data
    public static class Timeout {
        /**
         * 连接超时，单位毫秒。
         */
        private Integer connectMs = 10000;

        /**
         * 读取超时，单位毫秒。
         */
        private Integer readMs = 60000;
    }

    @Data
    public static class Retry {
        /**
         * 最大重试次数。
         */
        private Integer maxAttempts = 2;

        /**
         * 重试退避时间，单位毫秒。
         */
        private Integer backoffMs = 1000;
    }

    @Data
    public static class Rag {
        /**
         * 是否启用检索增强。
         */
        private boolean enabled = false;

        /**
         * LlamaIndex 服务入口。
         */
        private String endpoint;

        /**
         * LlamaIndex 服务密钥。
         */
        private String apiKey;

        /**
         * 默认检索条数。
         */
        private Integer defaultTopK = 5;
    }

    @Data
    public static class Pricing {
        /**
         * 计价币种。
         */
        private String currency = "USD";

        /**
         * 每百万输入 Token 单价。
         */
        private Double inputPerMillion = 0D;

        /**
         * 每百万输出 Token 单价。
         */
        private Double outputPerMillion = 0D;

        public boolean configured() {
            return (inputPerMillion != null && inputPerMillion > 0D)
                    || (outputPerMillion != null && outputPerMillion > 0D);
        }
    }

    @Data
    public static class Metrics {
        /**
         * 运行观测持久化配置。
         */
        private Persistence persistence = new Persistence();
    }

    @Data
    public static class Persistence {
        /**
         * 是否启用运行观测持久化。
         */
        private boolean enabled = true;

        /**
         * Redis 中存储运行观测的 Key。
         */
        private String redisKey = "xiaou:ai:runtime:metrics";
    }
}
