package io.github.xiaou61.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域放行列表来自配置，只作用于 {@code /api/**}。
 *
 * <p>桌面壳的页面来源不是 {@code http(s)} 域（Tauri 使用自定义协议），必须在这里显式加入
 * 允许列表，否则桌面端调不通后端；只配置 Web 端会让"Web 能用、exe 报跨域"。
 * 未开启 {@code allowCredentials}：令牌通过 Authorization 头传递，不依赖 Cookie。
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebCorsConfiguration implements WebMvcConfigurer {

    private final CorsProperties properties;

    public WebCorsConfiguration(CorsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (properties.allowedOrigins().isEmpty()) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
