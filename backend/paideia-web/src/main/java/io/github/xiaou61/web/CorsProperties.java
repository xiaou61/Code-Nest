package io.github.xiaou61.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 允许的跨域来源。默认空列表，即不做任何跨域放行——需要时必须显式配置。
 */
@ConfigurationProperties(prefix = "paideia.web.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
