package com.xiaou.sensitive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Runtime limits for remote sensitive-word sources. */
@Data
@Component
@ConfigurationProperties(prefix = "xiaou.sensitive.source")
public class SensitiveSourceProperties {

    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 15000;
    private int maxResponseBytes = 2 * 1024 * 1024;
    private int maxRedirects = 0;
    private boolean allowHttp = true;
}
