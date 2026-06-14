package com.xiaou.common.utils;

import com.xiaou.common.domain.Notification;
import com.xiaou.common.domain.NotificationTemplate;
import com.xiaou.common.enums.NotificationPriorityEnum;
import com.xiaou.common.enums.NotificationSourceEnum;
import com.xiaou.common.enums.NotificationStatusEnum;
import com.xiaou.common.enums.NotificationTypeEnum;
import com.xiaou.common.mapper.NotificationTemplateMapper;
import com.xiaou.common.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 消息通知工具类
 * 使用方式：NotificationUtil.sendXxx()
 */
@Component
public class NotificationUtil {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationUtil.class);
    
    private static NotificationService notificationService;
    private static NotificationTemplateMapper notificationTemplateMapper;
    private static NotificationCacheUtil notificationCacheUtil;
    
    @Autowired
    public void setNotificationService(NotificationService notificationService) {
        NotificationUtil.notificationService = notificationService;
    }
    
    @Autowired
    public void setNotificationTemplateMapper(NotificationTemplateMapper notificationTemplateMapper) {
        NotificationUtil.notificationTemplateMapper = notificationTemplateMapper;
    }
    
    @Autowired
    public void setNotificationCacheUtil(NotificationCacheUtil notificationCacheUtil) {
        NotificationUtil.notificationCacheUtil = notificationCacheUtil;
    }
    
    /**
     * 发送系统公告（全站广播）
     * @param title 公告标题
     * @param content 公告内容
     * @param priority 优先级：HIGH/MEDIUM/LOW
     */
    public static void sendAnnouncement(String title, String content, String priority) {
        // 验证优先级参数
        if (!NotificationPriorityEnum.isValidCode(priority)) {
            priority = NotificationPriorityEnum.LOW.getCode();
        }
        
        Notification notification = new Notification(
            title, content, NotificationTypeEnum.ANNOUNCEMENT.getCode(), 
            priority, 0L, null, NotificationStatusEnum.UNREAD.getCode()
        );
        notificationService.sendNotification(notification);
    }
    
    /**
     * 发送个人消息
     * @param receiverId 接收者ID
     * @param title 消息标题
     * @param content 消息内容
     */
    public static void sendPersonalMessage(Long receiverId, String title, String content) {
        sendPersonalMessage(receiverId, title, content, NotificationTypeEnum.PERSONAL.getCode());
    }
    
    /**
     * 发送个人消息（带类型）
     * @param receiverId 接收者ID
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     */
    public static void sendPersonalMessage(Long receiverId, String title, String content, String type) {
        // 防护：检查接收者ID是否为空
        if (receiverId == null) {
            log.warn("接收者ID为空，无法发送个人消息: title={}, content={}", title, content);
            return;
        }
        
        // 验证消息类型
        if (!NotificationTypeEnum.isValidCode(type)) {
            type = NotificationTypeEnum.PERSONAL.getCode();
        }
        
        // 检查用户是否开启该类型消息接收
        if (!isUserAcceptType(receiverId, type)) {
            return;
        }
        
        Notification notification = new Notification(
            title, content, type, NotificationPriorityEnum.LOW.getCode(), 
            0L, receiverId, NotificationStatusEnum.UNREAD.getCode()
        );
        boolean success = notificationService.sendNotification(notification);
        
        // 发送成功后清除用户的未读数量缓存
        if (success && receiverId != null) {
            try {
                notificationCacheUtil.clearUnreadCountCache(receiverId);
            } catch (Exception e) {
                // 缓存清除失败不影响主流程
            }
        }
    }
    
    /**
     * 批量发送消息
     * @param receiverIds 接收者ID列表
     * @param title 消息标题
     * @param content 消息内容
     */
    public static void sendBatchMessage(List<Long> receiverIds, String title, String content) {
        sendBatchMessage(receiverIds, title, content, NotificationTypeEnum.PERSONAL.getCode());
    }

    /**
     * 批量发送消息
     * @param receiverIds 接收者ID列表
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     */
    public static void sendBatchMessage(List<Long> receiverIds, String title, String content, String type) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }

        if (!NotificationTypeEnum.isValidCode(type)) {
            type = NotificationTypeEnum.PERSONAL.getCode();
        }
        
        List<Notification> notifications = new ArrayList<>();
        for (Long receiverId : receiverIds) {
            Notification notification = new Notification(
                title, content, type,
                NotificationPriorityEnum.LOW.getCode(), 0L, receiverId, 
                NotificationStatusEnum.UNREAD.getCode()
            );
            notifications.add(notification);
        }
        
        notificationService.sendBatchNotifications(notifications);
    }
    
    /**
     * 发送模板消息
     * @param receiverId 接收者ID
     * @param templateCode 模板代码
     * @param params 模板参数
     */
    public static void sendTemplateMessage(Long receiverId, String templateCode, Map<String, Object> params) {
        try {
            // 从数据库获取模板
            NotificationTemplate template = notificationTemplateMapper.selectByCode(templateCode);
            if (template == null) {
                // 如果数据库中没有模板，使用硬编码的备用模板
                String title = processTemplate(getTemplateTitle(templateCode), params);
                String content = processTemplate(getTemplateContent(templateCode), params);
                sendPersonalMessage(receiverId, title, content, NotificationTypeEnum.TEMPLATE.getCode());
                return;
            }
            
            // 处理模板变量替换
            String title = processTemplate(template.getTitleTemplate(), params);
            String content = processTemplate(template.getContentTemplate(), params);
            
            sendPersonalMessage(receiverId, title, content, NotificationTypeEnum.TEMPLATE.getCode());
        } catch (Exception e) {
            // 异常时使用硬编码模板作为降级方案
            String title = processTemplate(getTemplateTitle(templateCode), params);
            String content = processTemplate(getTemplateContent(templateCode), params);
            sendPersonalMessage(receiverId, title, content, NotificationTypeEnum.TEMPLATE.getCode());
        }
    }
    
    /**
     * 异步发送消息（推荐使用）
     * @param receiverId 接收者ID
     * @param title 消息标题
     * @param content 消息内容
     */
    public static void sendMessageAsync(Long receiverId, String title, String content) {
        Notification notification = new Notification(
            title, content, NotificationTypeEnum.PERSONAL.getCode(), 
            NotificationPriorityEnum.LOW.getCode(), 0L, receiverId, 
            NotificationStatusEnum.UNREAD.getCode()
        );
        notificationService.sendNotificationAsync(notification);
    }
    
    /**
     * 异步发送消息（带类型）
     * @param receiverId 接收者ID
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     */
    public static void sendMessageAsync(Long receiverId, String title, String content, String type) {
        // 验证消息类型
        if (!NotificationTypeEnum.isValidCode(type)) {
            type = NotificationTypeEnum.PERSONAL.getCode();
        }
        
        Notification notification = new Notification(
            title, content, type, NotificationPriorityEnum.LOW.getCode(), 
            0L, receiverId, NotificationStatusEnum.UNREAD.getCode()
        );
        notificationService.sendNotificationAsync(notification);
    }
    
    /**
     * 获取用户未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    public static int getUnreadCount(Long userId) {
        try {
            // 先尝试从Redis缓存获取
            Integer cachedCount = notificationCacheUtil.getCachedUnreadCount(userId);
            if (cachedCount != null) {
                return cachedCount;
            }
            
            // 缓存未命中，从数据库查询
            int count = notificationService.getUnreadCount(userId);
            
            // 将结果缓存到Redis
            notificationCacheUtil.cacheUnreadCount(userId, count);
            
            return count;
        } catch (Exception e) {
            // 异常时直接查询数据库，确保功能可用
            return notificationService.getUnreadCount(userId);
        }
    }
    
    /**
     * 发送社区互动消息
     * @param receiverId 接收者ID
     * @param title 消息标题
     * @param content 消息内容
     */
    public static void sendCommunityMessage(Long receiverId, String title, String content) {
        sendCommunityMessage(receiverId, title, content, null);
    }
    
    /**
     * 发送社区互动消息（带来源信息）
     * @param receiverId 接收者ID
     * @param title 消息标题
     * @param content 消息内容
     * @param sourceId 来源数据ID（如帖子ID）
     */
    public static void sendCommunityMessage(Long receiverId, String title, String content, String sourceId) {
        // 防护：检查接收者ID是否为空
        if (receiverId == null) {
            log.warn("接收者ID为空，无法发送社区消息: title={}, content={}", title, content);
            return;
        }
        
        // 检查用户是否开启该类型消息接收
        if (!isUserAcceptType(receiverId, NotificationTypeEnum.COMMUNITY_INTERACTION.getCode())) {
            return;
        }
        
        Notification notification = new Notification(
            title, content, NotificationTypeEnum.COMMUNITY_INTERACTION.getCode(), 
            NotificationPriorityEnum.LOW.getCode(), 0L, receiverId, 
            NotificationStatusEnum.UNREAD.getCode()
        );
        notification.setSourceModule(NotificationSourceEnum.COMMUNITY.getCode());
        notification.setSourceId(sourceId);
        notificationService.sendNotification(notification);
    }
    
    /**
     * 发送面试题相关消息
     * @param receiverId 接收者ID
     * @param title 消息标题
     * @param content 消息内容
     * @param sourceId 来源数据ID（如题目ID）
     */
    public static void sendInterviewMessage(Long receiverId, String title, String content, String sourceId) {
        // 防护：检查接收者ID是否为空
        if (receiverId == null) {
            log.warn("接收者ID为空，无法发送面试题消息: title={}, content={}", title, content);
            return;
        }
        
        // 检查用户是否开启该类型消息接收
        if (!isUserAcceptType(receiverId, NotificationTypeEnum.INTERVIEW_REMINDER.getCode())) {
            return;
        }
        
        Notification notification = new Notification(
            title, content, NotificationTypeEnum.INTERVIEW_REMINDER.getCode(), 
            NotificationPriorityEnum.LOW.getCode(), 0L, receiverId, 
            NotificationStatusEnum.UNREAD.getCode()
        );
        notification.setSourceModule(NotificationSourceEnum.INTERVIEW.getCode());
        notification.setSourceId(sourceId);
        notificationService.sendNotification(notification);
    }
    
    /**
     * 发送系统通知消息
     * @param receiverId 接收者ID
     * @param title 消息标题
     * @param content 消息内容
     */
    public static void sendSystemMessage(Long receiverId, String title, String content) {
        sendPersonalMessage(receiverId, title, content, NotificationTypeEnum.SYSTEM.getCode());
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 检查用户是否开启该类型消息接收
     * 现已强制所有用户接受所有类型的消息
     */
    private static boolean isUserAcceptType(Long userId, String type) {
        // 强制所有用户接受所有类型的消息
        return true;
    }
    
    /**
     * 处理模板变量替换
     */
    private static String processTemplate(String template, Map<String, Object> params) {
        if (template == null || params == null) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
    
    /**
     * 获取模板标题（简化实现）
     */
    private static String getTemplateTitle(String templateCode) {
        switch (templateCode) {
            case "WELCOME":
                return "欢迎加入{platform}";
            case "COMMUNITY_LIKE":
                return "您的帖子收到点赞";
            case "COMMUNITY_COMMENT":
                return "您的帖子收到评论";
            case "INTERVIEW_FAVORITE":
                return "收藏提醒";
            case "SYSTEM_MAINTENANCE":
                return "系统维护通知";
            default:
                return "系统消息";
        }
    }
    
    /**
     * 获取模板内容（简化实现）
     */
    private static String getTemplateContent(String templateCode) {
        switch (templateCode) {
            case "WELCOME":
                return "亲爱的{username}，欢迎加入我们的平台！";
            case "COMMUNITY_LIKE":
                return "您的帖子《{postTitle}》收到了{likerName}的点赞";
            case "COMMUNITY_COMMENT":
                return "您的帖子《{postTitle}》收到了{commenterName}的评论";
            case "INTERVIEW_FAVORITE":
                return "您收藏的面试题《{questionTitle}》已更新";
            case "SYSTEM_MAINTENANCE":
                return "系统将于{maintenanceTime}进行维护，预计耗时{duration}";
            default:
                return "您有新的系统消息，请注意查收";
        }
    }
} 
