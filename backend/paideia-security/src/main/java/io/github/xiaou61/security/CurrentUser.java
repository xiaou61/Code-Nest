package io.github.xiaou61.security;

import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前请求的身份。
 *
 * <p>身份只从安全上下文读取，也就是只从令牌来。接口层不允许接受客户端传入的用户标识——
 * 一旦允许，越权读取就只是改一个参数的问题。
 */
public final class CurrentUser {

    private static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

    private CurrentUser() {
    }

    public static String subjectId() {
        return authentication().getName();
    }

    /**
     * 当前主体在当前请求中的角色（小写，与令牌 claim 和数据库一致）；没有角色时返回空串。
     *
     * <p>取值来自权限而不是重新解析令牌：权限是安全链已经算过的结果，
     * 再从令牌读一遍等于把同一个判断做两遍、还可能不一致。
     */
    public static String role() {
        for (GrantedAuthority authority : authentication().getAuthorities()) {
            String value = authority.getAuthority();
            if (value.startsWith(ROLE_AUTHORITY_PREFIX)) {
                return value.substring(ROLE_AUTHORITY_PREFIX.length()).toLowerCase(java.util.Locale.ROOT);
            }
        }
        return "";
    }

    private static Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BizException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication;
    }
}
