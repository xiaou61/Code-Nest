package io.github.xiaou61.security;

import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.time.Instant;

/**
 * 认证端口：业务代码只依赖它，不依赖具体令牌机制。
 * 换成 OIDC 等实现时替换该端口，业务代码不动。
 *
 * <p>本端口**不认识账号**：它只接受一个"主体"（id + 角色）并签发令牌。
 * 账号从哪来、密码怎么校验，是账号模块的事——这样把"身份怎么确认"与"令牌怎么签发"分开。
 */
public interface AuthPort {

    String ROLE_ADMIN = "admin";
    String ROLE_LEARNER = "learner";

    /** 为指定主体签发令牌。 */
    Token issue(Subject subject);

    /**
     * 校验令牌并返回主体；令牌无效、过期、或**角色缺失/不可识别**时抛
     * {@code BizException(UNAUTHENTICATED)}。
     *
     * <p>服务端对角色比客户端严格：前端认不出角色时降权为学习者，服务端直接拒绝。
     * 理由是令牌由我们自己签发，合法令牌必然带角色；缺失只可能来自旧版本或伪造，
     * 此时应当拒绝而不是猜。
     */
    Subject authenticate(String token);

    record Token(String value, Instant expiresAt) {
    }

    record Subject(String id, String role) {
    }

    static BizException unauthenticated(String reason) {
        return new BizException(ErrorCode.UNAUTHENTICATED, reason);
    }
}
