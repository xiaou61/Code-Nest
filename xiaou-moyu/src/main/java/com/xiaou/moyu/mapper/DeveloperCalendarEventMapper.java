package com.xiaou.moyu.mapper;

import com.xiaou.moyu.domain.DeveloperCalendarEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 程序员日历事件数据访问层
 *
 * @author xiaou
 */
@Mapper
public interface DeveloperCalendarEventMapper {

    /**
     * 根据ID查询事件
     *
     * @param id 事件ID
     * @return 事件信息
     */
    DeveloperCalendarEvent selectById(@Param("id") Long id);

    /**
     * 根据日期查询事件列表
     *
     * @param eventDate 事件日期(MM-dd格式)
     * @return 事件列表
     */
    List<DeveloperCalendarEvent> selectByEventDate(@Param("eventDate") String eventDate);

    /**
     * 查询指定月份的所有事件
     *
     * @param month 月份(MM格式)
     * @return 事件列表
     */
    List<DeveloperCalendarEvent> selectByMonth(@Param("month") String month);

    /**
     * 根据事件类型查询事件列表
     *
     * @param eventType 事件类型
     * @param status 状态
     * @return 事件列表
     */
    List<DeveloperCalendarEvent> selectByEventType(@Param("eventType") Integer eventType, 
                                                   @Param("status") Integer status);

    /**
     * 查询所有启用的事件
     *
     * @return 事件列表
     */
    List<DeveloperCalendarEvent> selectAllEnabled();

    /**
     * 查询重要节日列表
     *
     * @return 重要节日列表
     */
    List<DeveloperCalendarEvent> selectMajorEvents();

    /**
     * 插入事件
     *
     * @param event 事件信息
     * @return 影响行数
     */
    int insert(DeveloperCalendarEvent event);

    /**
     * 根据ID更新事件
     *
     * @param event 事件信息
     * @return 影响行数
     */
    int updateById(DeveloperCalendarEvent event);

    /**
     * 根据ID更新事件状态
     *
     * @param id 事件ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 根据ID删除事件
     *
     * @param id 事件ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计事件总数
     *
     * @param status 状态
     * @return 事件总数
     */
    Long countByStatus(@Param("status") Integer status);

    /**
     * 批量查询事件
     *
     * @param ids 事件ID列表
     * @return 事件列表
     */
    List<DeveloperCalendarEvent> selectBatchIds(@Param("ids") List<Long> ids);

    /**
     * 批量删除事件
     *
     * @param ids 事件ID列表
     * @return 影响行数
     */
    int deleteBatchIds(@Param("ids") List<Long> ids);
}
