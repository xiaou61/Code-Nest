package com.xiaou.chat.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.xiaou.chat.domain.ChatMessage;
import com.xiaou.chat.dto.*;
import com.xiaou.chat.mapper.ChatMessageMapper;
import com.xiaou.chat.service.ChatMessageService;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天消息Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    
    private static final int MESSAGE_TYPE_TEXT = 1;
    private static final int MESSAGE_TYPE_IMAGE = 2;
    private static final int RECALL_TIME_LIMIT = 120; // 撤回时间限制：2分钟

    private final ChatMessageMapper chatMessageMapper;
    private final ChatRoomService chatRoomService;
    private final ChatUserBanService chatUserBanService;

    @Value("${xiaou.chat.message.max-text-length:1000}")
    private int maxTextLength;

    @Value("${xiaou.chat.message.max-image-url-length:1024}")
    private int maxImageUrlLength;
    
    @Override
    public ChatMessage sendMessage(ChatMessageRequest request, Long userId, String ipAddress, String deviceInfo) {
        validateMessageRequest(request);

        // 获取官方聊天室
        Long roomId = chatRoomService.getOfficialRoom().getId();
        
        // 检查用户是否被禁言
        if (chatUserBanService.isUserBanned(userId, roomId)) {
            throw new BusinessException("您已被禁言，无法发送消息");
        }
        
        // 创建消息记录
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setUserId(userId);
        int messageType = request.getMessageType() == null ? MESSAGE_TYPE_TEXT : request.getMessageType();
        message.setMessageType(messageType);
        message.setContent(normalizeContent(request, messageType));
        message.setImageUrl(normalizeImageUrl(request, messageType));
        message.setIsDeleted(0);
        message.setIpAddress(ipAddress);
        message.setDeviceInfo(deviceInfo);
        
        // 处理回复消息
        if (request.getReplyToId() != null && request.getReplyToId() > 0) {
            ChatMessage replyToMsg = chatMessageMapper.selectById(request.getReplyToId());
            if (replyToMsg != null) {
                message.setReplyToId(request.getReplyToId());
                message.setReplyToUser(replyToMsg.getUserNickname());
                // 截取回复内容摘要，最多50字
                String content = replyToMsg.getContent();
                if (content != null && content.length() > 50) {
                    content = content.substring(0, 50) + "...";
                }
                message.setReplyToContent(replyToMsg.getMessageType() == 2 ? "[图片]" : content);
            }
        }
        
        int result = chatMessageMapper.insert(message);
        if (result <= 0) {
            throw new BusinessException("发送消息失败");
        }
        
        log.info("用户发送消息，用户ID: {}, 消息ID: {}, 类型: {}", userId, message.getId(), request.getMessageType());
        
        // 查询完整消息信息（包含用户信息）
        return chatMessageMapper.selectById(message.getId());
    }

    private void validateMessageRequest(ChatMessageRequest request) {
        if (request == null) {
            throw new BusinessException("消息不能为空");
        }

        int messageType = request.getMessageType() == null ? MESSAGE_TYPE_TEXT : request.getMessageType();
        if (messageType != MESSAGE_TYPE_TEXT && messageType != MESSAGE_TYPE_IMAGE) {
            throw new BusinessException("不支持的消息类型");
        }

        if (messageType == MESSAGE_TYPE_TEXT) {
            String content = request.getContent();
            if (content == null || content.isBlank()) {
                throw new BusinessException("消息内容不能为空");
            }
            if (content.length() > maxTextLength) {
                throw new BusinessException("消息内容过长，最多" + maxTextLength + "个字符");
            }
            return;
        }

        String imageUrl = request.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException("图片消息地址不能为空");
        }
        if (imageUrl.length() > maxImageUrlLength) {
            throw new BusinessException("图片地址过长");
        }
        if (!isAllowedImageUrl(imageUrl)) {
            throw new BusinessException("图片地址格式不合法");
        }
        if (request.getContent() != null && request.getContent().length() > maxTextLength) {
            throw new BusinessException("图片说明过长，最多" + maxTextLength + "个字符");
        }
    }

    private String normalizeContent(ChatMessageRequest request, int messageType) {
        if (messageType == MESSAGE_TYPE_IMAGE) {
            String content = request.getContent();
            return content == null || content.isBlank() ? "[图片]" : content.trim();
        }
        return request.getContent().trim();
    }

    private String normalizeImageUrl(ChatMessageRequest request, int messageType) {
        if (messageType != MESSAGE_TYPE_IMAGE) {
            return null;
        }
        return request.getImageUrl().trim();
    }

    private boolean isAllowedImageUrl(String imageUrl) {
        String normalized = imageUrl.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://")
            || normalized.startsWith("https://")
            || normalized.startsWith("/api/files/")
            || normalized.startsWith("api/files/");
    }
    
    @Override
    public ChatHistoryResponse getHistory(ChatHistoryRequest request) {
        Long roomId = chatRoomService.getOfficialRoom().getId();
        
        List<ChatMessage> messages = chatMessageMapper.selectHistory(
            roomId, 
            request.getLastMessageId(), 
            request.getPageSize()
        );
        
        // 反转列表，使旧消息在前
        Collections.reverse(messages);
        
        // 转换为响应DTO
        List<ChatMessageResponse> messageResponses = messages.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        ChatHistoryResponse response = new ChatHistoryResponse();
        response.setMessages(messageResponses);
        response.setHasMore(messages.size() >= request.getPageSize());
        
        return response;
    }
    
    @Override
    public void recallMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageMapper.selectById(messageId);
        
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        
        if (!message.getUserId().equals(userId)) {
            throw new BusinessException("只能撤回自己的消息");
        }
        
        // 检查是否超过撤回时间限制
        long seconds = (System.currentTimeMillis() - message.getCreateTime().getTime()) / 1000;
        if (seconds > RECALL_TIME_LIMIT) {
            throw new BusinessException("消息发送超过2分钟，无法撤回");
        }
        
        int result = chatMessageMapper.deleteById(messageId);
        if (result <= 0) {
            throw new BusinessException("撤回消息失败");
        }
        
        log.info("用户撤回消息，用户ID: {}, 消息ID: {}", userId, messageId);
    }
    
    @Override
    public void deleteMessage(Long messageId) {
        int result = chatMessageMapper.deleteById(messageId);
        if (result <= 0) {
            throw new BusinessException("删除消息失败");
        }
        
        log.info("管理员删除消息，消息ID: {}", messageId);
    }
    
    @Override
    public void batchDeleteMessages(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的消息");
        }
        
        int result = chatMessageMapper.deleteBatch(ids);
        log.info("批量删除消息，数量: {}", result);
    }
    
    @Override
    public PageResult<ChatMessage> getAdminMessageList(AdminChatMessageListRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> 
            chatMessageMapper.selectAdminMessageList(request)
        );
    }
    
    @Override
    public void sendAnnouncement(String content) {
        Long roomId = chatRoomService.getOfficialRoom().getId();
        
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setUserId(0L); // 系统消息用户ID为0
        message.setMessageType(3); // 类型3：系统消息
        message.setContent(content);
        message.setIsDeleted(0);
        
        int result = chatMessageMapper.insert(message);
        if (result <= 0) {
            throw new BusinessException("发送系统公告失败");
        }
        
        log.info("发送系统公告，消息ID: {}", message.getId());
    }
    
    /**
     * 转换为响应DTO
     */
    private ChatMessageResponse convertToResponse(ChatMessage message) {
        ChatMessageResponse response = BeanUtil.copyProperties(message, ChatMessageResponse.class);
        
        // 判断是否可撤回（2分钟内）
        long seconds = (System.currentTimeMillis() - message.getCreateTime().getTime()) / 1000;
        response.setCanRecall(seconds <= RECALL_TIME_LIMIT);
        
        return response;
    }
}
