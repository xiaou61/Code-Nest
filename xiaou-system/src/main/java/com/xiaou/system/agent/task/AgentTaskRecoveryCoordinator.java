package com.xiaou.system.agent.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.UnexpectedRollbackException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Runs a bounded stale-task scan while keeping every recovery transaction independent.
 */
@Slf4j
@Component
public class AgentTaskRecoveryCoordinator {

    private final AgentTaskProperties properties;
    private final AgentTaskStateService stateService;

    public AgentTaskRecoveryCoordinator(
            AgentTaskProperties properties,
            AgentTaskStateService stateService
    ) {
        this.properties = properties;
        this.stateService = stateService;
    }

    public AgentTaskRecoveryResult recoverStaleTasks() {
        LocalDateTime staleBefore = LocalDateTime.now()
                .minusSeconds(properties.normalizedLeaseSeconds());
        List<String> taskIds = stateService.listStaleRunningIds(
                staleBefore,
                properties.normalizedBatchSize()
        );
        if (taskIds.isEmpty()) {
            return AgentTaskRecoveryResult.empty();
        }

        int recovered = 0;
        int review = 0;
        int conflicts = 0;
        int errors = 0;
        for (String taskId : taskIds) {
            try {
                AgentTaskStateService.RecoveryOutcome outcome = stateService.recoverOne(taskId, staleBefore);
                if (outcome == AgentTaskStateService.RecoveryOutcome.RECOVERED) {
                    recovered++;
                } else if (outcome == AgentTaskStateService.RecoveryOutcome.REQUIRES_REVIEW) {
                    review++;
                } else {
                    conflicts++;
                }
            } catch (UnexpectedRollbackException exception) {
                conflicts++;
                log.debug("管理员智能体单任务恢复检测到并发冲突: taskId={}", taskId);
            } catch (RuntimeException exception) {
                errors++;
                log.warn("管理员智能体单任务恢复失败: taskId={}, reason={}",
                        taskId, exception.getClass().getSimpleName());
            }
        }
        return new AgentTaskRecoveryResult(recovered, review, conflicts, errors);
    }
}
