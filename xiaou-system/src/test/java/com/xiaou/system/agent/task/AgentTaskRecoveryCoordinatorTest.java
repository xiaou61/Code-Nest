package com.xiaou.system.agent.task;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.UnexpectedRollbackException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentTaskRecoveryCoordinatorTest {

    @Test
    void shouldIsolateConflictAndFailureWhileContinuingTheScan() {
        AgentTaskProperties properties = new AgentTaskProperties();
        properties.setBatchSize(4);
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        when(stateService.listStaleRunningIds(any(LocalDateTime.class), eq(4)))
                .thenReturn(List.of("conflict", "failed", "recovered", "review"));
        when(stateService.recoverOne(eq("conflict"), any(LocalDateTime.class)))
                .thenThrow(new UnexpectedRollbackException("lease renewed"));
        when(stateService.recoverOne(eq("failed"), any(LocalDateTime.class)))
                .thenThrow(new IllegalStateException("database unavailable"));
        when(stateService.recoverOne(eq("recovered"), any(LocalDateTime.class)))
                .thenReturn(AgentTaskStateService.RecoveryOutcome.RECOVERED);
        when(stateService.recoverOne(eq("review"), any(LocalDateTime.class)))
                .thenReturn(AgentTaskStateService.RecoveryOutcome.REQUIRES_REVIEW);

        AgentTaskRecoveryResult result = new AgentTaskRecoveryCoordinator(properties, stateService)
                .recoverStaleTasks();

        assertEquals(new AgentTaskRecoveryResult(1, 1, 1, 1), result);
        verify(stateService).recoverOne(eq("recovered"), any(LocalDateTime.class));
        verify(stateService).recoverOne(eq("review"), any(LocalDateTime.class));
    }

    @Test
    void shouldRunEachTaskRecoveryInANewTransaction() throws Exception {
        Transactional transactional = AgentTaskStateService.class
                .getMethod("recoverOne", String.class, LocalDateTime.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }
}
