package com.xiaou.sre.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Alertmanager webhook 机器认证配置。
 *
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.sre.webhook")
public class SreWebhookProperties {

    /**
     * 默认关闭，避免未配置凭据时暴露内部入口。
     */
    private boolean enabled;

    /**
     * Alertmanager 专用 Bearer secret，不得复用用户或管理员 Token。
     */
    private String token = "";

    /**
     * 单个进程每分钟最多接受的 webhook 请求数。
     */
    private int maxRequestsPerMinute = 120;

    /**
     * 请求体上限，防止错误或恶意 payload 消耗应用内存。
     */
    private long maxBodyBytes = 2_000_000L;
}
