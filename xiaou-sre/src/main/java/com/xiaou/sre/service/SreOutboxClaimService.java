package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreOutboxEvent;

import java.util.List;

/**
 * Outbox 领取和状态迁移端口。
 *
 * @author xiaou
 */
public interface SreOutboxClaimService {

    List<Long> listPendingIds(int limit);

    /** Return the current pending queue size for monitoring. */
    long countPending();

    SreOutboxEvent claim(Long id);

    /** Recover leased events and return the number made pending again. */
    int recoverStaleProcessing();

    void markSucceeded(Long id);

    void markRetry(Long id, long delaySeconds);

    void markFailed(Long id);
}
