package com.xiaou.sre.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SRE Loki 只读日志证据配置。
 *
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.sre.loki")
public class SreLokiProperties {

    /** 是否启用 Loki 证据采集。 */
    private boolean enabled;

    /** Loki HTTP API 地址，只允许配置注入，不从告警内容读取。 */
    private String endpoint = "http://127.0.0.1:3100";

    /** 建连超时（毫秒）。 */
    private int connectTimeoutMillis = 2_000;

    /** 响应读取超时（毫秒）。 */
    private int readTimeoutMillis = 5_000;

    /** 单次响应最大字节数。 */
    private int maxResponseBytes = 512_000;

    /** 单次证据最多保存的日志流条数。 */
    private int maxResults = 50;

    /** 单条日志最多保存的字符数。 */
    private int maxLineLength = 2_000;

    /** 默认查询窗口（分钟）。 */
    private int lookbackMinutes = 15;

    /** 查询窗口硬上限（分钟）。 */
    private int maxRangeMinutes = 60;

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

    public int normalizedMaxLineLength() {
        return Math.max(128, Math.min(maxLineLength, 10_000));
    }

    public int normalizedMaxRangeMinutes() {
        return Math.max(1, Math.min(maxRangeMinutes, 24 * 60));
    }

    public int normalizedLookbackMinutes() {
        return Math.max(1, Math.min(lookbackMinutes, normalizedMaxRangeMinutes()));
    }
}
