package com.xiaou.sre.service;

/**
 * 一次持久化队列租约扫描的精确状态迁移计数。
 *
 * @author xiaou
 */
public record SreQueueRecoveryResult(
        long recovered,
        long terminalFailures,
        long deadlineExceeded
) {
    public SreQueueRecoveryResult {
        recovered = Math.max(0L, recovered);
        terminalFailures = Math.max(0L, terminalFailures);
        deadlineExceeded = Math.max(0L, deadlineExceeded);
    }

    public static SreQueueRecoveryResult empty() {
        return new SreQueueRecoveryResult(0, 0, 0);
    }
}
