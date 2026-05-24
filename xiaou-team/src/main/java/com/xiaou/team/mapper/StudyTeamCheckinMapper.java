package com.xiaou.team.mapper;

import com.xiaou.team.domain.StudyTeamCheckin;
import com.xiaou.team.dto.CheckinResponse;
import com.xiaou.team.dto.DateValueStat;
import com.xiaou.team.dto.TeamValueStat;
import com.xiaou.team.dto.UserValueStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 打卡记录Mapper
 * 
 * @author xiaou
 */
@Mapper
public interface StudyTeamCheckinMapper {
    
    /**
     * 插入打卡记录
     */
    int insert(StudyTeamCheckin checkin);
    
    /**
     * 更新打卡记录
     */
    int update(StudyTeamCheckin checkin);
    
    /**
     * 根据ID查询打卡记录
     */
    StudyTeamCheckin selectById(@Param("id") Long id);
    
    /**
     * 删除打卡记录
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新点赞数
     */
    int updateLikeCount(@Param("id") Long id, @Param("delta") Integer delta);
    
    /**
     * 查询小组打卡动态列表
     * 
     * @param teamId 小组ID
     * @param taskId 任务ID（可选）
     * @param limit 条数
     * @param offset 偏移
     * @return 打卡列表
     */
    List<CheckinResponse> selectCheckinList(@Param("teamId") Long teamId,
                                            @Param("taskId") Long taskId,
                                            @Param("limit") Integer limit,
                                            @Param("offset") Integer offset);
    
    /**
     * 查询用户打卡记录
     * 
     * @param userId 用户ID
     * @param teamId 小组ID（可选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 打卡列表
     */
    List<CheckinResponse> selectUserCheckins(@Param("userId") Long userId,
                                             @Param("teamId") Long teamId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
    
    /**
     * 根据ID查询打卡详情
     * 
     * @param checkinId 打卡ID
     * @return 打卡详情
     */
    CheckinResponse selectCheckinById(@Param("checkinId") Long checkinId);
    
    /**
     * 统计用户连续打卡天数
     * 
     * @param userId 用户ID
     * @param teamId 小组ID
     * @param taskId 任务ID
     * @param today 今日日期
     * @return 连续天数
     */
    Integer countStreakDays(@Param("userId") Long userId,
                            @Param("teamId") Long teamId,
                            @Param("taskId") Long taskId,
                            @Param("today") LocalDate today);

    /**
     * 统计用户最长连续打卡天数
     */
    Integer countMaxStreakDays(@Param("userId") Long userId,
                               @Param("teamId") Long teamId,
                               @Param("taskId") Long taskId);
    
    /**
     * 统计用户在小组的总打卡天数
     * 
     * @param userId 用户ID
     * @param teamId 小组ID
     * @return 总打卡天数
     */
    Integer countUserTotalCheckinDays(@Param("userId") Long userId,
                                      @Param("teamId") Long teamId);
    
    /**
     * 查询某日打卡的用户列表
     * 
     * @param teamId 小组ID
     * @param taskId 任务ID
     * @param date 日期
     * @return 用户ID列表
     */
    List<Long> selectCheckinUserIds(@Param("teamId") Long teamId,
                                    @Param("taskId") Long taskId,
                                    @Param("date") LocalDate date);

    /**
     * 统计日期范围内每个用户的打卡次数
     */
    List<UserValueStat> countUserCheckinsByDateRange(@Param("teamId") Long teamId,
                                                     @Param("userIds") Collection<Long> userIds,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    /**
     * 统计日期范围内每个用户的学习时长
     */
    List<UserValueStat> sumUserDurationByDateRange(@Param("teamId") Long teamId,
                                                   @Param("userIds") Collection<Long> userIds,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /**
     * 统计日期范围内每天的总学习时长
     */
    List<DateValueStat> sumDailyDurationByDateRange(@Param("teamId") Long teamId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * 统计近7天每个小组的打卡人数总和
     */
    List<TeamValueStat> countRecentCheckinUsersByTeamIds(@Param("teamIds") Collection<Long> teamIds,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);
    
    /**
     * 检查用户是否点赞
     * 
     * @param checkinId 打卡ID
     * @param userId 用户ID
     * @return 点赞数
     */
    Integer checkUserLiked(@Param("checkinId") Long checkinId,
                           @Param("userId") Long userId);
    
    /**
     * 查询用户今日在某任务的打卡记录
     * 
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param today 今日日期
     * @return 打卡记录
     */
    StudyTeamCheckin selectUserTodayCheckin(@Param("userId") Long userId,
                                            @Param("taskId") Long taskId,
                                            @Param("today") LocalDate today);
}
