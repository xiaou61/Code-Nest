package com.xiaou.sre.security;

import com.xiaou.sre.config.SreWebhookProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Alertmanager webhook 的机器认证和基础限流过滤器。
 *
 * <p>该入口不使用管理员浏览器 Token。默认关闭且 token 为空时返回 404，
 * 避免新接口在部署配置缺失时暴露。</p>
 *
 * @author xiaou
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SreWebhookAuthenticationFilter extends OncePerRequestFilter {

    public static final String WEBHOOK_PATH = "/internal/sre/alertmanager/v1/alerts";

    private final SreWebhookProperties properties;
    private final AtomicLong windowStartMillis = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger windowRequests = new AtomicInteger();

    public SreWebhookAuthenticationFilter(SreWebhookProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!WEBHOOK_PATH.equals(requestPath(request))) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!properties.isEnabled() || !StringUtils.hasText(properties.getToken())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (!matchesBearerToken(authorization, properties.getToken())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Bearer");
            return;
        }

        long contentLength = request.getContentLengthLong();
        if (contentLength > properties.getMaxBodyBytes()) {
            response.setStatus(413);
            return;
        }

        if (!tryAcquire()) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (requestUri == null) {
            return "";
        }
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private boolean matchesBearerToken(String authorization, String expectedToken) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return false;
        }
        String actualToken = authorization.substring(7).trim();
        if (!StringUtils.hasText(actualToken)) {
            return false;
        }
        return MessageDigest.isEqual(
                actualToken.getBytes(StandardCharsets.UTF_8),
                expectedToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean tryAcquire() {
        long now = System.currentTimeMillis();
        long start = windowStartMillis.get();
        if (now - start >= 60_000L) {
            synchronized (this) {
                if (now - windowStartMillis.get() >= 60_000L) {
                    windowStartMillis.set(now);
                    windowRequests.set(0);
                }
            }
        }
        return windowRequests.incrementAndGet() <= Math.max(properties.getMaxRequestsPerMinute(), 1);
    }
}
