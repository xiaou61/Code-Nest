package com.xiaou.chat.websocket;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.chat.domain.ChatMessage;
import com.xiaou.chat.domain.ChatRoom;
import com.xiaou.chat.dto.ChatMessageRequest;
import com.xiaou.chat.dto.ChatMessageResponse;
import com.xiaou.chat.service.ChatMessageService;
import com.xiaou.chat.service.ChatOnlineUserService;
import com.xiaou.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天WebSocket处理器
 * 
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final ChatMessageService chatMessageService;
    private final ChatOnlineUserService chatOnlineUserService;
    private final ChatRoomService chatRoomService;
    
    // 存储所有在线用户的WebSocket会话
    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();
    
    /**
     * 连接建立后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");
        
        if (userId == null) {
            log.warn("用户ID为空，关闭连接");
            session.close();
            return;
        }
        
        // 保存会话
        SESSIONS.put(sessionId, session);
        
        // 获取IP和设备信息
        String ipAddress = getIpAddress(session);
        String deviceInfo = getUserAgent(session);
        
        // 记录用户上线
        ChatRoom room = chatRoomService.getOfficialRoom();
        chatOnlineUserService.userOnline(userId, room.getId(), sessionId, ipAddress, deviceInfo);
        
        // 发送连接成功消息
        WebSocketMessage connectMsg = new WebSocketMessage(WebSocketMessage.MessageType.CONNECT, 
            Map.of("message", "连接成功", "userId", userId, "username", username));
        sendMessage(session, connectMsg);
        
        // 广播在线人数
        broadcastOnlineCount(room.getId());
        
        // 广播用户加入消息
        WebSocketMessage joinMsg = new WebSocketMessage(WebSocketMessage.MessageType.USER_JOIN,
            Map.of("userId", userId, "username", username));
        broadcastMessage(joinMsg, sessionId);
        
        log.info("用户连接成功，用户ID: {}, 会话ID: {}, 当前在线: {}", userId, sessionId, SESSIONS.size());
    }
    
    /**
     * 接收消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        Long userId = (Long) session.getAttributes().get("userId");
        
        if (userId == null) {
            log.warn("用户未登录，忽略消息");
            return;
        }
        
        try {
            String payload = message.getPayload();
            WebSocketMessage wsMessage = JSONUtil.toBean(payload, WebSocketMessage.class);
            
            String type = wsMessage.getType();
            
            // 处理心跳包
            if (WebSocketMessage.MessageType.HEARTBEAT.equals(type)) {
                chatOnlineUserService.updateHeartbeat(sessionId);
                return;
            }
            
            // 处理聊天消息
            if (WebSocketMessage.MessageType.MESSAGE.equals(type)) {
                handleChatMessage(session, wsMessage);
            }
            
        } catch (Exception e) {
            log.error("处理WebSocket消息异常，用户ID: {}", userId, e);
            WebSocketMessage errorMsg = new WebSocketMessage(WebSocketMessage.MessageType.ERROR,
                Map.of("message", "消息处理失败: " + e.getMessage()));
            sendMessage(session, errorMsg);
        }
    }
    
    /**
     * 处理聊天消息
     */
    private void handleChatMessage(WebSocketSession session, WebSocketMessage wsMessage) {
        Long userId = (Long) session.getAttributes().get("userId");
        String ipAddress = getIpAddress(session);
        String deviceInfo = getUserAgent(session);
        
        // 解析消息请求
        Map<String, Object> dataMap = toObjectMap(wsMessage.getData());
        ChatMessageRequest request = BeanUtil.toBean(dataMap, ChatMessageRequest.class);
        
        // 保存消息到数据库
        ChatMessage chatMessage = chatMessageService.sendMessage(request, userId, ipAddress, deviceInfo);
        
        // 转换为响应DTO
        ChatMessageResponse response = BeanUtil.copyProperties(chatMessage, ChatMessageResponse.class);
        response.setCanRecall(true); // 刚发送的消息可撤回
        
        // 广播消息给所有在线用户（排除发送者，避免重复显示）
        WebSocketMessage broadcastMsg = new WebSocketMessage(WebSocketMessage.MessageType.MESSAGE, response);
        broadcastMessage(broadcastMsg, session.getId());
        
        // 给发送者发送确认消息（用于更新本地消息ID和状态）
        WebSocketMessage ackMsg = new WebSocketMessage(WebSocketMessage.MessageType.MESSAGE_ACK, response);
        sendMessage(session, ackMsg);
        
        log.info("用户发送消息，用户ID: {}, 消息ID: {}", userId, chatMessage.getId());
    }

    private Map<String, Object> toObjectMap(Object data) {
        if (!(data instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("消息数据格式不正确");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> {
            if (key != null) {
                result.put(key.toString(), value);
            }
        });
        return result;
    }
    
    /**
     * 连接关闭后
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");
        
        // 移除会话
        SESSIONS.remove(sessionId);
        
        // 记录用户下线
        chatOnlineUserService.userOffline(sessionId);
        
        if (userId != null) {
            // 广播在线人数
            ChatRoom room = chatRoomService.getOfficialRoom();
            broadcastOnlineCount(room.getId());
            
            // 广播用户离开消息
            WebSocketMessage leaveMsg = new WebSocketMessage(WebSocketMessage.MessageType.USER_LEAVE,
                Map.of("userId", userId, "username", username != null ? username : ""));
            broadcastMessage(leaveMsg, sessionId);
            
            log.info("用户断开连接，用户ID: {}, 会话ID: {}, 当前在线: {}", userId, sessionId, SESSIONS.size());
        }
    }
    
    /**
     * 处理异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        String exceptionMsg = exception.getMessage();
        
        // 判断是否为客户端正常断开连接（浏览器关闭、刷新等）
        if (exception instanceof java.io.IOException && 
            (exceptionMsg != null && (
                exceptionMsg.contains("你的主机中的软件中止了一个已建立的连接") ||
                exceptionMsg.contains("Connection reset") ||
                exceptionMsg.contains("Broken pipe")
            ))) {
            // 客户端正常断开，使用 debug 级别，不打印堆栈
            log.debug("WebSocket连接断开（客户端关闭），会话ID: {}", sessionId);
        } else {
            // 其他异常，使用 error 级别，打印堆栈
            log.error("WebSocket传输异常，会话ID: {}", sessionId, exception);
        }
        
        if (session.isOpen()) {
            session.close();
        }
    }
    
    /**
     * 发送消息给指定会话
     */
    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        if (session != null && session.isOpen()) {
            try {
                String json = JSONUtil.toJsonStr(message);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("发送消息失败，会话ID: {}", session.getId(), e);
            }
        }
    }
    
    /**
     * 广播消息给所有在线用户（可排除指定会话）
     */
    private void broadcastMessage(WebSocketMessage message, String excludeSessionId) {
        String json = JSONUtil.toJsonStr(message);
        SESSIONS.forEach((sessionId, session) -> {
            if (!sessionId.equals(excludeSessionId) && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("广播消息失败，会话ID: {}", sessionId, e);
                }
            }
        });
    }
    
    /**
     * 广播在线人数
     */
    private void broadcastOnlineCount(Long roomId) {
        Integer count = chatOnlineUserService.getOnlineCount(roomId);
        WebSocketMessage message = new WebSocketMessage(WebSocketMessage.MessageType.ONLINE_COUNT,
            Map.of("count", count));
        broadcastMessage(message, null);
    }
    
    /**
     * 发送系统消息（全员广播）
     */
    public void sendSystemMessage(String content) {
        WebSocketMessage message = new WebSocketMessage(WebSocketMessage.MessageType.SYSTEM,
            Map.of("content", content));
        broadcastMessage(message, null);
    }
    
    /**
     * 踢出用户
     */
    public void kickUser(Long userId) {
        SESSIONS.forEach((sessionId, session) -> {
            Long sessionUserId = (Long) session.getAttributes().get("userId");
            if (sessionUserId != null && sessionUserId.equals(userId)) {
                WebSocketMessage kickMsg = new WebSocketMessage(WebSocketMessage.MessageType.KICK_OUT,
                    Map.of("message", "您已被管理员踢出聊天室"));
                sendMessage(session, kickMsg);
                
                try {
                    session.close();
                } catch (IOException e) {
                    log.error("关闭会话失败，会话ID: {}", sessionId, e);
                }
            }
        });
    }
    
    /**
     * 发送消息撤回通知
     */
    public void sendRecallNotification(Long messageId) {
        WebSocketMessage message = new WebSocketMessage(WebSocketMessage.MessageType.MESSAGE_RECALL,
            Map.of("messageId", messageId));
        broadcastMessage(message, null);
    }
    
    /**
     * 发送消息删除通知
     */
    public void sendDeleteNotification(Long messageId) {
        WebSocketMessage message = new WebSocketMessage(WebSocketMessage.MessageType.MESSAGE_DELETE,
            Map.of("messageId", messageId));
        broadcastMessage(message, null);
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(WebSocketSession session) {
        try {
            return session.getRemoteAddress().getAddress().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 获取User-Agent
     */
    private String getUserAgent(WebSocketSession session) {
        try {
            return (String) session.getHandshakeHeaders().getFirst("User-Agent");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
