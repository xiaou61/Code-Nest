package com.xiaou.common.utils;

import com.xiaou.common.cache.RedisValueStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 消息通知缓存工具类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCacheUtil {
    
    private final RedisValueStore redisValueStore;
    
    private static final String UNREAD_COUNT_KEY = "notification:unread:";
    
    /**
     * 缓存未读数量
     */
    public void cacheUnreadCount(Long userId, int count) {
        try {
            redisValueStore.put(UNREAD_COUNT_KEY + userId, count, Duration.ofMinutes(30));
        } catch (RuntimeException error) {
            log.warn("缓存通知未读数失败，userId={}", userId, error);
        }
    }
    
    /**
     * 获取缓存的未读数量
     */
    public Integer getCachedUnreadCount(Long userId) {
        try {
            return redisValueStore.find(UNREAD_COUNT_KEY + userId, Integer.class).orElse(null);
        } catch (RuntimeException error) {
            log.warn("读取通知未读数缓存失败，userId={}", userId, error);
            return null;
        }
    }
    
    /**
     * 清除未读数量缓存
     */
    public void clearUnreadCountCache(Long userId) {
        try {
            redisValueStore.delete(UNREAD_COUNT_KEY + userId);
        } catch (RuntimeException error) {
            log.warn("清除通知未读数缓存失败，userId={}", userId, error);
        }
    }
}
