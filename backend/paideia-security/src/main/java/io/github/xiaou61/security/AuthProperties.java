package io.github.xiaou61.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证配置。
 *
 * <p>未配置 {@code secret} 时由装配层生成一次性随机密钥并告警（见
 * {@link SecurityConfiguration#authPort}）：这样骨架不配任何东西也能起来，
 * 而随机密钥无法被跨重启利用，风险远低于一个写死的默认密钥。
 * 配置了密钥就校验强度，避免弱密钥悄悄生效。
 *
 * <p><b>默认存活期是 15 分钟</b>（WORK-003 决定）——这是"短 access + refresh 轮换"方案的一半：
 * access 无状态、签出后无法撤销，所以它的寿命就是"登出后令牌仍然可用的窗口"。
 * 骨架期这个默认值是 2 小时，与"短 access"的定位不符，已改为 15 分钟；
 * 需要更长可以在配置里显式覆盖（`paideia.auth.token-ttl`）。
 */
@ConfigurationProperties(prefix = "paideia.auth")
public record AuthProperties(String secret, String issuer, Duration tokenTtl) {

    /** access 的默认存活期。改这个值等于改"登出后令牌还能用多久"的窗口。 */
    private static final Duration DEFAULT_TOKEN_TTL = Duration.ofMinutes(15);

    private static final int MIN_SECRET_LENGTH = 32;

    public AuthProperties {
        if (hasText(secret) && secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "paideia.auth.secret 长度不足 " + MIN_SECRET_LENGTH + " 字符，HS256 的密钥强度不足");
        }
        issuer = hasText(issuer) ? issuer : "paideia";
        tokenTtl = (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative())
                ? DEFAULT_TOKEN_TTL
                : tokenTtl;
    }

    public boolean hasSecret() {
        return hasText(secret);
    }

    /** 换一个密钥，其余配置不变。 */
    public AuthProperties withSecret(String value) {
        return new AuthProperties(value, issuer, tokenTtl);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
