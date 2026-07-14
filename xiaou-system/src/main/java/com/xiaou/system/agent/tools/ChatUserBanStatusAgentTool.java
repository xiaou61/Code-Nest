package com.xiaou.system.agent.tools;

import com.xiaou.chat.domain.ChatUserBan;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.system.agent.AbstractReadonlyAgentTool;
import com.xiaou.system.agent.AgentExecutionContext;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinitionBuilder;
import com.xiaou.system.agent.AgentToolResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询聊天室用户禁言状态工具。
 *
 * @author xiaou
 */
@Component
public class ChatUserBanStatusAgentTool extends AbstractReadonlyAgentTool {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("(?:用户|uid|userId)?\\s*(\\d+)");

    private final ChatUserBanService chatUserBanService;

    public ChatUserBanStatusAgentTool(ChatUserBanService chatUserBanService) {
        super(AgentToolDefinitionBuilder.readonly("chat.userBan.active", "查询聊天室用户禁言状态")
                .description("查询指定用户在官方聊天室当前是否存在生效禁言。")
                .route("/admin/chat/users/ban/active")
                .input("userId", Map.of("type", "integer", "minimum", 1, "description", "用户ID"), true)
                .permission("agent:chat:user-ban:read")
                .build());
        this.chatUserBanService = chatUserBanService;
    }

    @Override
    public Optional<AgentToolCall> resolve(String message) {
        String normalized = normalize(message);
        if (!normalized.contains("禁言") || containsAny(normalized, List.of("解除", "解禁", "取消禁言"))) {
            return Optional.empty();
        }
        if (!containsAny(normalized, List.of("查", "看", "状态", "是否", "当前", "有没有", "被禁言"))) {
            return Optional.empty();
        }

        Long userId = extractUserId(normalized);
        if (userId == null) {
            return Optional.empty();
        }

        return Optional.of(call("查询用户 " + userId + " 的聊天室禁言状态", Map.of("userId", userId)));
    }

    @Override
    public AgentToolResult execute(AgentToolCall call, AgentExecutionContext context) {
        Long userId = longValue(call.getInput().get("userId"));
        ChatUserBan ban = chatUserBanService.getActiveBan(userId);

        if (ban == null) {
            return success(
                    "用户 " + userId + " 当前未被禁言。",
                    artifact("chatUserBanStatus", "聊天室用户禁言状态", Map.of("userId", userId, "active", false)),
                    List.of("如果需要禁言该用户，请说明禁言时长和原因。")
            );
        }

        return success(
                "用户 " + userId + " 当前处于禁言中。",
                artifact("chatUserBanStatus", "聊天室用户禁言状态", banData(ban, true)),
                List.of("如果确认需要解除禁言，可以继续说：解除用户" + userId + "禁言。")
        );
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

}
