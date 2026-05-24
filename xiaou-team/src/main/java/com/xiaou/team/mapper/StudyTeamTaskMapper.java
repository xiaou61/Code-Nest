package com.xiaou.team.mapper;

import com.xiaou.team.domain.StudyTeamTask;
import com.xiaou.team.dto.TaskResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 打卡任务Mapper
 * 
 * @author xiaou
 */
@Mapper
public interface StudyTeamTaskMapper {
    
    /**
     * 插入任务
     */
    int insert(StudyTeamTask task);
    
    /**
     * 更新任务
     */
    int update(StudyTeamTask task);
    
    /**
     * 根据ID查询任务
     */
    StudyTeamTask selectById(@Param("id") Long id);
    
    /**
     * 删除任务
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新任务状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 查询小组任务列表
     * 
     * @param teamId 小组ID
     * @param status 状态（可选）
     * @return 任务列表
     */
    List<TaskResponse> selectTaskList(@Param("teamId") Long teamId, 
                                      @Param("status") Integer status);
    
    /**
     * 根据ID查询任务详情
     * 
     * @param taskId 任务ID
     * @return 任务详情
     */
    TaskResponse selectTaskById(@Param("taskId") Long taskId);
    
    /**
     * 查询今日需要打卡的任务
     * 
     * @param teamId 小组ID
     * @param today 今日日期
     * @param dayOfWeek 星期几（1-7）
     * @return 任务列表
     */
    List<TaskResponse> selectTodayTasks(@Param("teamId") Long teamId,
                                        @Param("today") LocalDate today,
                                        @Param("dayOfWeek") Integer dayOfWeek);

    /**
     * 统计任务数量
     */
    Integer countTasks(@Param("teamId") Long teamId,
                       @Param("status") Integer status);
    
    /**
     * 统计今日已打卡人数
     * 
     * @param taskId 任务ID
     * @param today 今日日期
     * @return 打卡人数
     */
    Integer countTodayCheckins(@Param("taskId") Long taskId, 
                               @Param("today") LocalDate today);
    
    /**
     * 检查用户今日是否已打卡
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param today 今日日期
     * @return 打卡记录数
     */
    Integer checkUserTodayCheckin(@Param("taskId") Long taskId,
                                  @Param("userId") Long userId,
                                  @Param("today") LocalDate today);
}
