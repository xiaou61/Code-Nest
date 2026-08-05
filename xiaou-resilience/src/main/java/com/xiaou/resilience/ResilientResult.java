package com.xiaou.resilience;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public record ResilientResult<T>(T value, ResilientStatus status, long durationNanos) {

    public ResilientResult {
        Objects.requireNonNull(status, "status must not be null");
        durationNanos = Math.max(durationNanos, 0L);
    }

    /** The operation ran without throwing, including a legitimate null result. */
    public boolean succeeded() {
        return status == ResilientStatus.SUCCESS || status == ResilientStatus.EMPTY;
    }

    public boolean hasValue() {
        return status == ResilientStatus.SUCCESS;
    }

    public T valueOr(T fallback) {
        return hasValue() ? value : fallback;
    }

    public long durationMillis() {
        return TimeUnit.NANOSECONDS.toMillis(durationNanos);
    }
}
