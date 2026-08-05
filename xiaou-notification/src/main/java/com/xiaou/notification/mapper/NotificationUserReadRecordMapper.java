package com.xiaou.notification.mapper;

import com.xiaou.notification.domain.NotificationUserReadRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户通知阅读记录 Mapper 接口
 */
@Mapper
public interface NotificationUserReadRecordMapper {

    /**
     * 插入阅读记录
     */
    int insert(NotificationUserReadRecord record);

    /**
     * 批量插入阅读记录
     */
    int batchInsert(@Param("list") List<NotificationUserReadRecord> records);

    /**
     * 检查用户是否已读指定通知
     */
    NotificationUserReadRecord selectByUserAndNotification(@Param("userId") Long userId,
                                                          @Param("notificationId") Long notificationId);

    /**
     * 获取用户已读的公告ID列表
     */
    List<Long> selectReadAnnouncementIds(@Param("userId") Long userId);

    /**
     * 删除阅读记录（当通知被删除时）
     */
    int deleteByNotificationId(@Param("notificationId") Long notificationId);

    /**
     * 删除用户的阅读记录（当用户被删除时）
     */
    int deleteByUserId(@Param("userId") Long userId);
}
