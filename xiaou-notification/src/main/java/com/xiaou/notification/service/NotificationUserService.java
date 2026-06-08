package com.xiaou.notification.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.domain.Notification;
import com.xiaou.common.enums.NotificationStatusEnum;
import com.xiaou.common.mapper.NotificationMapper;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.common.service.NotificationService;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.notification.dto.DeleteMessageRequest;
import com.xiaou.notification.dto.MarkReadRequest;
import com.xiaou.notification.dto.NotificationQueryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户端消息通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationUserService {
    
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    
    /**
     * 获取用户消息列表
     */
    public PageResult<Notification> getMessageList(NotificationQueryRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        
        // 使用PageHelper分页插件
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            // 直接调用Mapper，让PageHelper拦截SQL，使用新的支持阅读记录的方法
            return notificationMapper.selectByUserIdWithReadRecord(
                userId,
                request.getStatus(),
                request.getType(),
                request.getTitle(),
                request.getStartTime(),
                request.getEndTime()
            );
        });
    }
    
    /**
     * 获取用户未读消息数量
     */
    public int getUnreadCount() {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return notificationService.getUnreadCount(userId);
    }
    
    /**
     * 获取消息详情
     */
    public Notification getMessageDetail(Long messageId) {
        Notification notification = notificationService.getMessageById(messageId);
        if (notification != null) {
            Long userId = StpUserUtil.getLoginIdAsLong();
            // 检查权限：只能查看自己的消息或公告
            if (notification.getReceiverId() == null || notification.getReceiverId().equals(userId)) {
                // 如果是未读消息，自动标记为已读
                if (NotificationStatusEnum.UNREAD.getCode().equals(notification.getStatus())) {
                    notificationService.markAsRead(messageId, userId);
                }
                return notification;
            }
        }
        return null;
    }
    
    /**
     * 标记消息已读
     */
    public boolean markAsRead(MarkReadRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        
        if (Boolean.TRUE.equals(request.getMarkAllRead())) {
            // 标记全部已读
            return markAllAsRead();
        } else if (request.getMessageIds() != null && !request.getMessageIds().isEmpty()) {
            // 批量标记已读
            return notificationService.batchMarkAsRead(request.getMessageIds(), userId);
        } else if (request.getMessageId() != null) {
            // 单个标记已读
            return notificationService.markAsRead(request.getMessageId(), userId);
        }
        
        return false;
    }
    
    /**
     * 全部标记已读
     */
    public boolean markAllAsRead() {
        Long userId = StpUserUtil.getLoginIdAsLong();
        // 这里可以实现更高效的全部标记已读逻辑
        // 简化实现：获取所有未读消息ID后批量标记
        List<Notification> unreadMessages = notificationService.getUserMessages(
            userId, NotificationStatusEnum.UNREAD.getCode(), null // 限制1000条以免性能问题
        );
        
        if (unreadMessages != null && !unreadMessages.isEmpty()) {
            List<Long> messageIds = unreadMessages.stream()
                .map(Notification::getId)
                .toList();
            return notificationService.batchMarkAsRead(messageIds, userId);
        }
        
        return true;
    }
    
    /**
     * 删除消息
     */
    public boolean deleteMessage(DeleteMessageRequest request) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        return notificationService.deleteMessage(request.getMessageId(), userId);
    }
} 
