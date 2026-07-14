package com.xiaou.chat.controller.admin;

import com.xiaou.chat.domain.ChatMessage;
import com.xiaou.chat.domain.ChatUserBan;
import com.xiaou.chat.dto.*;
import com.xiaou.chat.service.ChatMessageService;
import com.xiaou.chat.service.ChatOnlineUserService;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.chat.websocket.ChatWebSocketHandler;
import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.satoken.StpAdminUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端聊天控制器
 * 
 * @author xiaou
 */
@Slf4j
@RestController
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
public class ChatAdminController {
    
    private final ChatMessageService chatMessageService;
    private final ChatOnlineUserService chatOnlineUserService;
    private final ChatUserBanService chatUserBanService;
    private final ChatRoomService chatRoomService;
    private final ChatWebSocketHandler chatWebSocketHandler;
    
    /**
     * 获取消息列表（管理端）
     */
    @RequireAdmin
    @Log(module = "聊天室管理", type = Log.OperationType.SELECT, description = "查询消息列表")
    @PostMapping("/messages/list")
    public Result<PageResult<ChatMessage>> getMessageList(@RequestBody AdminChatMessageListRequest request) {
        log.info("管理端查询消息列表，request: {}", request);
        PageResult<ChatMessage> result = chatMessageService.getAdminMessageList(request);
        return Result.success(result);
    }
    
    /**
     * 删除消息
     */
    @RequireAdmin
    @Log(module = "聊天室管理", type = Log.OperationType.DELETE, description = "删除消息")
    @DeleteMapping("/messages/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        chatMessageService.deleteMessage(id);
        
        // 通过WebSocket通知所有用户
        chatWebSocketHandler.sendDeleteNotification(id);
        
        log.info("管理员删除消息，消息ID: {}", id);
        return Result.success();
    }
    
    /**
     * 批量删除消息
     */
    @RequireAdmin
    @Log(module = "聊天室管理", type = Log.OperationType.DELETE, description = "批量删除消息")
    @PostMapping("/messages/batch-delete")
    public Result<Void> batchDeleteMessages(@RequestBody List<Long> ids) {
        chatMessageService.batchDeleteMessages(ids);
        
        // 通知所有用户消息已删除
        ids.forEach(chatWebSocketHandler::sendDeleteNotification);
        
        log.info("管理员批量删除消息，数量: {}", ids.size());
        return Result.success();
    }
    
    /**
     * 获取在线用户列表（管理端）
     */
    @RequireAdmin
    @Log(module = "聊天室管理", type = Log.OperationType.SELECT, description = "查询在线用户列表")
    @PostMapping("/users/online")
    public Result<List<ChatOnlineUserResponse>> getOnlineUsers() {
        Long roomId = chatRoomService.getOfficialRoom().getId();
        List<ChatOnlineUserResponse> users = chatOnlineUserService.getOnlineUsers(roomId);
        return Result.success(users);
    }
    
    /**
     * 踢出用户
     */
    @RequireAdmin
    @Log(module = "聊天室管理", type = Log.OperationType.UPDATE, description = "踢出用户")
    @PostMapping("/users/kick")
    public Result<Void> kickUser(@RequestBody Long userId) {
        chatOnlineUserService.kickUser(userId);
        
        // 通过WebSocket踢出用户
        chatWebSocketHandler.kickUser(userId);
        
        log.info("管理员踢出用户，用户ID: {}", userId);
        return Result.success();
    }
    
    /**
     * 禁言用户
     */
    @RequireAdmin
    @Log(module = "聊天室管理", type = Log.OperationType.UPDATE, description = "禁言用户")
    @PostMapping("/users/ban")
    public Result<Void> banUser(@RequestBody ChatBanUserRequest request) {
        Long operatorId = StpAdminUtil.getLoginIdAsLong();
        chatUserBanService.banUser(request, operatorId);
        
        log.info("管理员禁言用户，用户ID: {}, 时长: {}秒, 原因: {}", 
            request.getUserId(), request.getBanDuration(), request.getBanReason());
        return Result.success();
    }

    /**
     * 查询用户当前生效禁言记录
     */
    @RequireAdmin
    @Log(module = "聊天室管理", type = Log.OperationType.SELECT, description = "查询用户禁言状态")
    @PostMapping("/users/ban/active")
    public Result<ChatUserBanResponse> getActiveUserBan(@RequestBody Long userId) {
        ChatUserBan ban = chatUserBanService.getActiveBan(userId);
        return Result.success(toUserBanResponse(ban));
    }
    
    /**
     * 解除禁言
     */
    @RequireAdmin
    @Log(module = "聊天室管理", type = Log.OperationType.UPDATE, description = "解除禁言")
    @PostMapping("/users/unban")
    public Result<Void> unbanUser(@RequestBody Long userId) {
        chatUserBanService.unbanUser(userId);
        
        log.info("管理员解除禁言，用户ID: {}", userId);
        return Result.success();
    }
    
    /**
     * 发送系统公告
     */
    @RequireAdmin
    @Log(module = "聊天室管理", type = Log.OperationType.INSERT, description = "发送系统公告")
    @PostMapping("/announcement")
    public Result<Void> sendAnnouncement(@RequestBody ChatAnnouncementRequest request) {
        // 保存公告到数据库
        chatMessageService.sendAnnouncement(request.getContent());
        
        // 通过WebSocket广播给所有用户
        chatWebSocketHandler.sendSystemMessage(request.getContent());
        
        log.info("管理员发送系统公告，内容: {}", request.getContent());
        return Result.success();
    }

    private ChatUserBanResponse toUserBanResponse(ChatUserBan ban) {
        if (ban == null) {
            return null;
        }
        ChatUserBanResponse response = new ChatUserBanResponse();
        response.setId(ban.getId());
        response.setUserId(ban.getUserId());
        response.setBanReason(ban.getBanReason());
        response.setBanStartTime(ban.getBanStartTime());
        response.setBanEndTime(ban.getBanEndTime());
        response.setStatus(ban.getStatus());
        return response;
    }
}
