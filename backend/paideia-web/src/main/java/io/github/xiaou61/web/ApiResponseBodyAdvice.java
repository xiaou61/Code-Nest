package io.github.xiaou61.web;

import io.github.xiaou61.platform.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 把控制器返回值统一包装成 {@link ApiResponse}，并补上追踪标识。
 *
 * <p>两处必须跳过：{@code /actuator/**} 是运维端点，包装会破坏其既有响应结构；
 * {@code String} 返回值走的是 {@code StringHttpMessageConverter}，包装成对象会抛类型转换错误。
 * 骨架期不提供返回 String 的接口，遇到时按原样返回，而不是悄悄改变响应类型。
 */
@RestControllerAdvice
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> || body instanceof String || isActuator(request)) {
            return body;
        }
        return ApiResponse.ok(body).withTraceId(traceIdOf(request));
    }

    private boolean isActuator(ServerHttpRequest request) {
        return request.getURI().getPath().startsWith("/actuator");
    }

    private String traceIdOf(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            Object value = servletRequest.getServletRequest().getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
            return value == null ? null : value.toString();
        }
        return null;
    }
}
