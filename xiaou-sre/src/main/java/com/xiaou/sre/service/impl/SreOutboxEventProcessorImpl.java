package com.xiaou.sre.service.impl;

import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.mapper.SreOutboxEventMapper;
import com.xiaou.sre.service.SreEvidenceCollectionService;
import com.xiaou.sre.service.SreOutboxEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 单个 Outbox 事件处理器。
 *
 * <p>证据写入与状态置为成功在同一事务中完成，进程在中途退出时会由下一轮重试。</p>
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SreOutboxEventProcessorImpl implements SreOutboxEventProcessor {

    private final SreEvidenceCollectionService evidenceCollectionService;
    private final SreOutboxEventMapper outboxEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(SreOutboxEvent event) {
        if (event == null || event.getId() == null) {
            throw new IllegalArgumentException("Outbox 事件不能为空");
        }
        evidenceCollectionService.collect(event);
        if (outboxEventMapper.markSucceeded(event.getId()) <= 0) {
            throw new IllegalStateException("Outbox 事件状态更新失败");
        }
    }
}
