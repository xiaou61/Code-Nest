package io.github.xiaou61.security;

import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.time.Instant;

/**
 * 认证端口：业务代码只依赖它，不依赖具体令牌机制。
 * 换成 OIDC 等实现时替换该端口，业务代码不动。
 */
public interface AuthPort {

    /** 为指定主体签发令牌。 */
    Token issue(String subject);

    /** 校验令牌并返回主体；令牌无效或过期时抛 {@code BizException(UNAUTHENTICATED)}。 */
    Subject authenticate(String token);

    record Token(String value, Instant expiresAt) {
    }

    record Subject(String id) {
    }

    static BizException unauthenticated(String reason) {
        return new BizException(ErrorCode.UNAUTHENTICATED, reason);
    }
}
