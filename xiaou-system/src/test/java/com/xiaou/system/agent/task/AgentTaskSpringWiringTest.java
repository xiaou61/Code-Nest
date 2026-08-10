package com.xiaou.system.agent.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.system.mapper.SysAgentTaskMapper;
import com.xiaou.system.mapper.SysAgentTaskEventMapper;
import com.xiaou.system.mapper.SysAgentTaskStepMapper;
import com.xiaou.system.service.SysAgentAuditService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AgentTaskSpringWiringTest {

    @Test
    void springCanResolveStateServiceAndWorkerProductionConstructors() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SysAgentTaskMapper.class, () -> mock(SysAgentTaskMapper.class));
            context.registerBean(SysAgentTaskEventMapper.class, () -> mock(SysAgentTaskEventMapper.class));
            context.registerBean(SysAgentTaskStepMapper.class, () -> mock(SysAgentTaskStepMapper.class));
            context.registerBean(SysAgentAuditService.class, () -> mock(SysAgentAuditService.class));
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.registerBean(MeterRegistry.class, () -> new SimpleMeterRegistry());
            context.registerBean(AgentTaskProperties.class, AgentTaskProperties::new);
            context.register(AgentTaskMetricsRecorder.class);
            context.register(AgentTaskResponseMapper.class);
            context.register(AgentTaskStateService.class);
            context.register(AgentTaskLeaseManager.class);
            context.register(AgentTaskRecoveryCoordinator.class);
            context.registerBean(AgentTaskGraphRunner.class, () -> mock(AgentTaskGraphRunner.class));
            context.register(AgentTaskWorker.class);

            assertDoesNotThrow(context::refresh);
            assertNotNull(context.getBean(AgentTaskStateService.class));
            assertNotNull(context.getBean(AgentTaskLeaseManager.class));
            assertNotNull(context.getBean(AgentTaskRecoveryCoordinator.class));
            assertNotNull(context.getBean(AgentTaskWorker.class));
        }
    }
}
