package io.github.xiaou61.security;

import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前请求的身份。
 *
 * <p>身份只从安全上下文读取，也就是只从令牌来。接口层不允许接受客户端传入的用户标识——
 * 一旦允许，越权读取就只是改一个参数的问题。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static String subjectId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BizException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }
}
