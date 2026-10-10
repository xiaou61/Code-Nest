package io.github.xiaou61.account.internal.credential;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
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

/**
 * 验证码在并发下的行为。
 *
 * <p>计数与消费这两件事正是它作为"一次性凭据"的全部价值：计数丢更新会抬高爆破上限，
 * 消费不原子会让同一枚码被用两次。两条都必须在并发下成立。
 */
class VerificationCodeTest {

    private static final String CODE = "123456";

    @Test
    @DisplayName("并发提交正确的码，只允许一次判定为正确（不可重放）")
    void concurrentCorrectAttemptsMatchExactlyOnce() throws Exception {
        VerificationCode verification = new VerificationCode("h", CODE, Instant.now().plus(Duration.ofMinutes(10)));
        AtomicInteger matched = new AtomicInteger();

        runConcurrently(16, () -> {
            if (verification.attempt(CODE, 100) == VerificationCode.Attempt.MATCHED) {
                matched.incrementAndGet();
            }
        });

        assertThat(matched.get()).as("同一个验证码只能被消费一次").isEqualTo(1);
    }

    @Test
    @DisplayName("并发错误提交也逐次计数：尝试上限不会被丢失更新绕过")
    void concurrentWrongAttemptsAreAllCounted() throws Exception {
        VerificationCode verification = new VerificationCode("h", CODE, Instant.now().plus(Duration.ofMinutes(10)));

        runConcurrently(16, () -> verification.attempt("000000", 5));

        assertThat(verification.attempts()).as("每次尝试都要记上").isEqualTo(16);
    }

    private static void runConcurrently(int threads, Runnable action) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i += 1) {
                futures.add(pool.submit(() -> {
                    start.await();
                    action.run();
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
    }
}
