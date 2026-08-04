package com.xiaou.web.growthcoach.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用户侧 Growth Coach 的受控运行配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.growth-coach")
public class GrowthCoachProperties {

    /** 是否开放用户侧 Growth Coach 写能力。 */
    private boolean enabled = false;

    /** 是否允许创建新的计划调整预览。 */
    private boolean previewEnabled = false;

    /** 是否允许确认并写入计划调整。 */
    private boolean confirmEnabled = false;

    /** Maximum time allowed for one read-only briefing source, in milliseconds. */
    private int briefingTimeoutMillis = 1500;

    /** Short cache for the read-only briefing, in milliseconds. */
    private int briefingCacheTtlMillis = 5000;

    /** Maximum number of user briefings kept in the local cache. */
    private int briefingCacheMaxEntries = 10000;

    /** 同一用户规划请求的 Redis 分布式锁前缀。 */
    private String planningLockKeyPrefix = "xiaou:growth-coach:planning";

    private RateLimit rateLimit = new RateLimit();

    private EvidenceProjection evidenceProjection = new EvidenceProjection();

    private ProactiveNudge proactiveNudge = new ProactiveNudge();

    private CodeArtifact codeArtifact = new CodeArtifact();

    private GithubOAuth githubOAuth = new GithubOAuth();

    private CodeReview codeReview = new CodeReview();

    @Data
    public static class RateLimit {
        /** Redis Key 前缀。 */
        private String redisKeyPrefix = "xiaou:growth-coach:rate";

        /** 固定窗口长度，单位秒。小于等于零时使用 60 秒。 */
        private int windowSeconds = 60;

        /** 单用户每窗口最多创建的非幂等预览数。零表示不限制。 */
        private int previewRequestsPerMinute = 6;

        /** 单用户每窗口最多确认的计划调整数。零表示不限制。 */
        private int confirmRequestsPerMinute = 12;
    }

    @Data
    public static class EvidenceProjection {
        /** 是否允许用户读取和后台任务触发证据投影。 */
        private boolean enabled = true;

        /** 是否开启近期变更的定时补偿。 */
        private boolean compensationEnabled = true;

        /** 补偿扫描 cron。 */
        private String compensationCron = "0 */10 * * * ?";

        /** 仅扫描最近多少分钟内发生变化的来源记录。 */
        private int compensationLookbackMinutes = 1440;

        /** 每个来源单次最多发现多少个用户。 */
        private int compensationUserBatchSize = 100;
    }

    @Data
    public static class ProactiveNudge {
        /** 是否开启高风险节奏的站内提醒。 */
        private boolean enabled = false;

        /** 主动扫描 cron。 */
        private String cron = "0 15 9 * * ?";

        /** 单次最多检查的本周计划用户数。 */
        private int userBatchSize = 100;
    }

    @Data
    public static class CodeArtifact {
        /** 是否允许用户验证和附加公开 GitHub commit/PR 来源。 */
        private boolean enabled = false;

        /** GitHub API 建连超时，单位毫秒。 */
        private int connectTimeoutMillis = 3000;

        /** GitHub API 响应超时，单位毫秒。 */
        private int readTimeoutMillis = 8000;

        /** 单用户每个固定窗口最多验证次数。零表示不限制。 */
        private int previewRequestsPerMinute = 5;

        /** 单用户每个固定窗口最多附加次数。零表示不限制。 */
        private int attachRequestsPerMinute = 5;

        /** 单次 GitHub 绑定后最多补验多少条已有公开代码来源。 */
        private int ownershipRecheckLimit = 8;
    }

    /**
     * GitHub OAuth 账号绑定配置。
     *
     * <p>令牌相关字段不使用 Lombok {@code @Data}，避免配置对象意外输出时泄露密钥或客户端密钥。</p>
     */
    @Getter
    @Setter
    public static class GithubOAuth {

        /** OAuth 能力总开关；即使开启，缺少任一必需配置仍保持不可用。 */
        private boolean enabled = false;

        /** GitHub OAuth App Client ID。 */
        private String clientId = "";

        /** GitHub OAuth App Client Secret，只允许通过环境变量注入。 */
        private String clientSecret = "";

        /** 与 GitHub App 登记值完全一致的固定回调地址。 */
        private String callbackUrl = "";

        /** 授权成功后的固定前端回跳地址。 */
        private String successRedirectUrl = "";

        /** 授权失败后的固定前端回跳地址。 */
        private String failureRedirectUrl = "";

        /** Base64/Base64URL 编码的 32 字节 AES-256-GCM 令牌加密密钥。 */
        private String tokenEncryptionKey = "";

        /** Redis 一次性授权 state 的键前缀。 */
        private String stateKeyPrefix = "xiaou:growth-coach:github-oauth:state";

        /** 授权 state 的存活秒数。 */
        private int stateTtlSeconds = 600;

        /** GitHub OAuth 建连超时，单位毫秒。 */
        private int connectTimeoutMillis = 3000;

        /** GitHub OAuth 响应超时，单位毫秒。 */
        private int readTimeoutMillis = 8000;

        /** 单用户每个固定窗口最多发起授权次数。 */
        private int authorizeRequestsPerMinute = 3;

        /** 单用户每个固定窗口最多处理回调次数。 */
        private int callbackRequestsPerMinute = 3;
    }

    @Data
    public static class CodeReview {
        /** 是否允许对用户自有的已保存 CodePen 发起 AI 审查。 */
        private boolean enabled = false;

        /** 单次送入模型的 HTML/CSS/JavaScript 合计最大字符数。 */
        private int maxSourceChars = 24000;

        /** 单用户每个固定窗口最多发起审查次数。零表示不限制。 */
        private int reviewRequestsPerMinute = 3;
    }
}
