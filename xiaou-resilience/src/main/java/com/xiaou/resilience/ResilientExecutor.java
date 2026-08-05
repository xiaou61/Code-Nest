package com.xiaou.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Executes independent aggregate-query sources with consistent timing and failure semantics.
 */
@Component
public class ResilientExecutor {

    private static final Logger log = LoggerFactory.getLogger(ResilientExecutor.class);

    public <T> ResilientResult<T> execute(String operation, Supplier<T> supplier) {
        String name = requireOperation(operation);
        Objects.requireNonNull(supplier, "supplier must not be null");
        long started = System.nanoTime();
        try {
            return completed(supplier.get(), started);
        } catch (RuntimeException error) {
            return failed(name, error, started);
        }
    }

    public <T> CompletableFuture<ResilientResult<T>> executeAsync(
            String operation,
            Supplier<T> supplier,
            Duration timeout,
            Executor executor
    ) {
        String name = requireOperation(operation);
        Objects.requireNonNull(supplier, "supplier must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        requirePositive(timeout);
        long started = System.nanoTime();
        try {
            return CompletableFuture.supplyAsync(supplier, executor)
                    .orTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS)
                    .handle((value, error) -> {
                        if (error == null) {
                            return completed(value, started);
                        }
                        Throwable cause = unwrap(error);
                        if (cause instanceof Error fatal) {
                            throw fatal;
                        }
                        if (cause instanceof TimeoutException) {
                            return timedOut(name, started);
                        }
                        return failed(name, cause, started);
                    });
        } catch (RuntimeException error) {
            return CompletableFuture.completedFuture(failed(name, error, started));
        }
    }

    private <T> ResilientResult<T> completed(T value, long started) {
        return new ResilientResult<>(
                value,
                value == null ? ResilientStatus.EMPTY : ResilientStatus.SUCCESS,
                System.nanoTime() - started
        );
    }

    private <T> ResilientResult<T> failed(String operation, Throwable error, long started) {
        log.warn("Resilient operation failed: operation={}, error={}",
                operation, error.getClass().getSimpleName());
        return new ResilientResult<>(null, ResilientStatus.FAILED, System.nanoTime() - started);
    }

    private <T> ResilientResult<T> timedOut(String operation, long started) {
        log.warn("Resilient operation timed out: operation={}", operation);
        return new ResilientResult<>(null, ResilientStatus.TIMEOUT, System.nanoTime() - started);
    }

    private Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private String requireOperation(String operation) {
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("operation must not be blank");
        }
        return operation;
    }

    private void requirePositive(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}
