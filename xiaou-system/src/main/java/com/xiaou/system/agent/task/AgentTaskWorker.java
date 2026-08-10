package com.xiaou.system.agent.task;

import com.xiaou.system.domain.SysAgentTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Claims and advances durable administrator-agent tasks through bounded graph cycles.
 */
@Slf4j
@Component
public class AgentTaskWorker {

    private final AgentTaskProperties properties;
    private final AgentTaskStateService stateService;
    private final AgentTaskRecoveryCoordinator recoveryCoordinator;
    private final AgentTaskGraphRunner graphRunner;
    private final AgentTaskLeaseManager leaseManager;
    private final AgentTaskMetricsRecorder metricsRecorder;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final String workerId = "agent-task-worker-" + UUID.randomUUID();

    public AgentTaskWorker(
            AgentTaskProperties properties,
            AgentTaskStateService stateService,
            AgentTaskRecoveryCoordinator recoveryCoordinator,
            AgentTaskGraphRunner graphRunner,
            AgentTaskLeaseManager leaseManager,
            AgentTaskMetricsRecorder metricsRecorder
    ) {
        this.properties = properties;
        this.stateService = stateService;
        this.recoveryCoordinator = recoveryCoordinator;
        this.graphRunner = graphRunner;
        this.leaseManager = leaseManager;
        this.metricsRecorder = metricsRecorder;
    }

    @Scheduled(
            fixedDelayString = "${xiaou.admin-agent.task.fixed-delay-ms:2000}",
            initialDelayString = "${xiaou.admin-agent.task.initial-delay-ms:10000}"
    )
    public void drain() {
        if (!properties.isEnabled() || !running.compareAndSet(false, true)) {
            return;
        }
        long startedNanos = System.nanoTime();
        String workerOutcome = "success";
        try {
            recordRecovery(recoveryCoordinator.recoverStaleTasks());
            List<String> taskIds = stateService.listClaimableIds(properties.normalizedBatchSize());
            for (String taskId : taskIds) {
                processOne(taskId);
            }
            recordQueueDepth(stateService.countQueuedTasks());
        } catch (RuntimeException exception) {
            workerOutcome = "error";
            log.error("管理员智能体任务队列扫描失败: reason={}",
                    exception.getClass().getSimpleName(), exception);
        } finally {
            recordWorkerRun(workerOutcome, System.nanoTime() - startedNanos);
            running.set(false);
        }
    }

    private void processOne(String taskId) {
        String leaseOwner = leaseManager.newClaimToken(workerId);
        SysAgentTask task;
        try {
            task = stateService.claim(taskId, leaseOwner);
        } catch (RuntimeException exception) {
            log.warn("管理员智能体任务领取失败: taskId={}, reason={}",
                    taskId, exception.getClass().getSimpleName());
            return;
        }
        if (task == null) {
            return;
        }

        try {
            leaseManager.callWithHeartbeat(taskId, leaseOwner, () -> {
                processClaimedTask(task, leaseOwner);
                return null;
            });
        } catch (RuntimeException exception) {
            // Keep the durable RUNNING state for lease recovery. Recovery decides whether retry is safe.
            log.warn("管理员智能体任务执行中断: taskId={}, reason={}",
                    taskId, exception.getClass().getSimpleName(), exception);
        }
    }

    private void processClaimedTask(SysAgentTask task, String leaseOwner) {
        String taskId = task.getTaskId();
        int maxCycles = boundedMaxSteps(task) + 1;
        for (int cycle = 0; cycle < maxCycles; cycle++) {
            AgentTaskCycleOutcome outcome = graphRunner.runCycle(taskId, leaseOwner);
            if (outcome == null) {
                throw new IllegalStateException("任务图没有返回周期结果");
            }
            recordCycle(outcome.status());
            if (outcome.status() != AgentTaskCycleOutcome.Status.CONTINUE) {
                return;
            }
        }
        stateService.failTask(
                taskId,
                leaseOwner,
                "WORKER_CYCLE_LIMIT",
                "任务 Worker 达到有界图周期上限，已停止继续执行。"
        );
    }

    private int boundedMaxSteps(SysAgentTask task) {
        int persisted = task == null || task.getMaxSteps() == null
                ? properties.normalizedMaxSteps()
                : task.getMaxSteps();
        return Math.max(1, Math.min(persisted, 10));
    }

    private void recordRecovery(AgentTaskRecoveryResult result) {
        try {
            metricsRecorder.recordStaleRecovery(result);
        } catch (RuntimeException exception) {
            log.warn("管理员智能体任务恢复指标记录失败: reason={}",
                    exception.getClass().getSimpleName());
        }
    }

    private void recordQueueDepth(long depth) {
        try {
            metricsRecorder.recordQueueDepth(depth);
        } catch (RuntimeException exception) {
            log.warn("管理员智能体任务队列深度指标记录失败: reason={}",
                    exception.getClass().getSimpleName());
        }
    }

    private void recordCycle(AgentTaskCycleOutcome.Status status) {
        try {
            metricsRecorder.recordCycleOutcome(status);
        } catch (RuntimeException exception) {
            log.warn("管理员智能体任务周期指标记录失败: reason={}",
                    exception.getClass().getSimpleName());
        }
    }

    private void recordWorkerRun(String outcome, long durationNanos) {
        try {
            metricsRecorder.recordWorkerRun(outcome, durationNanos);
        } catch (RuntimeException exception) {
            log.warn("管理员智能体任务 Worker 指标记录失败: reason={}",
                    exception.getClass().getSimpleName());
        }
    }
}
