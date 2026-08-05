package com.xiaou.notification.mapper;

import com.xiaou.notification.domain.NotificationTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息模板 Mapper 接口
 */
@Mapper
public interface NotificationTemplateMapper {

    /**
     * 根据模板代码查询模板
     */
    NotificationTemplate selectByCode(@Param("code") String code);

    /**
     * 查询所有启用的模板
     */
    List<NotificationTemplate> selectEnabledTemplates();

    /**
     * 插入模板
     */
    int insert(NotificationTemplate template);

    /**
     * 更新模板
     */
    int update(NotificationTemplate template);

    /**
     * 根据ID删除模板
     */
    int deleteById(@Param("id") Long id);
}
