package com.xiaou.system.worker;

import com.xiaou.sre.config.SreRcaEvaluationProperties;
import com.xiaou.sre.domain.SreRcaEvaluationRun;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.service.SreQueueRecoveryResult;
import com.xiaou.sre.service.SreQueueRetryOutcome;
import com.xiaou.sre.service.SreRcaEvaluationRunService;
import com.xiaou.system.service.SreRcaEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Database-backed RCA evaluation worker.
 *
 * <p>Claiming is conditional in MySQL, so multiple application instances can scan the same
 * queue without executing one run concurrently. The in-process guard prevents scheduler
 * overlap on a single instance.</p>
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SreRcaEvaluationWorker {

    private static final String EXECUTION_FAILURE = "EVALUATION_EXECUTION_FAILED";

    private final SreRcaEvaluationProperties properties;
    private final SreRcaEvaluationRunService runService;
    private final SreRcaEvaluationService evaluationService;
    private final SreMetricsRecorder metricsRecorder;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(
            fixedDelayString = "${xiaou.sre.evaluation.fixed-delay-ms:2000}",
            initialDelayString = "${xiaou.sre.evaluation.initial-delay-ms:10000}"
    )
    public void drain() {
        if (!properties.isEnabled() || !running.compareAndSet(false, true)) {
            return;
        }
        try {
            recordRecovery(runService.recoverStaleRuns());
            List<Long> runIds = runService.listClaimableIds(properties.normalizedBatchSize());
            for (Long runId : runIds) {
                processOne(runId);
            }
        } catch (RuntimeException exception) {
            log.error("SRE RCA 评测队列扫描失败: reason={}",
                    exception.getClass().getSimpleName(), exception);
        } finally {
            running.set(false);
        }
    }

    private void processOne(Long runId) {
        SreRcaEvaluationRun run;
        try {
            run = runService.claim(runId);
        } catch (RuntimeException exception) {
            log.warn("SRE RCA 评测运行领取失败: runId={}, reason={}",
                    runId, exception.getClass().getSimpleName());
            return;
        }
        if (run == null) {
            return;
        }
        incrementQueueEvent("claimed", 1);
        long startNanos = System.nanoTime();
        try {
            evaluationService.executeClaimedRun(run.getId());
            recordEvaluation("succeeded", System.nanoTime() - startNanos);
            log.info("SRE RCA 评测运行执行结束: runId={}, attempt={}",
                    run.getId(), run.getAttempts());
        } catch (RuntimeException exception) {
            recordEvaluation("failed", System.nanoTime() - startNanos);
            log.warn("SRE RCA 评测运行执行异常: runId={}, attempt={}, reason={}",
                    run.getId(), run.getAttempts(), exception.getClass().getSimpleName());
            try {
                recordRetryOutcome(runService.retryOrFail(run.getId(), EXECUTION_FAILURE));
            } catch (RuntimeException stateException) {
                log.error("SRE RCA 评测运行重试状态写入失败: runId={}, reason={}",
                        run.getId(), stateException.getClass().getSimpleName(), stateException);
            }
        }
    }

    private void recordRecovery(SreQueueRecoveryResult result) {
        if (result == null) {
            return;
        }
        incrementQueueEvent("lease_recovered", result.recovered());
        incrementQueueEvent("terminal_failure", result.terminalFailures());
        incrementQueueEvent("deadline_exceeded", result.deadlineExceeded());
    }

    private void recordRetryOutcome(SreQueueRetryOutcome outcome) {
        if (outcome == null) {
            return;
        }
        switch (outcome) {
            case RETRY_SCHEDULED -> incrementQueueEvent("retry", 1);
            case DEADLINE_EXCEEDED -> incrementQueueEvent("deadline_exceeded", 1);
            case TERMINAL_FAILURE -> incrementQueueEvent("terminal_failure", 1);
            case IGNORED -> {
                // 状态已被其他节点推进，无需重复计数。
            }
        }
    }

    private void recordEvaluation(String outcome, long durationNanos) {
        try {
            metricsRecorder.recordEvaluation(outcome, durationNanos);
        } catch (RuntimeException exception) {
            log.warn("SRE RCA 评测指标记录失败: reason={}", exception.getClass().getSimpleName());
        }
    }

    private void incrementQueueEvent(String event, long amount) {
        try {
            metricsRecorder.incrementQueueEvent("evaluation", event, amount);
        } catch (RuntimeException exception) {
            log.warn("SRE RCA 评测队列指标记录失败: reason={}", exception.getClass().getSimpleName());
        }
    }
}
