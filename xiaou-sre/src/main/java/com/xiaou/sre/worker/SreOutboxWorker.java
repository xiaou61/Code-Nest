package com.xiaou.sre.worker;

import com.xiaou.sre.config.SreOutboxProperties;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.metrics.SreOutboxMetricsRecorder;
import com.xiaou.sre.service.SreOutboxClaimService;
import com.xiaou.sre.service.SreOutboxEventProcessor;
import com.xiaou.sre.service.SreQueueRecoveryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SRE Outbox 定时消费器。
 *
 * <p>单机阶段使用数据库状态和租约即可避免重复消费；进程异常退出后，过期的
 * PROCESSING 事件会回到 PENDING。该 Worker 只做只读证据任务，不执行系统动作。</p>
 *
 * @author xiaou
 */
@Slf4j
@Component
public class SreOutboxWorker {

    private final SreOutboxProperties properties;
    private final SreOutboxClaimService claimService;
    private final SreOutboxEventProcessor eventProcessor;
    private final SreMetricsRecorder metricsRecorder;
    private final SreOutboxMetricsRecorder outboxMetrics;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    public SreOutboxWorker(SreOutboxProperties properties,
                           SreOutboxClaimService claimService,
                           SreOutboxEventProcessor eventProcessor,
                           SreMetricsRecorder metricsRecorder,
                           SreOutboxMetricsRecorder outboxMetrics) {
        this.properties = properties;
        this.claimService = claimService;
        this.eventProcessor = eventProcessor;
        this.metricsRecorder = metricsRecorder;
        this.outboxMetrics = outboxMetrics == null ? SreOutboxMetricsRecorder.noop() : outboxMetrics;
    }

    /** Compatibility constructor for existing focused tests and embedded callers. */
    public SreOutboxWorker(SreOutboxProperties properties,
                           SreOutboxClaimService claimService,
                           SreOutboxEventProcessor eventProcessor,
                           SreMetricsRecorder metricsRecorder) {
        this(properties, claimService, eventProcessor, metricsRecorder, SreOutboxMetricsRecorder.noop());
    }

    @Scheduled(
            fixedDelayString = "${xiaou.sre.outbox.fixed-delay-ms:5000}",
            initialDelayString = "${xiaou.sre.outbox.initial-delay-ms:10000}"
    )
    public void drain() {
        if (!properties.isEnabled() || !running.compareAndSet(false, true)) {
            return;
        }

        long started = System.nanoTime();
        String outcome = "success";
        try {
            recordRecovery(claimService.recoverStaleProcessing());
            outboxMetrics.recordPending(claimService.countPending());
            List<Long> pendingIds = claimService.listPendingIds(properties.normalizedBatchSize());
            if (pendingIds == null || pendingIds.isEmpty()) {
                return;
            }
            for (Long id : pendingIds) {
                processOne(id);
            }
        } catch (RuntimeException exception) {
            outcome = "error";
            log.error("SRE Outbox 扫描失败: {}", exception.getClass().getSimpleName(), exception);
        } finally {
            outboxMetrics.recordWorkerRun(outcome, System.nanoTime() - started);
            running.set(false);
        }
    }

    private void processOne(Long id) {
        SreOutboxEvent event;
        try {
            event = claimService.claim(id);
        } catch (RuntimeException exception) {
            log.warn("SRE Outbox 领取失败: eventId={}, reason={}", id, exception.getClass().getSimpleName());
            return;
        }
        if (event == null) {
            return;
        }
        incrementQueueEvent("claimed", 1);
        outboxMetrics.beginProcessing();
        long started = System.nanoTime();
        String outcome = "success";

        try {
            eventProcessor.process(event);
            log.info("SRE Outbox 处理成功: eventId={}, eventType={}", event.getId(), event.getEventType());
        } catch (RuntimeException exception) {
            outcome = handleFailure(event, exception);
        } finally {
            outboxMetrics.endProcessing();
            outboxMetrics.recordEvent(event.getEventType(), outcome, System.nanoTime() - started);
        }
    }

    private String handleFailure(SreOutboxEvent event, RuntimeException exception) {
        int attempts = event.getAttempts() == null ? 1 : Math.max(event.getAttempts(), 1);
        if (attempts >= properties.normalizedMaxAttempts()) {
            try {
                claimService.markFailed(event.getId());
                incrementQueueEvent("terminal_failure", 1);
            } catch (RuntimeException stateException) {
                log.error("SRE Outbox 标记失败状态异常: eventId={}, reason={}",
                        event.getId(), stateException.getClass().getSimpleName(), stateException);
            }
            log.error("SRE Outbox 超过最大尝试次数: eventId={}, eventType={}, attempts={}, reason={}",
                    event.getId(), event.getEventType(), attempts, exception.getClass().getSimpleName());
            return "failed";
        }

        long delaySeconds = retryDelaySeconds(attempts);
        try {
            claimService.markRetry(event.getId(), delaySeconds);
            incrementQueueEvent("retry", 1);
        } catch (RuntimeException stateException) {
            log.error("SRE Outbox 标记重试异常: eventId={}, reason={}",
                    event.getId(), stateException.getClass().getSimpleName(), stateException);
            return "retry_state_error";
        }
        log.warn("SRE Outbox 处理失败，将重试: eventId={}, eventType={}, attempts={}, delaySeconds={}, reason={}",
                event.getId(), event.getEventType(), attempts, delaySeconds, exception.getClass().getSimpleName());
        return "retry";
    }

    private long retryDelaySeconds(int attempts) {
        long delay = properties.normalizedRetryBackoffSeconds();
        long maxDelay = properties.normalizedMaxRetryBackoffSeconds();
        for (int i = 1; i < attempts && delay < maxDelay; i++) {
            delay = Math.min(maxDelay, delay * 2L);
        }
        return delay;
    }

    private void recordRecovery(SreQueueRecoveryResult result) {
        if (result == null) {
            return;
        }
        incrementQueueEvent("lease_recovered", result.recovered());
        incrementQueueEvent("terminal_failure", result.terminalFailures());
    }

    private void incrementQueueEvent(String event, long amount) {
        try {
            metricsRecorder.incrementQueueEvent("outbox", event, amount);
        } catch (RuntimeException exception) {
            log.warn("SRE Outbox 指标记录失败: reason={}", exception.getClass().getSimpleName());
        }
    }
}
