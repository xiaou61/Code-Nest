package com.xiaou.system.agent.tools;

import com.xiaou.chat.domain.ChatUserBan;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatUserBanAgentToolTest {

    private final ChatUserBanService chatUserBanService = mock(ChatUserBanService.class);
    private final ChatUserBanStatusAgentTool statusTool = new ChatUserBanStatusAgentTool(chatUserBanService);
    private final ChatUserUnbanAgentTool unbanTool = new ChatUserUnbanAgentTool(chatUserBanService);
    private final AgentExecutionContext context = new AgentExecutionContext("session-1", "", new AgentOperator(7L, "admin"));

    @Test
    void statusToolShouldResolveReadonlyQuery() {
        Optional<AgentToolCall> call = statusTool.resolve("查询用户88禁言状态");

        assertTrue(call.isPresent());
        assertEquals("chat.userBan.active", call.get().getToolName());
        assertEquals(88L, call.get().getInput().get("userId"));
        assertEquals("readonly", statusTool.definition().getRiskLevel());
    }

    @Test
    void statusToolShouldAnswerInactiveUser() {
        AgentToolCall call = call("chat.userBan.active", 88L);
        when(chatUserBanService.getActiveBan(88L)).thenReturn(null);

        AgentToolResult result = statusTool.execute(call, context);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("当前未被禁言"));
        assertEquals(1, result.getArtifacts().size());
        verify(chatUserBanService).getActiveBan(88L);
    }

    @Test
    void unbanToolShouldResolveWriteRequestWithoutMatchingReadonlyQuery() {
        Optional<AgentToolCall> queryCall = unbanTool.resolve("查询用户88禁言状态");
        Optional<AgentToolCall> unbanCall = unbanTool.resolve("解除用户88禁言");

        assertFalse(queryCall.isPresent());
        assertTrue(unbanCall.isPresent());
        assertEquals("chat.userBan.unban", unbanCall.get().getToolName());
        assertEquals(88L, unbanCall.get().getInput().get("userId"));
        assertTrue(unbanTool.definition().isConfirmationRequired());
    }

    @Test
    void unbanPreviewShouldBlockWhenUserIsNotBanned() {
        AgentToolCall call = call("chat.userBan.unban", 88L);
        when(chatUserBanService.getActiveBan(88L)).thenReturn(null);

        AgentToolPreview preview = unbanTool.preview(call, context);

        assertFalse(preview.isExecutable());
        assertTrue(preview.getBlockedReason().contains("当前未被禁言"));
        assertEquals(1, preview.getArtifacts().size());
        verify(chatUserBanService).getActiveBan(88L);
    }

    @Test
    void unbanPreviewShouldDescribeActiveBanDiff() {
        AgentToolCall call = call("chat.userBan.unban", 88L);
        when(chatUserBanService.getActiveBan(88L)).thenReturn(activeBan(88L));

        AgentToolPreview preview = unbanTool.preview(call, context);

        assertTrue(preview.isExecutable());
        assertTrue(preview.getSummary().contains("将解除用户 88"));
        assertEquals(3, preview.getPlan().size());
        assertEquals(1, preview.getDiff().size());
        assertEquals(1, preview.getArtifacts().size());
    }

    @Test
    void unbanExecuteShouldCallTypedService() {
        AgentToolCall call = call("chat.userBan.unban", 88L);

        AgentToolResult result = unbanTool.execute(call, context);

        assertTrue(result.isSuccess());
        assertTrue(result.getSummary().contains("已解除用户 88"));
        assertNotNull(result.getArtifacts().get(0).getData());
        verify(chatUserBanService).unbanUser(88L);
    }

    private AgentToolCall call(String toolName, Long userId) {
        AgentToolCall call = new AgentToolCall();
        call.setToolName(toolName);
        call.getInput().put("userId", userId);
        return call;
    }

    private ChatUserBan activeBan(Long userId) {
        ChatUserBan ban = new ChatUserBan();
        ban.setId(9001L);
        ban.setUserId(userId);
        ban.setRoomId(1L);
        ban.setBanReason("刷屏");
        ban.setBanStartTime(new Date(1_700_000_000_000L));
        ban.setBanEndTime(new Date(1_700_003_600_000L));
        ban.setOperatorId(7L);
        ban.setStatus(1);
        return ban;
    }
}
