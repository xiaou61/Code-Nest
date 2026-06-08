package com.xiaou.common.mapper;

import com.xiaou.common.domain.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息通知 Mapper 接口
 */
@Mapper
public interface NotificationMapper {
    
    /**
     * 插入单个消息
     */
    int insert(Notification notification);
    
    /**
     * 批量插入消息
     */
    int batchInsert(@Param("list") List<Notification> notifications);
    
    /**
     * 统计未读消息数量
     */
    int countUnread(@Param("userId") Long userId);
    
    /**
     * 分页查询用户消息
     */
    List<Notification> selectByUserId(@Param("userId") Long userId, 
                                      @Param("status") String status,
                                      @Param("type") String type,
                                      @Param("title") String title,
                                      @Param("startTime") String startTime,
                                      @Param("endTime") String endTime);
    
    /**
     * 根据ID查询消息
     */
    Notification selectById(@Param("id") Long id);
    
    /**
     * 标记消息已读
     */
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);
    
    /**
     * 批量标记已读
     */
    int batchMarkAsRead(@Param("ids") List<Long> ids, @Param("userId") Long userId);
    
    /**
     * 删除消息（软删除）
     */
    int deleteMessage(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 管理员删除消息（软删除）
     */
    int deleteMessageById(@Param("id") Long id);
    
    /**
     * 统计用户消息总数
     */
    int countByUserId(@Param("userId") Long userId, 
                      @Param("status") String status,
                      @Param("type") String type);
    
    /**
     * 统计今日发送消息数量
     */
    Long countTodayMessages();
    
    /**
     * 统计本月发送消息数量
     */
    Long countMonthMessages();
    
    /**
     * 统计全部未读消息数量
     */
    Long countAllUnreadMessages();
    
    /**
     * 按类型统计消息数量
     */
    Long countByType(@Param("type") String type);
    
    /**
     * 按时间范围和类型统计消息数量
     */
    Long countByTimeRangeAndType(@Param("startTime") String startTime,
                                 @Param("endTime") String endTime,
                                 @Param("type") String type);
    
    /**
     * 统计用户真实未读消息数量（考虑阅读记录表）
     */
    int countUnreadWithReadRecord(@Param("userId") Long userId);
    
    /**
     * 查询用户消息列表（考虑阅读记录表）
     */
    List<Notification> selectByUserIdWithReadRecord(@Param("userId") Long userId, 
                                                   @Param("status") String status,
                                                   @Param("type") String type,
                                                   @Param("title") String title,
                                                   @Param("startTime") String startTime,
                                                   @Param("endTime") String endTime);
}
