package com.xiaou.common.web;

import com.xiaou.common.core.domain.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ResultHttpStatusAdvice implements ResponseBodyAdvice<Result<?>> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Result<?> beforeBodyWrite(Result<?> body,
                                     MethodParameter returnType,
                                     MediaType selectedContentType,
                                     Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                     ServerHttpRequest request,
                                     ServerHttpResponse response) {
        if (body != null && !body.isSuccess()) {
            response.setStatusCode(ResultHttpStatusMapper.resolve(body.getCode()));
        }
        return body;
    }
}
