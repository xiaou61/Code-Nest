package com.xiaou.notification.mapper;

import com.xiaou.notification.domain.NotificationConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息配置 Mapper 接口
 */
@Mapper
public interface NotificationConfigMapper {

    /**
     * 插入消息配置
     */
    int insert(NotificationConfig config);

    /**
     * 根据用户ID和类型查询配置
     */
    NotificationConfig selectByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    /**
     * 查询用户所有消息配置
     */
    List<NotificationConfig> selectByUserId(@Param("userId") Long userId);

    /**
     * 更新消息配置
     */
    int updateByUserIdAndType(@Param("userId") Long userId,
                              @Param("type") String type,
                              @Param("isEnabled") Boolean isEnabled);

    /**
     * 批量插入默认配置
     */
    int batchInsert(@Param("list") List<NotificationConfig> configs);

    /**
     * 检查用户是否开启某类型消息接收
     */
    boolean isTypeEnabled(@Param("userId") Long userId, @Param("type") String type);
}
