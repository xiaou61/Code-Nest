package com.xiaou.system.agent.tools;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.OperationLogQueryRequest;
import com.xiaou.system.dto.OperationLogResponse;
import com.xiaou.system.service.SysOperationLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogListAgentToolTest {

    private final SysOperationLogService operationLogService = mock(SysOperationLogService.class);
    private final OperationLogListAgentTool tool = new OperationLogListAgentTool(operationLogService);
    private final OperationLogCleanAgentTool cleanTool = new OperationLogCleanAgentTool(operationLogService);
    private final AgentExecutionContext context = new AgentExecutionContext("session-1", "", new AgentOperator(7L, "admin"));

    @Test
    void shouldResolveRecentOperationLogQueryWithClampedLimit() {
        Optional<AgentToolCall> call = tool.resolve("帮我查最近99条操作日志");

        assertTrue(call.isPresent());
        assertEquals("system.operationLog.list", call.get().getToolName());
        assertEquals(20, call.get().getInput().get("pageSize"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExecuteTypedOperationLogServiceAndReturnArtifact() {
        when(operationLogService.getOperationLogPage(any()))
                .thenReturn(PageResult.of(1, 3, 1L, List.of(operationLog())));
        AgentToolCall call = new AgentToolCall();
        call.setToolName("system.operationLog.list");
        call.setInput(Map.of("pageSize", 3));

        AgentToolResult result = tool.execute(call, context);

        assertTrue(result.isSuccess());
        assertEquals("已查询到最近 1 条操作日志。", result.getSummary());
        assertEquals("operationLogList", result.getArtifacts().get(0).getType());
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getArtifacts().get(0).getData().get("records");
        assertEquals("系统", records.get(0).get("module"));

        ArgumentCaptor<OperationLogQueryRequest> captor = ArgumentCaptor.forClass(OperationLogQueryRequest.class);
        verify(operationLogService).getOperationLogPage(captor.capture());
        assertEquals(1, captor.getValue().getPageNum());
        assertEquals(3, captor.getValue().getPageSize());
    }

    @Test
    void shouldResolveDestructiveCleanupRequest() {
        Optional<AgentToolCall> call = cleanTool.resolve("请清理 30 天前的操作日志");

        assertTrue(call.isPresent());
        assertEquals("system.operationLog.cleanExpired", call.get().getToolName());
        assertEquals(30, call.get().getInput().get("days"));
        assertEquals("high", cleanTool.definition().getRiskLevel());
        assertEquals("DESTRUCTIVE_WRITE", cleanTool.definition().getRiskCategory());
        assertTrue(cleanTool.definition().isConfirmationRequired());
    }

    @Test
    void shouldPreviewCleanupWithoutCallingService() {
        AgentToolCall call = cleanupCall(30);

        AgentToolPreview preview = cleanTool.preview(call, context);

        assertTrue(preview.isExecutable());
        assertTrue(preview.getSummary().contains("确认前不会执行删除"));
        assertEquals(3, preview.getPlan().size());
        assertEquals(1, preview.getDiff().size());
        assertEquals("operationLogCleanupPreview", preview.getArtifacts().get(0).getType());
        org.mockito.Mockito.verifyNoInteractions(operationLogService);
    }

    @Test
    void shouldExecuteCleanupThroughTypedService() {
        when(operationLogService.cleanOperationLogByDays(30)).thenReturn(true);

        AgentToolResult result = cleanTool.execute(cleanupCall(30), context);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("已清理 30 天前"));
        assertEquals("operationLogCleanupResult", result.getArtifacts().get(0).getType());
        verify(operationLogService).cleanOperationLogByDays(30);
    }

    private AgentToolCall cleanupCall(int days) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName("system.operationLog.cleanExpired");
        call.setInput(Map.of("days", days));
        return call;
    }

    private OperationLogResponse operationLog() {
        OperationLogResponse log = new OperationLogResponse();
        log.setId(1L);
        log.setModule("系统");
        log.setOperationType("SELECT");
        log.setDescription("查询日志");
        log.setOperatorName("admin");
        log.setStatus(0);
        log.setOperationTime(LocalDateTime.of(2026, 7, 9, 15, 0));
        return log;
    }
}
