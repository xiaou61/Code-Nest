package com.xiaou.team.service.impl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.team.domain.StudyTeamCheckin;
import com.xiaou.team.domain.StudyTeamCheckinLike;
import com.xiaou.team.domain.StudyTeamDailyStats;
import com.xiaou.team.domain.StudyTeamMember;
import com.xiaou.team.domain.StudyTeamTask;
import com.xiaou.team.dto.CheckinRequest;
import com.xiaou.team.dto.CheckinResponse;
import com.xiaou.team.enums.MemberStatus;
import com.xiaou.team.mapper.StudyTeamCheckinLikeMapper;
import com.xiaou.team.mapper.StudyTeamCheckinMapper;
import com.xiaou.team.mapper.StudyTeamDailyStatsMapper;
import com.xiaou.team.mapper.StudyTeamMapper;
import com.xiaou.team.mapper.StudyTeamMemberMapper;
import com.xiaou.team.mapper.StudyTeamTaskMapper;
import com.xiaou.team.service.TeamCheckinService;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 打卡服务实现
 * 
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class TeamCheckinServiceImpl implements TeamCheckinService {
    
    private final StudyTeamCheckinMapper checkinMapper;
    private final StudyTeamCheckinLikeMapper likeMapper;
    private final StudyTeamTaskMapper taskMapper;
    private final StudyTeamMapper teamMapper;
    private final StudyTeamMemberMapper memberMapper;
    private final StudyTeamDailyStatsMapper dailyStatsMapper;
    private final UserInfoApiService userInfoApiService;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long checkin(Long teamId, CheckinRequest request, Long userId) {
        return doCheckin(teamId, request, LocalDate.now(), false, userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long supplementCheckin(Long teamId, CheckinRequest request, LocalDate date, Long userId) {
        // 验证补卡日期（只能补最近7天内的）
        LocalDate today = LocalDate.now();
        if (date.isAfter(today) || date.isBefore(today.minusDays(7))) {
            throw new BusinessException("只能补最近7天内的打卡");
        }
        
        return doCheckin(teamId, request, date, true, userId);
    }
    
    /**
     * 执行打卡
     */
    private Long doCheckin(Long teamId, CheckinRequest request, LocalDate date, boolean isSupplement, Long userId) {
        // 验证任务存在
        StudyTeamTask task = taskMapper.selectById(request.getTaskId());
        if (task == null || task.getIsDeleted() == 1) {
            throw new BusinessException("任务不存在");
        }
        
        // 验证任务属于该小组
        if (!task.getTeamId().equals(teamId)) {
            throw new BusinessException("任务不属于该小组");
        }
        
        // 验证用户是小组成员
        StudyTeamMember member = memberMapper.selectByTeamIdAndUserId(teamId, userId);
        if (member == null || MemberStatus.QUIT.getCode().equals(member.getStatus())) {
            throw new BusinessException("您不是小组成员");
        }
        
        // 检查是否已打卡
        StudyTeamCheckin existingCheckin = checkinMapper.selectUserTodayCheckin(userId, request.getTaskId(), date);
        if (existingCheckin != null) {
            throw new BusinessException("该任务今日已打卡");
        }
        
        // 验证打卡内容要求
        if (task.getRequireContent() == 1 && !StringUtils.hasText(request.getContent())) {
            throw new BusinessException("该任务需要填写打卡内容");
        }
        if (task.getRequireImage() == 1 && CollectionUtils.isEmpty(request.getImages())) {
            throw new BusinessException("该任务需要上传图片");
        }
        
        // 创建打卡记录
        StudyTeamCheckin checkin = new StudyTeamCheckin();
        checkin.setTeamId(teamId);
        checkin.setTaskId(request.getTaskId());
        checkin.setUserId(userId);
        checkin.setCheckinDate(date);
        checkin.setCompleteValue(request.getCompleteValue() != null ? request.getCompleteValue() : task.getTargetValue());
        checkin.setContent(request.getContent());
        
        // 图片列表转JSON存储
        if (!CollectionUtils.isEmpty(request.getImages())) {
            try {
                checkin.setImages(objectMapper.writeValueAsString(request.getImages()));
            } catch (JsonProcessingException e) {
                checkin.setImages("[]");
            }
        }
        
        checkin.setDuration(request.getDuration());
        checkin.setRelatedLink(request.getRelatedLink());
        checkin.setIsSupplement(isSupplement ? 1 : 0);
        checkin.setLikeCount(0);
        checkin.setCommentCount(0);
        checkin.setCreateBy(userId);
        checkin.setCreateTime(LocalDateTime.now());
        checkin.setIsDeleted(0);
        
        checkinMapper.insert(checkin);
        
        // 更新成员打卡信息
        Integer totalCheckins = member.getTotalCheckins() != null ? member.getTotalCheckins() + 1 : 1;
        Integer currentStreak = checkinMapper.countStreakDays(userId, teamId, null, date);
        Integer maxStreak = checkinMapper.countMaxStreakDays(userId, teamId, null);
        memberMapper.updateCheckinStats(teamId, userId, totalCheckins,
                currentStreak != null ? currentStreak : 0,
                maxStreak != null ? maxStreak : 0);
        teamMapper.incrementCheckinCount(teamId);
        StudyTeamDailyStats existingStats = dailyStatsMapper.selectByTeamIdAndDate(teamId, date);
        if (existingStats == null || existingStats.getCheckinCount() == null || existingStats.getCheckinCount() == 0) {
            teamMapper.incrementActiveDays(teamId);
        }
        refreshDailyStats(teamId, date);
        
        return checkin.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCheckin(Long checkinId, Long userId) {
        StudyTeamCheckin checkin = checkinMapper.selectById(checkinId);
        if (checkin == null || checkin.getIsDeleted() == 1) {
            throw new BusinessException("打卡记录不存在");
        }
        
        // 只能删除自己的打卡记录
        if (!checkin.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的打卡记录");
        }
        
        // 只能删除24小时内的打卡
        if (Duration.between(checkin.getCreateTime(), LocalDateTime.now()).toHours() > 24) {
            throw new BusinessException("只能删除24小时内的打卡记录");
        }
        
        checkinMapper.deleteById(checkinId);
        StudyTeamMember member = memberMapper.selectByTeamIdAndUserId(checkin.getTeamId(), userId);
        if (member != null) {
            Integer totalCheckins = Math.max((member.getTotalCheckins() != null ? member.getTotalCheckins() : 1) - 1, 0);
            Integer currentStreak = checkinMapper.countStreakDays(userId, checkin.getTeamId(), null, LocalDate.now());
            Integer maxStreak = checkinMapper.countMaxStreakDays(userId, checkin.getTeamId(), null);
            memberMapper.updateCheckinStats(checkin.getTeamId(), userId, totalCheckins,
                    currentStreak != null ? currentStreak : 0,
                    maxStreak != null ? maxStreak : 0);
        }
        teamMapper.decrementCheckinCount(checkin.getTeamId());
        if (checkinMapper.selectCheckinUserIds(checkin.getTeamId(), null, checkin.getCheckinDate()).isEmpty()) {
            teamMapper.decrementActiveDays(checkin.getTeamId());
        }
        refreshDailyStats(checkin.getTeamId(), checkin.getCheckinDate());
    }
    
    @Override
    public CheckinResponse getCheckinDetail(Long checkinId, Long userId) {
        CheckinResponse checkin = checkinMapper.selectCheckinById(checkinId);
        if (checkin == null) {
            throw new BusinessException("打卡记录不存在");
        }
        
        fillCheckinExtraInfo(checkin, userId, buildUserInfoMap(java.util.List.of(checkin)));
        return checkin;
    }
    
    @Override
    public List<CheckinResponse> getCheckinList(Long teamId, Long taskId, Integer page, Integer pageSize, Long userId) {
        int offset = (page - 1) * pageSize;
        List<CheckinResponse> checkins = checkinMapper.selectCheckinList(teamId, taskId, pageSize, offset);
        Map<Long, SimpleUserInfo> userInfoMap = buildUserInfoMap(checkins);
        
        for (CheckinResponse checkin : checkins) {
            fillCheckinExtraInfo(checkin, userId, userInfoMap);
        }
        
        return checkins;
    }
    
    @Override
    public List<CheckinResponse> getUserCheckins(Long userId, Long teamId, LocalDate startDate, LocalDate endDate) {
        List<CheckinResponse> checkins = checkinMapper.selectUserCheckins(userId, teamId, startDate, endDate);
        Map<Long, SimpleUserInfo> userInfoMap = buildUserInfoMap(checkins);
        
        for (CheckinResponse checkin : checkins) {
            fillCheckinExtraInfo(checkin, userId, userInfoMap);
        }
        
        return checkins;
    }
    
    @Override
    public List<LocalDate> getCheckinCalendar(Long userId, Long teamId, Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<CheckinResponse> checkins = checkinMapper.selectUserCheckins(userId, teamId, startDate, endDate);
        
        return checkins.stream()
            .map(CheckinResponse::getCheckinDate)
            .distinct()
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeCheckin(Long checkinId, Long userId) {
        StudyTeamCheckin checkin = checkinMapper.selectById(checkinId);
        if (checkin == null || checkin.getIsDeleted() == 1) {
            throw new BusinessException("打卡记录不存在");
        }
        
        // 检查是否已点赞
        Integer liked = checkinMapper.checkUserLiked(checkinId, userId);
        if (liked != null && liked > 0) {
            throw new BusinessException("您已点赞过");
        }
        
        // 添加点赞记录
        StudyTeamCheckinLike like = new StudyTeamCheckinLike();
        like.setCheckinId(checkinId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        likeMapper.insert(like);
        
        // 更新点赞数
        checkinMapper.updateLikeCount(checkinId, 1);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeCheckin(Long checkinId, Long userId) {
        StudyTeamCheckin checkin = checkinMapper.selectById(checkinId);
        if (checkin == null || checkin.getIsDeleted() == 1) {
            throw new BusinessException("打卡记录不存在");
        }
        
        // 删除点赞记录
        int deleted = likeMapper.delete(checkinId, userId);
        
        if (deleted > 0) {
            // 更新点赞数
            checkinMapper.updateLikeCount(checkinId, -1);
        }
    }
    
    @Override
    public Integer getStreakDays(Long userId, Long teamId, Long taskId) {
        LocalDate today = LocalDate.now();
        Integer streak = checkinMapper.countStreakDays(userId, teamId, taskId, today);
        return streak != null ? streak : 0;
    }
    
    @Override
    public Integer getTotalCheckinDays(Long userId, Long teamId) {
        Integer total = checkinMapper.countUserTotalCheckinDays(userId, teamId);
        return total != null ? total : 0;
    }
    
    /**
     * 填充打卡额外信息
     */
    private void fillCheckinExtraInfo(CheckinResponse checkin, Long userId, Map<Long, SimpleUserInfo> userInfoMap) {
        // 解析图片JSON
        if (checkin.getImages() != null && !checkin.getImages().isEmpty()) {
            try {
                checkin.setImageList(objectMapper.readValue(checkin.getImages(), 
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)));
            } catch (Exception e) {
                checkin.setImageList(java.util.Collections.emptyList());
            }
        }
        
        // 用户信息
        if (checkin.getUserId() != null) {
            SimpleUserInfo userInfo = userInfoMap.get(checkin.getUserId());
            if (userInfo != null) {
                checkin.setUserName(userInfo.getDisplayName());
                checkin.setUserAvatar(userInfo.getAvatar());
            }
        }
        
        // 是否已点赞
        if (userId != null) {
            Integer liked = checkinMapper.checkUserLiked(checkin.getId(), userId);
            checkin.setLiked(liked != null && liked > 0);
        } else {
            checkin.setLiked(false);
        }
        
        // 相对时间
        checkin.setRelativeTime(getRelativeTime(checkin.getCreateTime()));
    }

    private Map<Long, SimpleUserInfo> buildUserInfoMap(List<CheckinResponse> checkins) {
        if (checkins == null || checkins.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = checkins.stream()
                .map(CheckinResponse::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userInfoApiService.getSimpleUserInfoBatch(userIds);
    }
    
    /**
     * 获取相对时间描述
     */
    private String getRelativeTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        
        Duration duration = Duration.between(time, LocalDateTime.now());
        long minutes = duration.toMinutes();
        
        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (minutes < 1440) {
            return (minutes / 60) + "小时前";
        } else if (minutes < 10080) {
            return (minutes / 1440) + "天前";
        } else {
            return time.toLocalDate().toString();
        }
    }

    private void refreshDailyStats(Long teamId, LocalDate date) {
        StudyTeamDailyStats stats = new StudyTeamDailyStats();
        stats.setTeamId(teamId);
        stats.setStatDate(date);
        Integer memberCount = memberMapper.countActiveMembers(teamId);
        Integer checkinCount = dailyStatsMapper.countCheckinsByDate(teamId, date);
        stats.setMemberCount(memberCount != null ? memberCount : 0);
        stats.setCheckinCount(checkinCount != null ? checkinCount : 0);
        if (stats.getMemberCount() > 0) {
            stats.setCheckinRate(BigDecimal.valueOf(stats.getCheckinCount() * 100.0 / stats.getMemberCount()));
        } else {
            stats.setCheckinRate(BigDecimal.ZERO);
        }
        StudyTeamDailyStats existing = dailyStatsMapper.selectByTeamIdAndDate(teamId, date);
        stats.setDiscussionCount(existing != null && existing.getDiscussionCount() != null ? existing.getDiscussionCount() : 0);
        stats.setNewMemberCount(existing != null && existing.getNewMemberCount() != null ? existing.getNewMemberCount() : 0);
        dailyStatsMapper.insertOrUpdate(stats);
    }
}
