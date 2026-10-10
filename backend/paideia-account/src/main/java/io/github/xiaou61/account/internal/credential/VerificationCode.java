package io.github.xiaou61.account.internal.credential;

import io.github.xiaou61.account.internal.support.Digest;
import java.time.Instant;
import java.util.Locale;

/**
 * 一枚邮箱验证码的状态。
 *
 * <p>刻意是可变对象：尝试次数要在**不刷新存活期**的前提下递增。如果每次尝试都重新
 * {@code put}，过期时间会被顺延，攻击者就能靠不断尝试让验证码永不过期。
 */
public final class VerificationCode {

    private final String codeHash;
    private final Instant expiresAt;
    private final String emailHash;
    private int attempts;

    public VerificationCode(String emailHash, String code, Instant expiresAt) {
        this.emailHash = emailHash;
        this.codeHash = Digest.sha256Hex(normalize(code));
        this.expiresAt = expiresAt;
    }

    public String emailHash() {
        return emailHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean matches(String candidate) {
        return candidate != null && codeHash.equals(Digest.sha256Hex(normalize(candidate)));
    }

    public int incrementAttempts() {
        attempts += 1;
        return attempts;
    }

    public int attempts() {
        return attempts;
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
