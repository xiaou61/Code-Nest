package com.xiaou.common.service;

import com.xiaou.common.domain.Notification;
import com.xiaou.common.domain.NotificationUserReadRecord;
import com.xiaou.common.mapper.NotificationMapper;
import com.xiaou.common.mapper.NotificationUserReadRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息通知内部服务类
 */
@Service
public class NotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    @Autowired
    private NotificationUserReadRecordMapper readRecordMapper;
    
    /**
     * 同步发送单个消息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean sendNotification(Notification notification) {
        try {
            int result = notificationMapper.insert(notification);
            return result > 0;
        } catch (Exception e) {
            log.error("发送消息失败: {}", notification, e);
            return false;
        }
    }
    
    /**
     * 异步发送消息
     */
    @Async("notificationExecutor")
    public void sendNotificationAsync(Notification notification) {
        sendNotification(notification);
    }
    
    /**
     * 批量发送消息
     */
    @Async("notificationExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void sendBatchNotifications(List<Notification> notifications) {
        try {
            if (notifications != null && !notifications.isEmpty()) {
                notificationMapper.batchInsert(notifications);
                log.info("批量发送消息成功，数量: {}", notifications.size());
            }
        } catch (Exception e) {
            log.error("批量发送消息失败, 数量: {}", notifications.size(), e);
        }
    }
    
    /**
     * 获取未读消息数量
     */
    public int getUnreadCount(Long userId) {
        try {
            return notificationMapper.countUnreadWithReadRecord(userId);
        } catch (Exception e) {
            log.error("获取未读消息数量失败, userId: {}", userId, e);
            return 0;
        }
    }
    
    /**
     * 分页查询用户消息
     */
    public List<Notification> getUserMessages(Long userId, String status, String type) {
        try {
            return notificationMapper.selectByUserIdWithReadRecord(userId, status, type, null, null, null);
        } catch (Exception e) {
            log.error("查询用户消息失败, userId: {}", userId, e);
            return null;
        }
    }
    
    /**
     * 统计用户消息总数
     */
    public int countUserMessages(Long userId, String status, String type) {
        try {
            return notificationMapper.countByUserId(userId, status, type);
        } catch (Exception e) {
            log.error("统计用户消息总数失败, userId: {}", userId, e);
            return 0;
        }
    }
    
    /**
     * 标记消息已读
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markAsRead(Long messageId, Long userId) {
        try {
            // 先获取消息信息
            Notification notification = notificationMapper.selectById(messageId);
            if (notification == null) {
                log.warn("消息不存在, messageId: {}", messageId);
                return false;
            }
            
            // 如果消息已经是已读状态，直接返回成功
            if ("READ".equals(notification.getStatus())) {
                return true;
            }
            
            // 如果是系统公告（receiverId为null），创建用户阅读记录
            if (notification.getReceiverId() == null) {
                // 检查是否已有阅读记录
                NotificationUserReadRecord existingRecord = readRecordMapper.selectByUserAndNotification(userId, messageId);
                if (existingRecord == null) {
                    NotificationUserReadRecord readRecord = new NotificationUserReadRecord();
                    readRecord.setUserId(userId);
                    readRecord.setNotificationId(messageId);
                    readRecord.setReadTime(LocalDateTime.now());
                    readRecord.setCreatedTime(LocalDateTime.now());
                    int result = readRecordMapper.insert(readRecord);
                    return result > 0;
                }
                return true; // 已经有阅读记录了
            } else {
                // 个人消息，直接更新status
                int result = notificationMapper.markAsRead(messageId, userId);
                return result > 0;
            }
        } catch (Exception e) {
            log.error("标记消息已读失败, messageId: {}, userId: {}", messageId, userId, e);
            return false;
        }
    }
    
    /**
     * 批量标记已读
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean batchMarkAsRead(List<Long> messageIds, Long userId) {
        try {
            if (messageIds != null && !messageIds.isEmpty()) {
                // 分别处理个人消息和系统公告
                List<Long> personalMessageIds = new java.util.ArrayList<>();
                List<NotificationUserReadRecord> readRecords = new java.util.ArrayList<>();
                
                for (Long messageId : messageIds) {
                    Notification notification = notificationMapper.selectById(messageId);
                    if (notification != null) {
                        if (notification.getReceiverId() == null) {
                            // 系统公告，检查是否已有阅读记录
                            NotificationUserReadRecord existingRecord = readRecordMapper.selectByUserAndNotification(userId, messageId);
                            if (existingRecord == null) {
                                NotificationUserReadRecord readRecord = new NotificationUserReadRecord();
                                readRecord.setUserId(userId);
                                readRecord.setNotificationId(messageId);
                                readRecord.setReadTime(LocalDateTime.now());
                                readRecord.setCreatedTime(LocalDateTime.now());
                                readRecords.add(readRecord);
                            }
                        } else if (notification.getReceiverId().equals(userId)) {
                            // 个人消息
                            personalMessageIds.add(messageId);
                        }
                    }
                }
                
                boolean success = true;
                
                // 批量处理个人消息
                if (!personalMessageIds.isEmpty()) {
                    int result = notificationMapper.batchMarkAsRead(personalMessageIds, userId);
                    success = success && result > 0;
                }
                
                // 批量插入系统公告阅读记录
                if (!readRecords.isEmpty()) {
                    int result = readRecordMapper.batchInsert(readRecords);
                    success = success && result > 0;
                }
                
                return success;
            }
            return false;
        } catch (Exception e) {
            log.error("批量标记已读失败, messageIds: {}, userId: {}", messageIds, userId, e);
            return false;
        }
    }
    
    /**
     * 删除消息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMessage(Long messageId, Long userId) {
        try {
            int result = notificationMapper.deleteMessage(messageId, userId);
            return result > 0;
        } catch (Exception e) {
            log.error("删除消息失败, messageId: {}, userId: {}", messageId, userId, e);
            return false;
        }
    }
    
    /**
     * 根据ID查询消息
     */
    public Notification getMessageById(Long messageId) {
        try {
            return notificationMapper.selectById(messageId);
        } catch (Exception e) {
            log.error("根据ID查询消息失败, messageId: {}", messageId, e);
            return null;
        }
    }
} 
