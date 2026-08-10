package com.xiaou.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentOperatorResolver;
import com.xiaou.system.agent.task.AgentTaskCycleRuntime;
import com.xiaou.system.agent.task.AgentTaskStateService;
import com.xiaou.system.dto.AgentTaskCancelRequest;
import com.xiaou.system.dto.AgentTaskConfirmRequest;
import com.xiaou.system.dto.AgentTaskCreateRequest;
import com.xiaou.system.dto.AgentTaskEventPageResponse;
import com.xiaou.system.dto.AgentTaskInputRequest;
import com.xiaou.system.dto.AgentTaskPauseRequest;
import com.xiaou.system.dto.AgentTaskResponse;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentTaskControllerTest {

    @Test
    void shouldExposeOwnerScopedDurableTaskEndpoints() throws Exception {
        assertNotNull(AgentTaskController.class.getAnnotation(RestController.class));
        RequestMapping classMapping = AgentTaskController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/admin/agent/tasks"}, classMapping.value());

        Method create = AgentTaskController.class.getMethod("create", AgentTaskCreateRequest.class);
        assertNotNull(create.getAnnotation(PostMapping.class));
        assertValidBody(create, 0);
        assertAdmin(create);

        Method list = AgentTaskController.class.getMethod("list", String.class, Integer.class);
        assertNotNull(list.getAnnotation(GetMapping.class));
        assertAdmin(list);

        Method events = AgentTaskController.class.getMethod("events", String.class, Long.class, Integer.class);
        assertArrayEquals(new String[]{"/{taskId}/events"}, events.getAnnotation(GetMapping.class).value());
        assertAdmin(events);

        Method detail = AgentTaskController.class.getMethod("detail", String.class);
        assertArrayEquals(new String[]{"/{taskId}"}, detail.getAnnotation(GetMapping.class).value());
        assertAdmin(detail);

        Method confirm = AgentTaskController.class.getMethod(
                "confirm", String.class, AgentTaskConfirmRequest.class);
        assertArrayEquals(new String[]{"/{taskId}/confirm"}, confirm.getAnnotation(PostMapping.class).value());
        assertValidBody(confirm, 1);
        assertAdmin(confirm);

        Method cancel = AgentTaskController.class.getMethod(
                "cancel", String.class, AgentTaskCancelRequest.class);
        assertArrayEquals(new String[]{"/{taskId}/cancel"}, cancel.getAnnotation(PostMapping.class).value());
        assertValidBody(cancel, 1);
        assertAdmin(cancel);

        Method pause = AgentTaskController.class.getMethod("pause", String.class, AgentTaskPauseRequest.class);
        assertArrayEquals(new String[]{"/{taskId}/pause"}, pause.getAnnotation(PostMapping.class).value());
        assertValidBody(pause, 1);
        assertAdmin(pause);

        Method resume = AgentTaskController.class.getMethod("resume", String.class);
        assertArrayEquals(new String[]{"/{taskId}/resume"}, resume.getAnnotation(PostMapping.class).value());
        assertAdmin(resume);

        Method input = AgentTaskController.class.getMethod("submitInput", String.class, AgentTaskInputRequest.class);
        assertArrayEquals(new String[]{"/{taskId}/input"}, input.getAnnotation(PostMapping.class).value());
        assertValidBody(input, 1);
        assertAdmin(input);
    }

    @Test
    void shouldResolveCurrentOperatorAndScopeEveryOperationToLoginId() {
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskCycleRuntime cycleRuntime = mock(AgentTaskCycleRuntime.class);
        AgentOperatorResolver operatorResolver = mock(AgentOperatorResolver.class);
        AgentTaskController controller = new AgentTaskController(stateService, cycleRuntime, operatorResolver);
        AgentOperator operator = new AgentOperator(7L, "admin", "", List.of("ADMIN"), List.of());
        AgentTaskResponse task = new AgentTaskResponse();
        task.setTaskId("agent-task-1");
        when(operatorResolver.resolve(7L)).thenReturn(operator);
        when(stateService.create(any(), eq(operator))).thenReturn(task);
        when(stateService.listOwned(7L, "RUNNING", 20)).thenReturn(List.of(task));
        when(stateService.detail("agent-task-1", 7L)).thenReturn(task);
        AgentTaskEventPageResponse eventPage = new AgentTaskEventPageResponse();
        when(stateService.events("agent-task-1", 7L, 10L, 20)).thenReturn(eventPage);
        when(stateService.pause(eq("agent-task-1"), eq(7L), any())).thenReturn(task);
        when(stateService.resume("agent-task-1", 7L)).thenReturn(task);
        when(stateService.submitInput(eq("agent-task-1"), eq(7L), any())).thenReturn(task);
        when(cycleRuntime.confirm("agent-task-1", operator, "CONFIRM-WRITE")).thenReturn(task);
        when(stateService.cancel(eq("agent-task-1"), eq(7L), any())).thenReturn(task);

        AgentTaskCreateRequest createRequest = new AgentTaskCreateRequest();
        createRequest.setGoal("inspect runtime");
        AgentTaskConfirmRequest confirmRequest = new AgentTaskConfirmRequest();
        confirmRequest.setConfirmationText("CONFIRM-WRITE");
        AgentTaskCancelRequest cancelRequest = new AgentTaskCancelRequest();
        cancelRequest.setReason("stop");
        AgentTaskPauseRequest pauseRequest = new AgentTaskPauseRequest();
        pauseRequest.setReason("review");
        AgentTaskInputRequest inputRequest = new AgentTaskInputRequest();
        inputRequest.setInput(java.util.Map.of("region", "cn"));

        try (MockedStatic<StpAdminUtil> stpAdminUtil = mockStatic(StpAdminUtil.class)) {
            stpAdminUtil.when(StpAdminUtil::getLoginIdAsLong).thenReturn(7L);

            Result<AgentTaskResponse> created = controller.create(createRequest);
            Result<List<AgentTaskResponse>> listed = controller.list("RUNNING", 20);
            Result<AgentTaskResponse> detailed = controller.detail("agent-task-1");
            Result<AgentTaskEventPageResponse> events = controller.events(
                    "agent-task-1", 10L, 20);
            Result<AgentTaskResponse> paused = controller.pause("agent-task-1", pauseRequest);
            Result<AgentTaskResponse> resumed = controller.resume("agent-task-1");
            Result<AgentTaskResponse> input = controller.submitInput("agent-task-1", inputRequest);
            Result<AgentTaskResponse> confirmed = controller.confirm("agent-task-1", confirmRequest);
            Result<AgentTaskResponse> cancelled = controller.cancel("agent-task-1", cancelRequest);

            assertEquals(task, created.getData());
            assertEquals(List.of(task), listed.getData());
            assertEquals(task, detailed.getData());
            assertEquals(eventPage, events.getData());
            assertEquals(task, paused.getData());
            assertEquals(task, resumed.getData());
            assertEquals(task, input.getData());
            assertEquals(task, confirmed.getData());
            assertEquals(task, cancelled.getData());
        }

        verify(stateService).create(createRequest, operator);
        verify(stateService).listOwned(7L, "RUNNING", 20);
        verify(stateService).detail("agent-task-1", 7L);
        verify(stateService).events("agent-task-1", 7L, 10L, 20);
        verify(stateService).pause("agent-task-1", 7L, pauseRequest);
        verify(stateService).resume("agent-task-1", 7L);
        verify(stateService).submitInput("agent-task-1", 7L, inputRequest);
        verify(cycleRuntime).confirm("agent-task-1", operator, "CONFIRM-WRITE");
        verify(stateService).cancel("agent-task-1", 7L, cancelRequest);
    }

    @Test
    void shouldRejectInvalidTaskAndConfirmationBodiesBeforeDelegation() throws Exception {
        AgentTaskStateService stateService = mock(AgentTaskStateService.class);
        AgentTaskCycleRuntime cycleRuntime = mock(AgentTaskCycleRuntime.class);
        AgentOperatorResolver operatorResolver = mock(AgentOperatorResolver.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new AgentTaskController(stateService, cycleRuntime, operatorResolver)
        ).build();

        mockMvc.perform(post("/admin/agent/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new AgentTaskCreateRequest())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/admin/agent/tasks/agent-task-1/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationText\":\" \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stateService, cycleRuntime, operatorResolver);
    }

    private void assertAdmin(Method method) {
        assertNotNull(method.getAnnotation(RequireAdmin.class));
    }

    private void assertValidBody(Method method, int parameterIndex) {
        assertTrue(java.util.Arrays.stream(method.getParameterAnnotations()[parameterIndex])
                .anyMatch(annotation -> annotation.annotationType().equals(Valid.class)));
    }
}
