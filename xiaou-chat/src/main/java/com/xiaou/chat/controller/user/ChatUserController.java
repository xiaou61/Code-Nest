package com.xiaou.chat.controller.user;

import com.xiaou.chat.dto.ChatHistoryRequest;
import com.xiaou.chat.dto.ChatHistoryResponse;
import com.xiaou.chat.dto.ChatOnlineUserResponse;
import com.xiaou.chat.dto.ChatRecallRequest;
import com.xiaou.chat.service.ChatMessageService;
import com.xiaou.chat.service.ChatOnlineUserService;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.chat.service.ChatWebSocketTicketService;
import com.xiaou.chat.websocket.ChatWebSocketHandler;
import com.xiaou.common.annotation.Log;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpUserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端聊天控制器
 * 
 * @author xiaou
 */
@Slf4j
@RestController
@RequestMapping("/user/chat")
@RequiredArgsConstructor
public class ChatUserController {
    
    private final ChatMessageService chatMessageService;
    private final ChatOnlineUserService chatOnlineUserService;
    private final ChatRoomService chatRoomService;
    private final ChatWebSocketTicketService chatWebSocketTicketService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    /**
     * 获取 WebSocket 一次性握手票据
     */
    @PostMapping("/ws-ticket")
    public Result<String> createWebSocketTicket() {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return Result.success(chatWebSocketTicketService.createTicket(userId));
    }
    
    /**
     * 获取历史消息
     */
    @Log(module = "聊天室", type = Log.OperationType.SELECT, description = "获取历史消息")
    @PostMapping("/history")
    public Result<ChatHistoryResponse> getHistory(@RequestBody ChatHistoryRequest request) {
        log.info("获取历史消息，lastMessageId: {}, pageSize: {}", 
            request.getLastMessageId(), request.getPageSize());
        ChatHistoryResponse response = chatMessageService.getHistory(request);
        return Result.success(response);
    }
    
    /**
     * 获取在线人数
     */
    @Log(module = "聊天室", type = Log.OperationType.SELECT, description = "获取在线人数")
    @PostMapping("/online-count")
    public Result<Integer> getOnlineCount() {
        Long roomId = chatRoomService.getOfficialRoom().getId();
        Integer count = chatOnlineUserService.getOnlineCount(roomId);
        return Result.success(count);
    }
    
    /**
     * 获取在线用户列表
     */
    @Log(module = "聊天室", type = Log.OperationType.SELECT, description = "获取在线用户列表")
    @PostMapping("/online-users")
    public Result<List<ChatOnlineUserResponse>> getOnlineUsers() {
        Long roomId = chatRoomService.getOfficialRoom().getId();
        List<ChatOnlineUserResponse> users = chatOnlineUserService.getOnlineUsers(roomId);
        return Result.success(users);
    }
    
    /**
     * 撤回消息
     */
    @Log(module = "聊天室", type = Log.OperationType.DELETE, description = "撤回消息")
    @PostMapping("/message/recall")
    public Result<Void> recallMessage(@RequestBody ChatRecallRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        chatMessageService.recallMessage(request.getMessageId(), userId);
        
        // 通过WebSocket通知所有用户
        chatWebSocketHandler.sendRecallNotification(request.getMessageId());
        
        log.info("用户撤回消息，用户ID: {}, 消息ID: {}", userId, request.getMessageId());
        return Result.success();
    }
}
