package com.xiaou.system.agent.tools;

import com.xiaou.chat.domain.ChatUserBan;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentTool;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.agent.AgentToolPreview;
import com.xiaou.system.agent.AgentToolResult;
import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.dto.AgentChatDiffItem;
import com.xiaou.system.dto.AgentChatPlanStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解除聊天室用户禁言工具。
 *
 * @author xiaou
 */
@Component
@RequiredArgsConstructor
public class ChatUserUnbanAgentTool implements AgentTool {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("(?:用户|uid|userId)?\\s*(\\d+)");

    private final ChatUserBanService chatUserBanService;

    @Override
    public AgentToolDefinition definition() {
        AgentToolDefinition definition = new AgentToolDefinition();
        definition.setName("chat.userBan.unban");
        definition.setTitle("解除聊天室用户禁言");
        definition.setDescription("解除指定用户在官方聊天室当前生效的禁言。");
        definition.setIntent("chat.userBan.unban");
        definition.setRoute("/admin/chat/users/unban");
        definition.setRiskLevel("medium");
        definition.setRiskCategory("WRITE");
        definition.setConfirmationRequired(true);
        definition.setConfirmationText("确认解除禁言");
        definition.setInputSchema(Map.of(
                "userId", Map.of("type", "integer", "minimum", 1, "description", "用户ID")
        ));
        definition.setRequiredInputKeys(List.of("userId"));
        definition.setRequiredPermissions(List.of("agent:chat:user-ban:write"));
        return definition;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!normalized.contains("禁言") || !containsAny(normalized, List.of("解除", "解禁", "取消禁言"))) {
            return Optional.empty();
        }

        Long userId = extractUserId(normalized);
        if (userId == null) {
            return Optional.empty();
        }

        AgentToolCall call = new AgentToolCall();
        call.setToolName(definition().getName());
        call.setSummary("解除用户 " + userId + " 的聊天室禁言");
        call.setInput(new LinkedHashMap<>(Map.of("userId", userId)));
        return Optional.of(call);
    }

    @Override
    public AgentToolPreview preview(AgentToolCall call, AgentExecutionContext context) {
        Long userId = longValue(call.getInput().get("userId"));
        ChatUserBan ban = chatUserBanService.getActiveBan(userId);

        AgentToolPreview preview = new AgentToolPreview();
        if (ban == null) {
            String message = "用户 " + userId + " 当前未被禁言，无需解除。";
            preview.setExecutable(false);
            preview.setSummary(message);
            preview.setBlockedReason(message);
            preview.getPlan().add(new AgentChatPlanStep("查询当前状态", "done", "未发现生效禁言记录。"));
            preview.getArtifacts().add(new AgentChatArtifact(
                    "chatUserBanUnbanPreview",
                    "聊天室解除禁言预览",
                    Map.of("userId", userId, "active", false)
            ));
            return preview;
        }

        preview.setSummary("将解除用户 " + userId + " 的聊天室禁言。确认前不会修改禁言状态。");
        preview.getPlan().add(new AgentChatPlanStep("查询当前状态", "done", "已找到生效禁言记录：" + ban.getId()));
        preview.getPlan().add(new AgentChatPlanStep("生成预览", "done", "解除用户 " + userId + " 的当前禁言。"));
        preview.getPlan().add(new AgentChatPlanStep("等待确认", "blocked", "需要输入强确认文本后才会执行。"));
        preview.getDiff().add(new AgentChatDiffItem(
                "chatUserBan.status",
                "生效中",
                "已解除",
                "用户 " + userId + " 的聊天室禁言状态将被解除"
        ));
        preview.getArtifacts().add(new AgentChatArtifact(
                "chatUserBanUnbanPreview",
                "聊天室解除禁言预览",
                banData(ban, true)
        ));
        return preview;
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        Long userId = longValue(call.getInput().get("userId"));
        chatUserBanService.unbanUser(userId);

        AgentToolResult result = new AgentToolResult();
        result.setSuccess(true);
        result.setSummary("已解除用户 " + userId + " 的聊天室禁言。");
        result.getDiff().add(new AgentChatDiffItem(
                "chatUserBan.status",
                "生效中",
                "已解除",
                "用户 " + userId + " 的聊天室禁言状态已更新"
        ));
        result.getArtifacts().add(new AgentChatArtifact(
                "chatUserBanUnbanResult",
                "聊天室解除禁言结果",
                Map.of("userId", userId, "success", true)
        ));
        result.getNextActions().add("可以继续查询用户" + userId + "禁言状态，确认当前已解除。");
        return result;
    }

    private Map<String, Object> banData(ChatUserBan ban, boolean active) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("active", active);
        data.put("id", ban.getId());
        data.put("userId", ban.getUserId());
        data.put("roomId", ban.getRoomId());
        data.put("banReason", ban.getBanReason());
        data.put("banStartTime", ban.getBanStartTime());
        data.put("banEndTime", ban.getBanEndTime());
        data.put("operatorId", ban.getOperatorId());
        data.put("status", ban.getStatus());
        return data;
    }

    private Long extractUserId(String message) {
        Matcher matcher = USER_ID_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return Long.parseLong(matcher.group(1));
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private boolean containsAny(String value, List<String> keywords) {
        return keywords.stream().anyMatch(value::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
