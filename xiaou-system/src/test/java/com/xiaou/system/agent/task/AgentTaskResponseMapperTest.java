package com.xiaou.system.agent.task;

import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskStep;
import com.xiaou.system.dto.AgentTaskResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentTaskResponseMapperTest {

    @Test
    void shouldExposeBoundedStepResultAndPendingConfirmationMetadata() {
        AgentTaskProperties properties = new AgentTaskProperties();
        properties.setMaxResultJsonChars(1_000);
        SysAgentTask task = new SysAgentTask();
        task.setTaskId("agent-task-1");
        task.setStatus(AgentTaskStatus.WAITING_CONFIRMATION.name());

        SysAgentTaskStep step = new SysAgentTaskStep();
        step.setStepOrder(1);
        step.setToolName("system.write.setting");
        step.setStatus(AgentTaskStepStatus.WAITING_CONFIRMATION.name());
        step.setAuditId("agent-audit-1");
        step.setConfirmationText("确认修改配置");
        step.setResultJson("x".repeat(2_000));

        AgentTaskResponse response = new AgentTaskResponseMapper(properties)
                .toResponse(task, List.of(step));

        assertEquals("等待确认", response.getStatusText());
        assertEquals(1_000, response.getSteps().get(0).getResultJson().length());
        assertNotNull(response.getConfirmation());
        assertEquals("agent-audit-1", response.getConfirmation().getAuditId());
        assertEquals("确认修改配置", response.getConfirmation().getRequiredText());
    }
}
