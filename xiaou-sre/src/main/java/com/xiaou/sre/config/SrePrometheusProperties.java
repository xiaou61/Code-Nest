package com.xiaou.sre.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SRE Prometheus 只读证据配置。
 *
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.sre.prometheus")
public class SrePrometheusProperties {

    /** 是否启用 Prometheus 证据采集。 */
    private boolean enabled;

    /** Prometheus HTTP API 地址，只允许配置注入，不从告警内容读取。 */
    private String endpoint = "http://127.0.0.1:9090";

    /** 建连超时（毫秒）。 */
    private int connectTimeoutMillis = 2_000;

    /** 响应读取超时（毫秒）。 */
    private int readTimeoutMillis = 5_000;

    /** 单次响应最大字节数。 */
    private int maxResponseBytes = 512_000;

    /** 单次证据最多保存的时序结果条数。 */
    private int maxResults = 50;

    public int normalizedConnectTimeoutMillis() {
        return Math.max(200, Math.min(connectTimeoutMillis, 30_000));
    }

    public int normalizedReadTimeoutMillis() {
        return Math.max(500, Math.min(readTimeoutMillis, 60_000));
    }

    public int normalizedMaxResponseBytes() {
        return Math.max(16_384, Math.min(maxResponseBytes, 5_000_000));
    }

    public int normalizedMaxResults() {
        return Math.max(1, Math.min(maxResults, 500));
    }
}
