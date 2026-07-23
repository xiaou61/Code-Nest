package com.xiaou.sre.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SRE Outbox 后台消费配置。
 *
 * <p>默认关闭，避免在数据库迁移尚未完成时由应用启动后台查询不存在的表。</p>
 *
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.sre.outbox")
public class SreOutboxProperties {

    /** 是否启用后台消费。 */
    private boolean enabled;

    /** 每轮扫描间隔（毫秒）。 */
    private long fixedDelayMillis = 5_000L;

    /** 应用启动后的首次扫描延迟（毫秒）。 */
    private long initialDelayMillis = 10_000L;

    /** 单轮最多尝试领取的事件数。 */
    private int batchSize = 20;

    /** 单个事件最多处理次数，超过后进入 FAILED。 */
    private int maxAttempts = 5;

    /** PROCESSING 事件的租约时间（秒）。 */
    private long leaseSeconds = 120L;

    /** 首次失败后的退避时间（秒）。 */
    private long retryBackoffSeconds = 10L;

    /** 退避时间上限（秒）。 */
    private long maxRetryBackoffSeconds = 900L;

    public int normalizedBatchSize() {
        return Math.max(1, Math.min(batchSize, 100));
    }

    public int normalizedMaxAttempts() {
        return Math.max(1, Math.min(maxAttempts, 20));
    }

    public long normalizedLeaseSeconds() {
        return Math.max(30L, Math.min(leaseSeconds, 3600L));
    }

    public long normalizedRetryBackoffSeconds() {
        return Math.max(1L, Math.min(retryBackoffSeconds, 3600L));
    }

    public long normalizedMaxRetryBackoffSeconds() {
        return Math.max(normalizedRetryBackoffSeconds(), Math.min(maxRetryBackoffSeconds, 86_400L));
    }
}
