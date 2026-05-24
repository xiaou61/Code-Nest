package com.xiaou.team.service.impl;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.team.domain.StudyTeam;
import com.xiaou.team.domain.StudyTeamMember;
import com.xiaou.team.dto.DateValueStat;
import com.xiaou.team.dto.RankResponse;
import com.xiaou.team.dto.TeamStatsResponse;
import com.xiaou.team.mapper.*;
import com.xiaou.team.service.TeamRankService;
import com.xiaou.team.service.TeamStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务实现
 * 
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class TeamStatsServiceImpl implements TeamStatsService {
    
    private final StudyTeamMapper teamMapper;
    private final StudyTeamMemberMapper memberMapper;
    private final StudyTeamTaskMapper taskMapper;
    private final StudyTeamCheckinMapper checkinMapper;
    private final StudyTeamDiscussionMapper discussionMapper;
    private final StudyTeamDailyStatsMapper dailyStatsMapper;
    private final TeamRankService rankService;
    
    @Override
    public TeamStatsResponse getTeamStats(Long teamId, Long userId) {
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        
        TeamStatsResponse stats = new TeamStatsResponse();
        stats.setTeamId(teamId);
        stats.setTeamName(team.getTeamName());
        
        // 成员数
        Integer memberCount = memberMapper.selectCount(teamId).intValue();
        stats.setMemberCount(memberCount);
        
        // 今日打卡人数
        LocalDate today = LocalDate.now();
        Integer todayCheckins = dailyStatsMapper.countCheckinsByDate(teamId, today);
        stats.setTodayCheckinCount(todayCheckins != null ? todayCheckins : 0);
        
        // 今日打卡率
        if (memberCount > 0) {
            stats.setTodayCheckinRate((double) stats.getTodayCheckinCount() / memberCount * 100);
        } else {
            stats.setTodayCheckinRate(0.0);
        }
        
        // 累计打卡次数
        Integer totalCheckins = dailyStatsMapper.countTotalCheckins(teamId);
        stats.setTotalCheckins(totalCheckins != null ? totalCheckins : 0);
        
        // 累计打卡天数
        Integer totalDays = dailyStatsMapper.countTotalCheckinDays(teamId);
        stats.setTotalCheckinDays(totalDays != null ? totalDays : 0);
        
        // 讨论数量
        Integer discussionCount = discussionMapper.countDiscussions(teamId, null);
        stats.setDiscussionCount(discussionCount != null ? discussionCount : 0);
        
        // 任务数量
        Integer taskCount = taskMapper.countTasks(teamId, null);
        stats.setTaskCount(taskCount != null ? taskCount : 0);
        
        // 活跃任务数
        Integer activeTaskCount = taskMapper.countTasks(teamId, 1);
        stats.setActiveTaskCount(activeTaskCount != null ? activeTaskCount : 0);
        
        // 本周统计
        stats.setWeeklyStats(getWeeklyDailyStats(teamId, memberCount));
        
        // 用户个人统计
        if (userId != null) {
            stats.setUserStats(getUserStats(teamId, userId));
        }
        
        return stats;
    }
    
    @Override
    public TeamStatsResponse getWeeklyStats(Long teamId) {
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        
        TeamStatsResponse stats = new TeamStatsResponse();
        stats.setTeamId(teamId);
        stats.setTeamName(team.getTeamName());
        
        Integer memberCount = memberMapper.selectCount(teamId).intValue();
        stats.setMemberCount(memberCount);
        
        stats.setWeeklyStats(getWeeklyDailyStats(teamId, memberCount));
        
        return stats;
    }
    
    @Override
    public TeamStatsResponse getMonthlyStats(Long teamId, Integer year, Integer month) {
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        
        TeamStatsResponse stats = new TeamStatsResponse();
        stats.setTeamId(teamId);
        stats.setTeamName(team.getTeamName());
        
        Integer memberCount = memberMapper.selectCount(teamId).intValue();
        stats.setMemberCount(memberCount);
        
        stats.setMonthlyStats(getMonthlyDailyStats(teamId, year, month, memberCount));
        
        return stats;
    }
    
    @Override
    public TeamStatsResponse.UserStats getUserStats(Long teamId, Long userId) {
        StudyTeamMember member = memberMapper.selectByTeamIdAndUserId(teamId, userId);
        
        if (member == null) {
            return null;
        }
        
        TeamStatsResponse.UserStats userStats = new TeamStatsResponse.UserStats();
        userStats.setUserId(userId);
        userStats.setTotalCheckins(member.getTotalCheckins() != null ? member.getTotalCheckins() : 0);
        userStats.setContribution(member.getContribution() != null ? member.getContribution() : 0);
        
        // 连续打卡天数
        LocalDate today = LocalDate.now();
        Integer streakDays = checkinMapper.countStreakDays(userId, teamId, null, today);
        userStats.setStreakDays(streakDays != null ? streakDays : 0);
        
        // 获取排名
        RankResponse rank = rankService.getUserRank(teamId, userId, "checkin");
        if (rank != null) {
            userStats.setRank(rank.getRank());
        }
        
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);
        userStats.setWeeklyCheckins(countUserCheckins(teamId, userId, weekStart, today));
        userStats.setMonthlyCheckins(countUserCheckins(teamId, userId, monthStart, today));
        userStats.setTotalDuration(sumUserDuration(teamId, userId, null, null));
        
        return userStats;
    }
    
    /**
     * 获取本周每日统计
     */
    private List<TeamStatsResponse.DailyStats> getWeeklyDailyStats(Long teamId, Integer memberCount) {
        List<TeamStatsResponse.DailyStats> dailyStatsList = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, Integer> dailyDurationMap = toDateValueMap(checkinMapper.sumDailyDurationByDateRange(teamId, monday, today));
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            if (date.isAfter(today)) {
                break;
            }
            
            TeamStatsResponse.DailyStats dailyStats = new TeamStatsResponse.DailyStats();
            dailyStats.setDate(date);
            
            Integer checkinCount = dailyStatsMapper.countCheckinsByDate(teamId, date);
            dailyStats.setCheckinCount(checkinCount != null ? checkinCount : 0);
            
            if (memberCount > 0) {
                dailyStats.setCheckinRate((double) dailyStats.getCheckinCount() / memberCount * 100);
            } else {
                dailyStats.setCheckinRate(0.0);
            }
            
            dailyStats.setTotalDuration(dailyDurationMap.getOrDefault(date, 0));
            
            dailyStatsList.add(dailyStats);
        }
        
        return dailyStatsList;
    }
    
    /**
     * 获取月度每日统计
     */
    private List<TeamStatsResponse.DailyStats> getMonthlyDailyStats(Long teamId, Integer year, Integer month, Integer memberCount) {
        List<TeamStatsResponse.DailyStats> dailyStatsList = new ArrayList<>();
        
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();
        
        if (endDate.isAfter(today)) {
            endDate = today;
        }
        Map<LocalDate, Integer> dailyDurationMap = toDateValueMap(checkinMapper.sumDailyDurationByDateRange(teamId, startDate, endDate));
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            TeamStatsResponse.DailyStats dailyStats = new TeamStatsResponse.DailyStats();
            dailyStats.setDate(date);
            
            Integer checkinCount = dailyStatsMapper.countCheckinsByDate(teamId, date);
            dailyStats.setCheckinCount(checkinCount != null ? checkinCount : 0);
            
            if (memberCount > 0) {
                dailyStats.setCheckinRate((double) dailyStats.getCheckinCount() / memberCount * 100);
            } else {
                dailyStats.setCheckinRate(0.0);
            }
            
            dailyStats.setTotalDuration(dailyDurationMap.getOrDefault(date, 0));
            
            dailyStatsList.add(dailyStats);
        }
        
        return dailyStatsList;
    }

    private Integer countUserCheckins(Long teamId, Long userId, LocalDate startDate, LocalDate endDate) {
        return checkinMapper.countUserCheckinsByDateRange(teamId, Collections.singletonList(userId), startDate, endDate)
                .stream()
                .findFirst()
                .map(stat -> stat.getValue() != null ? stat.getValue() : 0)
                .orElse(0);
    }

    private Integer sumUserDuration(Long teamId, Long userId, LocalDate startDate, LocalDate endDate) {
        return checkinMapper.sumUserDurationByDateRange(teamId, Collections.singletonList(userId), startDate, endDate)
                .stream()
                .findFirst()
                .map(stat -> stat.getValue() != null ? stat.getValue() : 0)
                .orElse(0);
    }

    private Map<LocalDate, Integer> toDateValueMap(List<DateValueStat> stats) {
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<LocalDate, Integer> map = new HashMap<>();
        for (DateValueStat stat : stats) {
            if (stat.getStatDate() != null) {
                map.put(stat.getStatDate(), stat.getValue() != null ? stat.getValue() : 0);
            }
        }
        return map;
    }
}
