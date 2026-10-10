package io.github.xiaou61.account.internal.support;

import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内限流。三种口径，分别对应三类真实场景：
 *
 * <ul>
 *   <li>{@link #requireInterval}：同一动作的最小间隔（"60 秒内只能发一次验证码"）。</li>
 *   <li>{@link #requireUnderLimit}：**每次调用都计数**（"每小时最多给这个邮箱发 5 次"）——
 *       调用本身就是被限制的行为。</li>
 *   <li>{@link #checkUnderLimit} + {@link #recordFailure}：**只查、失败才计数**
 *       （登录失败次数）。用第一种口径写登录会有一个荒谬的后果：用户正常登录 6 次就被封。</li>
 * </ul>
 *
 * <p>超限时抛 {@code BizException(TOO_MANY_REQUESTS)}，消息里带上还要等多少秒，
 * 前端可以直接倒计时而不是干等。
 *
 * <p><b>天花板</b>：进程内、单实例有效、重启即清零。多实例部署时必须换共享存储，
 * 否则每个实例各算一份，实际阈值变成 limit × 实例数。
 */
public final class RateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maxKeys;
    private final Clock clock;

    public RateLimiter(int maxKeys, Clock clock) {
        this.maxKeys = maxKeys;
        this.clock = clock;
    }

    /** 同一个 key 在给定间隔内只能通过一次；调用即记录。 */
    public void requireInterval(String key, Duration interval) {
        long now = clock.millis();
        Bucket bucket = bucketFor("interval:" + key);
        Long last = bucket.lastPassMillis;
        if (last != null) {
            long elapsed = now - last;
            if (elapsed < interval.toMillis()) {
                throw tooMany((interval.toMillis() - elapsed) / 1000);
            }
        }
        bucket.lastPassMillis = now;
        bucket.expiresAtMillis = now + interval.toMillis();
    }

    /** 窗口内的调用次数不得超过 limit；**调用即计数**。 */
    public void requireUnderLimit(String key, int limit, Duration window) {
        long now = clock.millis();
        Bucket bucket = bucketFor("limit:" + key);
        ensureWindow(bucket, now, window);
        bucket.count += 1;
        if (bucket.count > limit) {
            throw tooMany((window.toMillis() - (now - bucket.windowStartMillis)) / 1000);
        }
    }

    /** 只检查、不计数。用于"失败才计数"的场景，在动作开始时调用。 */
    public void checkUnderLimit(String key, int limit, Duration window) {
        long now = clock.millis();
        Bucket bucket = bucketFor("limit:" + key);
        ensureWindow(bucket, now, window);
        if (bucket.count >= limit) {
            throw tooMany((window.toMillis() - (now - bucket.windowStartMillis)) / 1000);
        }
    }

    /** 只计数、不检查。与 {@link #checkUnderLimit} 配对，在动作失败时调用。 */
    public void recordFailure(String key, Duration window) {
        long now = clock.millis();
        Bucket bucket = bucketFor("limit:" + key);
        ensureWindow(bucket, now, window);
        bucket.count += 1;
    }

    /** 成功之后清掉计数（登录成功后不应再受此前失败次数影响）。 */
    public void clear(String key) {
        buckets.remove("interval:" + key);
        buckets.remove("limit:" + key);
    }

    public int purgeExpired() {
        long now = clock.millis();
        int removed = 0;
        for (var iterator = buckets.entrySet().iterator(); iterator.hasNext(); ) {
            if (iterator.next().getValue().expiresAtMillis <= now) {
                iterator.remove();
                removed += 1;
            }
        }
        return removed;
    }

    private static void ensureWindow(Bucket bucket, long now, Duration window) {
        if (bucket.windowStartMillis == null || now - bucket.windowStartMillis >= window.toMillis()) {
            bucket.windowStartMillis = now;
            bucket.count = 0;
            bucket.expiresAtMillis = now + window.toMillis();
        }
    }

    private Bucket bucketFor(String key) {
        Bucket existing = buckets.get(key);
        if (existing != null) {
            return existing;
        }
        if (buckets.size() >= maxKeys) {
            purgeExpired();
            if (buckets.size() >= maxKeys) {
                // 键太多说明正在被刷；此时拒绝比继续吃内存更安全
                throw tooMany(60);
            }
        }
        return buckets.computeIfAbsent(key, ignored -> new Bucket());
    }

    private static BizException tooMany(long secondsToWait) {
        long seconds = Math.max(1, secondsToWait);
        return new BizException(ErrorCode.TOO_MANY_REQUESTS, "操作过于频繁，请在 " + seconds + " 秒后重试");
    }

    /** 一个键的几种口径共用一条记录，各自只用自己的字段。 */
    private static final class Bucket {
        private Long lastPassMillis;
        private Long windowStartMillis;
        private int count;
        private long expiresAtMillis;
    }
}
