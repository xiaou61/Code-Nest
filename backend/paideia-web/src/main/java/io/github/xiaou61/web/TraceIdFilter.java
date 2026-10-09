package io.github.xiaou61.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 为每个请求确定追踪标识，写入 MDC、请求属性与响应头。
 *
 * <p>响应包装与异常处理都从请求属性读取它，所以本过滤器必须最先执行。
 * 上游传来的标识只在符合白名单字符集时沿用——响应头会被回写，未校验的输入可以据此注入内容。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_ATTRIBUTE = "paideia.traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final String MDC_KEY = "traceId";
    private static final String SAFE_TRACE_ID = "[A-Za-z0-9_-]{1,64}";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        MDC.put(MDC_KEY, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String incoming = request.getHeader(TRACE_ID_HEADER);
        if (incoming != null && incoming.matches(SAFE_TRACE_ID)) {
            return incoming;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
