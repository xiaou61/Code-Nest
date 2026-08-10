package com.xiaou.system.agent.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskEvent;
import com.xiaou.system.domain.SysAgentTaskStep;
import com.xiaou.system.dto.AgentTaskEventPageResponse;
import com.xiaou.system.dto.AgentTaskInputRequest;
import com.xiaou.system.dto.AgentTaskPauseRequest;
import com.xiaou.system.mapper.SysAgentTaskEventMapper;
import com.xiaou.system.mapper.SysAgentTaskMapper;
import com.xiaou.system.mapper.SysAgentTaskStepMapper;
import com.xiaou.system.service.SysAgentAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentTaskControlPlaneTest {

    @Test
    void shouldPauseAndResumeOwnerTaskWithInterventionEvents() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentTaskEventMapper eventMapper = mock(SysAgentTaskEventMapper.class);
        SysAgentTask running = task("RUNNING");
        SysAgentTask paused = task("PAUSED");
        SysAgentTask queued = task("QUEUED");
        when(taskMapper.selectOwned("task-1", 7L)).thenReturn(running, paused);
        when(taskMapper.transition(any())).thenReturn(1);
        when(taskMapper.selectByTaskId("task-1")).thenReturn(paused, queued);
        when(stepMapper.selectByTaskId("task-1")).thenReturn(List.of());
        when(eventMapper.insert(any())).thenReturn(1);

        AgentTaskStateService service = service(taskMapper, stepMapper, eventMapper);
        AgentTaskPauseRequest pauseRequest = new AgentTaskPauseRequest();
        pauseRequest.setReason("operator review");

        assertEquals("PAUSED", service.pause("task-1", 7L, pauseRequest).getStatus());
        assertEquals("QUEUED", service.resume("task-1", 7L).getStatus());

        ArgumentCaptor<SysAgentTaskEvent> events = ArgumentCaptor.forClass(SysAgentTaskEvent.class);
        verify(eventMapper, org.mockito.Mockito.times(2)).insert(events.capture());
        assertEquals(AgentTaskEventType.TASK_PAUSED.name(), events.getAllValues().get(0).getEventType());
        assertEquals(AgentTaskEventType.TASK_RESUMED.name(), events.getAllValues().get(1).getEventType());
        assertEquals("RUNNING", events.getAllValues().get(0).getFromStatus());
        assertEquals("PAUSED", events.getAllValues().get(0).getToStatus());
    }

    @Test
    void shouldRejectEventReadForAnotherOwnerBeforeQueryingEvents() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskEventMapper eventMapper = mock(SysAgentTaskEventMapper.class);
        when(taskMapper.selectOwned("task-1", 8L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service(taskMapper, mock(SysAgentTaskStepMapper.class), eventMapper)
                        .events("task-1", 8L, 0L, 50));

        verify(eventMapper, never()).selectOwnedAfter(any(), any(), any(), anyInt());
    }

    @Test
    void shouldReturnStableBoundedEventPageWithNextCursor() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskEventMapper eventMapper = mock(SysAgentTaskEventMapper.class);
        when(taskMapper.selectOwned("task-1", 7L)).thenReturn(task("RUNNING"));
        when(eventMapper.selectOwnedAfter("task-1", 7L, 10L, 3)).thenReturn(List.of(
                event(11L, AgentTaskEventType.TASK_CLAIMED),
                event(12L, AgentTaskEventType.STEP_STARTED),
                event(13L, AgentTaskEventType.STEP_COMPLETED)
        ));

        AgentTaskEventPageResponse page = service(
                taskMapper, mock(SysAgentTaskStepMapper.class), eventMapper)
                .events("task-1", 7L, 10L, 2);

        assertEquals(List.of(11L, 12L), page.getEvents().stream()
                .map(com.xiaou.system.dto.AgentTaskEventResponse::getCursor)
                .toList());
        assertEquals(12L, page.getNextCursor());
        assertTrue(page.isHasMore());
        verify(eventMapper).selectOwnedAfter("task-1", 7L, 10L, 3);
    }

    @Test
    void shouldMergeBoundedInputAndQueueWaitingTask() throws Exception {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentTaskEventMapper eventMapper = mock(SysAgentTaskEventMapper.class);
        SysAgentTask waiting = task("WAITING_INPUT");
        waiting.setWorkflowContextJson("{\"scope\":\"runtime\"}");
        SysAgentTask queued = task("QUEUED");
        when(taskMapper.selectOwned("task-1", 7L)).thenReturn(waiting);
        when(taskMapper.transition(any())).thenReturn(1);
        when(taskMapper.selectByTaskId("task-1")).thenReturn(queued);
        when(stepMapper.selectByTaskId("task-1")).thenReturn(List.of());
        when(eventMapper.insert(any())).thenReturn(1);

        AgentTaskInputRequest request = new AgentTaskInputRequest();
        request.setInput(new LinkedHashMap<>(Map.of("region", "cn")));
        AgentTaskResponseAssertions.assertStatus(
                service(taskMapper, stepMapper, eventMapper).submitInput("task-1", 7L, request),
                "QUEUED"
        );

        ArgumentCaptor<SysAgentTask> taskCaptor = ArgumentCaptor.forClass(SysAgentTask.class);
        verify(taskMapper).transition(taskCaptor.capture());
        Map<?, ?> stored = new ObjectMapper().readValue(
                taskCaptor.getValue().getWorkflowContextJson(), Map.class);
        assertEquals("runtime", stored.get("scope"));
        assertEquals("cn", stored.get("region"));

        ArgumentCaptor<SysAgentTaskEvent> eventCaptor = ArgumentCaptor.forClass(SysAgentTaskEvent.class);
        verify(eventMapper).insert(eventCaptor.capture());
        assertEquals(AgentTaskEventType.INPUT_SUBMITTED.name(), eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getDetailJson().contains("region"));
        assertTrue(!eventCaptor.getValue().getDetailJson().contains("cn"));
    }

    @Test
    void shouldFailTheTransactionPathWhenRequiredEventCannotBePersisted() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentTaskEventMapper eventMapper = mock(SysAgentTaskEventMapper.class);
        when(taskMapper.selectOwned("task-1", 7L)).thenReturn(task("RUNNING"));
        when(taskMapper.transition(any())).thenReturn(1);
        when(eventMapper.insert(any())).thenThrow(new IllegalStateException("event store unavailable"));

        AgentTaskPauseRequest request = new AgentTaskPauseRequest();
        request.setReason("review");

        assertThrows(IllegalStateException.class,
                () -> service(taskMapper, stepMapper, eventMapper).pause("task-1", 7L, request));
        verify(taskMapper, never()).selectByTaskId("task-1");
    }

    @Test
    void shouldRejectUnboundedPlannerInputBeforeStateOrEventMutation() {
        SysAgentTaskMapper taskMapper = mock(SysAgentTaskMapper.class);
        SysAgentTaskStepMapper stepMapper = mock(SysAgentTaskStepMapper.class);
        SysAgentTaskEventMapper eventMapper = mock(SysAgentTaskEventMapper.class);
        when(taskMapper.selectOwned("task-1", 7L)).thenReturn(task("WAITING_INPUT"));
        AgentTaskInputRequest request = new AgentTaskInputRequest();
        request.setInput(Map.of("region", "x".repeat(1_001)));

        assertThrows(BusinessException.class,
                () -> service(taskMapper, stepMapper, eventMapper)
                        .submitInput("task-1", 7L, request));

        verify(taskMapper, never()).transition(any());
        verify(eventMapper, never()).insert(any());
    }

    private AgentTaskStateService service(
            SysAgentTaskMapper taskMapper,
            SysAgentTaskStepMapper stepMapper,
            SysAgentTaskEventMapper eventMapper
    ) {
        return new AgentTaskStateService(
                taskMapper,
                stepMapper,
                mock(SysAgentAuditService.class),
                new ObjectMapper(),
                new AgentTaskProperties(),
                new AgentTaskResponseMapper(new AgentTaskProperties()),
                mock(AgentTaskMetricsRecorder.class),
                eventMapper
        );
    }

    private SysAgentTask task(String status) {
        SysAgentTask task = new SysAgentTask();
        task.setTaskId("task-1");
        task.setGoal("inspect runtime");
        task.setStatus(status);
        task.setOperatorId(7L);
        task.setOperatorName("admin");
        task.setMaxSteps(5);
        task.setCompletedSteps(0);
        task.setCurrentStepOrder(0);
        task.setLeaseOwner("worker-1");
        return task;
    }

    private SysAgentTaskEvent event(long cursor, AgentTaskEventType type) {
        SysAgentTaskEvent event = new SysAgentTaskEvent();
        event.setId(cursor);
        event.setTaskId("task-1");
        event.setEventType(type.name());
        event.setActorType("WORKER");
        event.setDetailJson("{}");
        return event;
    }

    private static final class AgentTaskResponseAssertions {
        private static void assertStatus(com.xiaou.system.dto.AgentTaskResponse response, String expected) {
            assertEquals(expected, response.getStatus());
        }
    }
}
