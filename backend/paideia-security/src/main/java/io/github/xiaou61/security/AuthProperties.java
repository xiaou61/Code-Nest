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
 */
@ConfigurationProperties(prefix = "paideia.auth")
public record AuthProperties(String secret, String issuer, Duration tokenTtl) {

    private static final int MIN_SECRET_LENGTH = 32;

    public AuthProperties {
        if (hasText(secret) && secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "paideia.auth.secret 长度不足 " + MIN_SECRET_LENGTH + " 字符，HS256 的密钥强度不足");
        }
        issuer = hasText(issuer) ? issuer : "paideia";
        tokenTtl = (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) ? Duration.ofHours(2) : tokenTtl;
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
