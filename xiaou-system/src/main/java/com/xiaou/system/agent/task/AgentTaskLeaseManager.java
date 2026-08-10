package com.xiaou.system.agent.task;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Creates fenced claim tokens and keeps a lease alive around one bounded operation.
 */
@Slf4j
@Component
public class AgentTaskLeaseManager implements AutoCloseable {

    private static final int MAX_LEASE_OWNER_CHARS = 120;
    private static final int HEARTBEAT_THREADS = 2;
    private static final String DEFAULT_OWNER_PREFIX = "agent-task-worker";
    private static final AtomicInteger HEARTBEAT_THREAD_SEQUENCE = new AtomicInteger();

    private final AgentTaskStateService stateService;
    private final AgentTaskMetricsRecorder metricsRecorder;
    private final ScheduledExecutorService scheduler;
    private final long renewalIntervalMillis;

    @Autowired
    public AgentTaskLeaseManager(
            AgentTaskStateService stateService,
            AgentTaskProperties properties,
            AgentTaskMetricsRecorder metricsRecorder
    ) {
        this(
                stateService,
                metricsRecorder,
                newHeartbeatScheduler(),
                Duration.ofSeconds(Math.max(1L, properties.normalizedLeaseSeconds() / 3L))
        );
    }

    AgentTaskLeaseManager(
            AgentTaskStateService stateService,
            AgentTaskMetricsRecorder metricsRecorder,
            ScheduledExecutorService scheduler,
            Duration renewalInterval
    ) {
        this.stateService = stateService;
        this.metricsRecorder = metricsRecorder;
        this.scheduler = scheduler;
        long configuredMillis = renewalInterval == null ? 0L : renewalInterval.toMillis();
        this.renewalIntervalMillis = Math.max(1L, configuredMillis);
    }

    public String newClaimToken(String workerId) {
        String prefix = StringUtils.hasText(workerId) ? workerId.trim() : DEFAULT_OWNER_PREFIX;
        String suffix = UUID.randomUUID().toString();
        int maxPrefixLength = MAX_LEASE_OWNER_CHARS - suffix.length() - 1;
        if (prefix.length() > maxPrefixLength) {
            prefix = prefix.substring(0, maxPrefixLength);
        }
        return prefix + ":" + suffix;
    }

    public <T> T callWithHeartbeat(
            String taskId,
            String leaseOwner,
            Supplier<T> operation
    ) {
        if (operation == null) {
            throw new IllegalArgumentException("租约保护操作不能为空");
        }
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(leaseOwner)) {
            throw new IllegalArgumentException("租约保护需要完整的任务标识和租约所有者");
        }

        AtomicBoolean active = new AtomicBoolean(true);
        ScheduledFuture<?> heartbeat = scheduler.scheduleWithFixedDelay(
                () -> renew(taskId, leaseOwner, active),
                renewalIntervalMillis,
                renewalIntervalMillis,
                TimeUnit.MILLISECONDS
        );
        try {
            return operation.get();
        } finally {
            active.set(false);
            heartbeat.cancel(false);
        }
    }

    private void renew(String taskId, String leaseOwner, AtomicBoolean active) {
        if (!active.get()) {
            return;
        }
        try {
            if (stateService.renewLease(taskId, leaseOwner)) {
                recordRenewal("success");
                return;
            }
            active.set(false);
            recordRenewal("lost");
        } catch (RuntimeException exception) {
            recordRenewal("error");
            log.warn("管理员智能体任务租约续期失败: reason={}",
                    exception.getClass().getSimpleName());
        }
    }

    private void recordRenewal(String outcome) {
        try {
            metricsRecorder.recordLeaseRenewal(outcome);
        } catch (RuntimeException exception) {
            log.warn("管理员智能体任务租约指标记录失败: reason={}",
                    exception.getClass().getSimpleName());
        }
    }

    private static ScheduledExecutorService newHeartbeatScheduler() {
        return Executors.newScheduledThreadPool(HEARTBEAT_THREADS, runnable -> {
            Thread thread = new Thread(runnable,
                    "agent-task-lease-heartbeat-" + HEARTBEAT_THREAD_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    @PreDestroy
    public void close() {
        scheduler.shutdownNow();
    }
}
