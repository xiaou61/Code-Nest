package com.xiaou.chat.service.impl;

import com.xiaou.chat.domain.ChatUserBan;
import com.xiaou.chat.dto.ChatBanUserRequest;
import com.xiaou.chat.mapper.ChatUserBanMapper;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户禁言Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUserBanServiceImpl implements ChatUserBanService {
    
    private final ChatUserBanMapper chatUserBanMapper;
    private final ChatRoomService chatRoomService;
    
    @Override
    public void banUser(ChatBanUserRequest request, Long operatorId) {
        // 获取官方聊天室ID
        Long roomId = chatRoomService.getOfficialRoom().getId();
        
        // 检查是否已被禁言
        ChatUserBan existingBan = chatUserBanMapper.selectActiveByUserId(request.getUserId(), roomId);
        if (existingBan != null) {
            throw new BusinessException("该用户已被禁言");
        }
        
        // 创建禁言记录
        ChatUserBan ban = new ChatUserBan();
        ban.setUserId(request.getUserId());
        ban.setRoomId(roomId);
        ban.setBanReason(request.getBanReason());
        ban.setBanStartTime(new Date());
        ban.setOperatorId(operatorId);
        ban.setStatus(1);
        
        // 计算禁言结束时间
        if (request.getBanDuration() > 0) {
            long endTime = System.currentTimeMillis() + request.getBanDuration() * 1000L;
            ban.setBanEndTime(new Date(endTime));
        } else {
            // 0表示永久禁言
            ban.setBanEndTime(null);
        }
        
        int result = chatUserBanMapper.insert(ban);
        if (result <= 0) {
            throw new BusinessException("禁言失败");
        }
        
        log.info("用户禁言成功，用户ID: {}, 禁言时长: {}秒, 原因: {}", 
            request.getUserId(), request.getBanDuration(), request.getBanReason());
    }
    
    @Override
    public void unbanUser(Long userId) {
        Long roomId = chatRoomService.getOfficialRoom().getId();
        ChatUserBan ban = chatUserBanMapper.selectActiveByUserId(userId, roomId);
        
        if (ban == null) {
            throw new BusinessException("该用户未被禁言");
        }
        
        int result = chatUserBanMapper.unban(ban.getId());
        if (result <= 0) {
            throw new BusinessException("解除禁言失败");
        }
        
        log.info("解除禁言成功，用户ID: {}", userId);
    }

    @Override
    public ChatUserBan getActiveBan(Long userId) {
        Long roomId = chatRoomService.getOfficialRoom().getId();
        return chatUserBanMapper.selectActiveByUserId(userId, roomId);
    }
    
    @Override
    public boolean isUserBanned(Long userId, Long roomId) {
        ChatUserBan ban = chatUserBanMapper.selectActiveByUserId(userId, roomId);
        return ban != null;
    }
    
    @Override
    public void autoUnbanExpired() {
        List<ChatUserBan> expiredBans = chatUserBanMapper.selectExpiredBans();
        
        if (expiredBans.isEmpty()) {
            return;
        }
        
        List<Long> ids = expiredBans.stream()
            .map(ChatUserBan::getId)
            .collect(Collectors.toList());
        
        int result = chatUserBanMapper.batchUnban(ids);
        log.info("自动解除过期禁言，数量: {}", result);
    }
}
