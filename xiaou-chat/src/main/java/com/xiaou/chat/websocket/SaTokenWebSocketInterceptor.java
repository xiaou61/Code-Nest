package com.xiaou.chat.websocket;

import cn.dev33.satoken.session.SaSession;
import com.xiaou.chat.service.ChatWebSocketTicketService;
import com.xiaou.common.satoken.StpUserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器 - 基于 Sa-Token 认证
 * 
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaTokenWebSocketInterceptor implements HandshakeInterceptor {

    private final ChatWebSocketTicketService chatWebSocketTicketService;
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

                String ticket = UriComponentsBuilder.fromUri(servletRequest.getURI())
                    .build()
                    .getQueryParams()
                    .getFirst("ticket");
                Long userId = chatWebSocketTicketService.consumeTicket(ticket);
                if (userId != null) {
                    String username = getUsername(userId);

                    attributes.put("userId", userId);
                    attributes.put("username", username != null ? username : "用户" + userId);

                    log.info("WebSocket握手成功，用户ID: {}, 用户名: {}", userId, username);
                    return true;
                } else {
                    log.warn("WebSocket握手失败，票据无效或已过期");
                }
            }
        } catch (Exception e) {
            log.error("WebSocket握手异常", e);
        }
        
        return false;
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // 握手后处理
        if (exception != null) {
            log.error("WebSocket握手后处理异常", exception);
        }
    }

    private String getUsername(Long userId) {
        try {
            SaSession session = StpUserUtil.stpLogic.getSessionByLoginId(userId);
            Object username = session.get("username");
            return username != null ? username.toString() : null;
        } catch (Exception e) {
            log.warn("获取用户名失败: {}", e.getMessage());
            return null;
        }
    }
}
