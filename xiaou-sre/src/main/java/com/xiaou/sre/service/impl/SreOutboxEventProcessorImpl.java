package com.xiaou.sre.service.impl;

import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.mapper.SreOutboxEventMapper;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.service.SreEvidenceCollectionService;
import com.xiaou.sre.service.SreOutboxEventProcessor;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SreOutboxEventProcessorImpl implements SreOutboxEventProcessor {

    private final SreEvidenceCollectionService evidenceCollectionService;
    private final SreOutboxEventMapper outboxEventMapper;
    private final SreMetricsRecorder metrics;

    @Autowired
    public SreOutboxEventProcessorImpl(SreEvidenceCollectionService evidenceCollectionService,
                                       SreOutboxEventMapper outboxEventMapper,
                                       SreMetricsRecorder metrics) {
        this.evidenceCollectionService = evidenceCollectionService;
        this.outboxEventMapper = outboxEventMapper;
        this.metrics = metrics;
    }

    /** Compatibility constructor for embedded callers and focused tests. */
    public SreOutboxEventProcessorImpl(SreEvidenceCollectionService evidenceCollectionService,
                                       SreOutboxEventMapper outboxEventMapper) {
        this(evidenceCollectionService, outboxEventMapper, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(SreOutboxEvent event) {
        if (event == null || event.getId() == null) {
            throw new IllegalArgumentException("Outbox 事件不能为空");
        }
        long started = System.nanoTime();
        String outcome = "success";
        try {
            evidenceCollectionService.collect(event);
            if (outboxEventMapper.markSucceeded(event.getId()) <= 0) {
                outcome = "error";
                throw new IllegalStateException("Outbox 事件状态更新失败");
            }
        } catch (RuntimeException exception) {
            outcome = "error";
            throw exception;
        } finally {
            if (metrics != null) {
                metrics.recordEvidenceCollection("outbox", outcome, System.nanoTime() - started);
            }
        }
    }
}
