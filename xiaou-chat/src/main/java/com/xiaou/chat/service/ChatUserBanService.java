package com.xiaou.chat.service;

import com.xiaou.chat.domain.ChatUserBan;
import com.xiaou.chat.dto.ChatBanUserRequest;

/**
 * 用户禁言Service接口
 * 
 * @author xiaou
 */
public interface ChatUserBanService {
    
    /**
     * 禁言用户
     */
    void banUser(ChatBanUserRequest request, Long operatorId);
    
    /**
     * 解除禁言
     */
    void unbanUser(Long userId);

    /**
     * 查询用户当前生效禁言记录
     */
    ChatUserBan getActiveBan(Long userId);
    
    /**
     * 检查用户是否被禁言
     */
    boolean isUserBanned(Long userId, Long roomId);
    
    /**
     * 自动解除过期禁言
     */
    void autoUnbanExpired();
}
