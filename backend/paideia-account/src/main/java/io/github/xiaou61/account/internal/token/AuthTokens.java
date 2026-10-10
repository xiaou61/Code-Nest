package io.github.xiaou61.account.internal.token;

import io.github.xiaou61.account.internal.user.User;

/**
 * 一组新签发的令牌。
 *
 * <p>时间用 ISO-8601 字符串而不是时间戳：与既有 `AuthController` 的做法一致，
 * 不依赖 Jackson 的日期序列化配置。
 *
 * <p>{@link UserView} 是唯一对外的用户视图——**口令摘要、邮箱验证时间等内部字段不外泄**。
 */
public record AuthTokens(
        String accessToken,
        String accessExpiresAt,
        String refreshToken,
        String refreshExpiresAt,
        UserView user) {

    public record UserView(String id, String username, String email, String role) {

        public static UserView of(User user) {
            return new UserView(
                    String.valueOf(user.getId()), user.getUsername(), user.getEmail(), user.getRole());
        }
    }
}
