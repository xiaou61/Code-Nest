package com.xiaou.sre.service;

/**
 * 队列执行失败后的受控状态迁移结果。
 *
 * @author xiaou
 */
public enum SreQueueRetryOutcome {
    RETRY_SCHEDULED,
    DEADLINE_EXCEEDED,
    TERMINAL_FAILURE,
    IGNORED
}
