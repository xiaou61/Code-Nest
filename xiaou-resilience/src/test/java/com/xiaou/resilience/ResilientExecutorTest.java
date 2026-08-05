package com.xiaou.resilience;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class ResilientExecutorTest {

    private final ResilientExecutor executor = new ResilientExecutor();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        worker.shutdownNow();
    }

    @Test
    void executeShouldDistinguishSuccessEmptyAndFailure() {
        ResilientResult<String> success = executor.execute("source.success", () -> "value");
        ResilientResult<String> empty = executor.execute("source.empty", () -> null);
        ResilientResult<String> failure = executor.execute("source.failure", () -> {
            throw new IllegalStateException("unavailable");
        });

        assertThat(success.status()).isEqualTo(ResilientStatus.SUCCESS);
        assertThat(success.valueOr("fallback")).isEqualTo("value");
        assertThat(empty.status()).isEqualTo(ResilientStatus.EMPTY);
        assertThat(empty.succeeded()).isTrue();
        assertThat(empty.valueOr("fallback")).isEqualTo("fallback");
        assertThat(failure.status()).isEqualTo(ResilientStatus.FAILED);
        assertThat(failure.succeeded()).isFalse();
    }

    @Test
    void executeAsyncShouldReturnTimeoutWithoutFailingTheAggregateFuture() {
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<ResilientResult<String>> future = executor.executeAsync(
                "source.timeout",
                () -> {
                    try {
                        release.await();
                        return "late";
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(error);
                    }
                },
                Duration.ofMillis(30),
                worker
        );

        try {
            assertThat(future.join().status()).isEqualTo(ResilientStatus.TIMEOUT);
        } finally {
            release.countDown();
        }
    }
}
