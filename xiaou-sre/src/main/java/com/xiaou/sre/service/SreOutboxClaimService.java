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

    SreOutboxEvent claim(Long id);

    SreQueueRecoveryResult recoverStaleProcessing();

    void markSucceeded(Long id);

    void markRetry(Long id, long delaySeconds);

    void markFailed(Long id);
}
