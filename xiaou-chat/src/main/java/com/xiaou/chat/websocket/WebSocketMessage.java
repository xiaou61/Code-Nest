package com.xiaou.chat.websocket;

import lombok.Data;

/**
 * WebSocket消息封装
 * 
 * @author xiaou
 */
@Data
public class WebSocketMessage {
    
    /**
     * 消息类型
     */
    private String type;
    
    /**
     * 消息数据
     */
    private Object data;
    
    public WebSocketMessage() {}
    
    public WebSocketMessage(String type, Object data) {
        this.type = type;
        this.data = data;
    }
    
    /**
     * 消息类型常量
     */
    public static class MessageType {
        public static final String CONNECT = "CONNECT";           // 连接成功
        public static final String MESSAGE = "MESSAGE";           // 普通消息
        public static final String SYSTEM = "SYSTEM";             // 系统消息
        public static final String ONLINE_COUNT = "ONLINE_COUNT"; // 在线人数更新
        public static final String USER_JOIN = "USER_JOIN";       // 用户加入
        public static final String USER_LEAVE = "USER_LEAVE";     // 用户离开
        public static final String MESSAGE_ACK = "MESSAGE_ACK";       // 消息发送确认
        public static final String MESSAGE_RECALL = "MESSAGE_RECALL"; // 消息撤回
        public static final String MESSAGE_DELETE = "MESSAGE_DELETE"; // 消息删除
        public static final String KICK_OUT = "KICK_OUT";         // 被踢出
        public static final String HEARTBEAT = "HEARTBEAT";       // 心跳包
        public static final String PONG = "PONG";                 // 心跳响应
        public static final String TYPING = "TYPING";             // 输入中状态
        public static final String ERROR = "ERROR";               // 错误消息
    }
}
