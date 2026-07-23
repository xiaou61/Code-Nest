package com.xiaou.sre.service.impl;

import com.xiaou.sre.config.SreOutboxProperties;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.mapper.SreOutboxEventMapper;
import com.xiaou.sre.service.SreOutboxClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 领取和状态迁移实现。
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreOutboxClaimServiceImpl implements SreOutboxClaimService {

    private final SreOutboxEventMapper outboxEventMapper;
    private final SreOutboxProperties properties;

    @Override
    @Transactional(readOnly = true)
    public List<Long> listPendingIds(int limit) {
        List<Long> ids = outboxEventMapper.selectPendingIds(Math.max(1, Math.min(limit, 100)));
        return ids == null ? List.of() : ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SreOutboxEvent claim(Long id) {
        if (id == null || id <= 0 || outboxEventMapper.claim(id) <= 0) {
            return null;
        }
        SreOutboxEvent event = outboxEventMapper.selectById(id);
        if (event == null) {
            throw new IllegalStateException("Outbox 事件领取后无法读取");
        }
        return event;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverStaleProcessing() {
        LocalDateTime staleBefore = LocalDateTime.now().minusSeconds(properties.normalizedLeaseSeconds());
        int maxAttempts = properties.normalizedMaxAttempts();
        outboxEventMapper.failStaleProcessing(staleBefore, maxAttempts);
        outboxEventMapper.recoverStaleProcessing(staleBefore, maxAttempts);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSucceeded(Long id) {
        if (id != null) {
            outboxEventMapper.markSucceeded(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRetry(Long id, long delaySeconds) {
        if (id == null) {
            return;
        }
        long delay = Math.max(1L, Math.min(delaySeconds, 86_400L));
        outboxEventMapper.markRetry(id, LocalDateTime.now().plusSeconds(delay));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long id) {
        if (id != null) {
            outboxEventMapper.markFailed(id);
        }
    }
}
