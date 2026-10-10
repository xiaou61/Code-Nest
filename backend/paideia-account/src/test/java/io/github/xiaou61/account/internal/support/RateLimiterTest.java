package io.github.xiaou61.account.internal.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    /** 可控时钟：窗口边界是时间行为，等真实时间是浪费。 */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    @Test
    @DisplayName("最小间隔内第二次被拒，超时后放行")
    void enforcesMinimumInterval() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(100, clock);

        limiter.requireInterval("email", Duration.ofSeconds(60));
        assertThatThrownBy(() -> limiter.requireInterval("email", Duration.ofSeconds(60)))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).errorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);

        clock.advance(Duration.ofSeconds(61));
        assertThatCode(() -> limiter.requireInterval("email", Duration.ofSeconds(60))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("窗口内超过次数上限即被拒，窗口过去后重新计数")
    void enforcesWindowLimit() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(100, clock);

        limiter.requireUnderLimit("k", 2, Duration.ofHours(1));
        limiter.requireUnderLimit("k", 2, Duration.ofHours(1));
        assertThatThrownBy(() -> limiter.requireUnderLimit("k", 2, Duration.ofHours(1)))
                .isInstanceOf(BizException.class);

        clock.advance(Duration.ofHours(1).plusSeconds(1));
        assertThatCode(() -> limiter.requireUnderLimit("k", 2, Duration.ofHours(1))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkUnderLimit 只查不计数：连查十次不该把自己查超限")
    void checkOnlyDoesNotCount() {
        RateLimiter limiter = new RateLimiter(100, Clock.systemUTC());

        for (int attempt = 0; attempt < 10; attempt += 1) {
            assertThatCode(() -> limiter.checkUnderLimit("k", 3, Duration.ofMinutes(15)))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("登录这类场景：失败才计数，第 N 次失败后开始拒绝")
    void recordFailureCountsOnlyFailures() {
        RateLimiter limiter = new RateLimiter(100, Clock.systemUTC());
        Duration window = Duration.ofMinutes(15);

        for (int attempt = 1; attempt <= 3; attempt += 1) {
            assertThatCode(() -> limiter.checkUnderLimit("login", 3, window)).doesNotThrowAnyException();
            limiter.recordFailure("login", window);
        }

        // 前三次失败之后，第四次一进门就被挡
        assertThatThrownBy(() -> limiter.checkUnderLimit("login", 3, window)).isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("成功之后 clear 会清掉失败计数")
    void clearResetsFailures() {
        RateLimiter limiter = new RateLimiter(100, Clock.systemUTC());
        Duration window = Duration.ofMinutes(15);

        limiter.recordFailure("login", window);
        limiter.recordFailure("login", window);
        limiter.clear("login");

        // 清零后应能重新获得完整的失败额度
        for (int attempt = 1; attempt <= 3; attempt += 1) {
            assertThatCode(() -> limiter.checkUnderLimit("login", 3, window)).doesNotThrowAnyException();
            limiter.recordFailure("login", window);
        }
        assertThatThrownBy(() -> limiter.checkUnderLimit("login", 3, window)).isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("并发下最小间隔只放行一次：读—判断—写必须是原子的")
    void concurrentIntervalAllowsExactlyOne() throws Exception {
        RateLimiter limiter = new RateLimiter(1_000, Clock.systemUTC());

        AtomicInteger passed = runConcurrently(16, () -> limiter.requireInterval("email", Duration.ofSeconds(60)));

        assertThat(passed.get()).as("同一 key 的间隔内只允许一次通过").isEqualTo(1);
    }

    @Test
    @DisplayName("并发下窗口计数不丢更新：通过次数正好等于上限")
    void concurrentWindowLimitNeverExceedsLimit() throws Exception {
        RateLimiter limiter = new RateLimiter(1_000, Clock.systemUTC());

        AtomicInteger passed = runConcurrently(32, () -> limiter.requireUnderLimit("k", 5, Duration.ofHours(1)));

        assertThat(passed.get()).as("并发也不能让通过次数超过 limit").isEqualTo(5);
    }

    /** 让 n 个线程同时起跑各执行一次 action，返回未被限流（抛 BizException）的次数。 */
    private static AtomicInteger runConcurrently(int threads, Runnable action) throws Exception {
        AtomicInteger passed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i += 1) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        action.run();
                        passed.incrementAndGet();
                    } catch (BizException expected) {
                        // 被限流是预期结果
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        return passed;
    }
}
