package com.xiaou.chat.service;

import com.xiaou.common.cache.CacheStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * 聊天 WebSocket 一次性握手票据服务
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class ChatWebSocketTicketService {

    private static final String TICKET_KEY_PREFIX = "xiaou:chat:ws-ticket:";
    private static final int TICKET_BYTE_LENGTH = 32;
    private static final long TICKET_TTL_SECONDS = 60L;
    private static final int MAX_TICKET_LENGTH = 128;

    private final CacheStore cacheStore;
    private final SecureRandom secureRandom = new SecureRandom();

    public String createTicket(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        byte[] randomBytes = new byte[TICKET_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        cacheStore.put(buildKey(ticket), String.valueOf(userId), Duration.ofSeconds(TICKET_TTL_SECONDS));
        return ticket;
    }

    public Long consumeTicket(String ticket) {
        if (!isValidTicket(ticket)) {
            return null;
        }

        String cachedUserId = cacheStore.take(buildKey(ticket), String.class).orElse(null);
        if (cachedUserId == null) {
            return null;
        }

        try {
            return Long.parseLong(cachedUserId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildKey(String ticket) {
        return TICKET_KEY_PREFIX + ticket;
    }

    private boolean isValidTicket(String ticket) {
        return ticket != null
                && !ticket.isBlank()
                && ticket.length() <= MAX_TICKET_LENGTH
                && ticket.matches("^[A-Za-z0-9_-]+$");
    }
}
