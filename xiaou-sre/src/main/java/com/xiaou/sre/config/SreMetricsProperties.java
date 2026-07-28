package com.xiaou.sre.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Database snapshot settings for SRE operational gauges.
 *
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.sre.metrics")
public class SreMetricsProperties {

    private boolean enabled;
    private long refreshMs = 10_000L;
    private long initialDelayMs = 15_000L;
}
