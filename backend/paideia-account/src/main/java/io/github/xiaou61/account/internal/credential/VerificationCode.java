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

    /** 一次尝试的判定结果。 */
    public enum Attempt {
        /** 码正确，且本次把它消费掉了：之后任何尝试都不会再通过。 */
        MATCHED,
        /** 码不正确，但还允许继续尝试。 */
        MISMATCH,
        /** 尝试次数已超上限，该验证码应作废。 */
        EXHAUSTED,
        /** 已被消费过（并发重放）。 */
        REPLAYED
    }

    private final String codeHash;
    private final Instant expiresAt;
    private final String emailHash;
    private int attempts;
    private boolean consumed;

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

    /**
     * 记一次尝试并判定结果。
     *
     * <p>「计数 + 比对 + 消费」必须在**同一个原子操作**里完成。拆成
     * {@code incrementAttempts()} 与 {@code matches()} 两步调用时，并发提交会让
     * 计数丢更新（实际尝试次数少于提交次数，绕过尝试上限），也会让同一个验证码被两个线程
     * 同时判定为正确（一次性语义失效）。
     *
     * <p>本实例由 {@code ExpiringStore}（ConcurrentHashMap）持有，会被多个请求线程取到同一份，
     * 所以方法必须是 synchronized。
     */
    public synchronized Attempt attempt(String candidate, int maxAttempts) {
        if (consumed) {
            return Attempt.REPLAYED;
        }
        attempts += 1;
        if (attempts > maxAttempts) {
            return Attempt.EXHAUSTED;
        }
        if (matches(candidate)) {
            consumed = true;
            return Attempt.MATCHED;
        }
        return Attempt.MISMATCH;
    }

    public synchronized int attempts() {
        return attempts;
    }

    private boolean matches(String candidate) {
        return candidate != null && codeHash.equals(Digest.sha256Hex(normalize(candidate)));
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
