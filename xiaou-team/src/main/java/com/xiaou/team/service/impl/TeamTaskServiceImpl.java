package com.xiaou.team.service.impl;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.team.domain.StudyTeam;
import com.xiaou.team.domain.StudyTeamMember;
import com.xiaou.team.domain.StudyTeamTask;
import com.xiaou.team.dto.TaskCreateRequest;
import com.xiaou.team.dto.TaskResponse;
import com.xiaou.team.enums.MemberStatus;
import com.xiaou.team.enums.MemberRole;
import com.xiaou.team.enums.RepeatType;
import com.xiaou.team.enums.TaskType;
import com.xiaou.team.mapper.StudyTeamMapper;
import com.xiaou.team.mapper.StudyTeamMemberMapper;
import com.xiaou.team.mapper.StudyTeamTaskMapper;
import com.xiaou.team.service.TeamTaskService;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 打卡任务服务实现
 * 
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class TeamTaskServiceImpl implements TeamTaskService {
    
    private final StudyTeamTaskMapper taskMapper;
    private final StudyTeamMapper teamMapper;
    private final StudyTeamMemberMapper memberMapper;
    private final UserInfoApiService userInfoApiService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(Long teamId, TaskCreateRequest request, Long userId) {
        // 验证小组存在
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        
        // 验证用户权限（组长或管理员）
        checkAdminPermission(teamId, userId);
        
        // 创建任务
        StudyTeamTask task = new StudyTeamTask();
        task.setTeamId(teamId);
        task.setTaskName(request.getTaskName());
        task.setTaskDesc(request.getTaskDesc());
        task.setTaskType(request.getTaskType());
        task.setTargetValue(request.getTargetValue() != null ? request.getTargetValue() : 1);
        task.setTargetUnit(request.getTargetUnit() != null ? request.getTargetUnit() : "次");
        task.setRepeatType(request.getRepeatType() != null ? request.getRepeatType() : RepeatType.DAILY.getCode());
        task.setRepeatDays(request.getRepeatDays());
        task.setRequireContent(request.getRequireContent() != null ? request.getRequireContent() : 0);
        task.setRequireImage(request.getRequireImage() != null ? request.getRequireImage() : 0);
        task.setStartDate(request.getStartDate());
        task.setEndDate(request.getEndDate());
        task.setCreatorId(userId);
        task.setStatus(1);
        task.setCreateBy(userId);
        task.setCreateTime(LocalDateTime.now());
        task.setIsDeleted(0);
        
        taskMapper.insert(task);
        return task.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(Long taskId, TaskCreateRequest request, Long userId) {
        StudyTeamTask task = taskMapper.selectById(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            throw new BusinessException("任务不存在");
        }
        
        // 验证用户权限
        checkAdminPermission(task.getTeamId(), userId);
        
        // 更新任务
        task.setTaskName(request.getTaskName());
        task.setTaskDesc(request.getTaskDesc());
        task.setTaskType(request.getTaskType());
        task.setTargetValue(request.getTargetValue());
        task.setTargetUnit(request.getTargetUnit());
        task.setRepeatType(request.getRepeatType());
        task.setRepeatDays(request.getRepeatDays());
        task.setRequireContent(request.getRequireContent());
        task.setRequireImage(request.getRequireImage());
        task.setStartDate(request.getStartDate());
        task.setEndDate(request.getEndDate());
        task.setUpdateBy(userId);
        task.setUpdateTime(LocalDateTime.now());
        
        taskMapper.update(task);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long taskId, Long userId) {
        StudyTeamTask task = taskMapper.selectById(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            throw new BusinessException("任务不存在");
        }
        
        // 验证用户权限
        checkAdminPermission(task.getTeamId(), userId);
        
        // 逻辑删除
        taskMapper.deleteById(taskId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setTaskStatus(Long taskId, Integer status, Long userId) {
        StudyTeamTask task = taskMapper.selectById(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            throw new BusinessException("任务不存在");
        }
        
        // 验证用户权限
        checkAdminPermission(task.getTeamId(), userId);
        
        taskMapper.updateStatus(taskId, status);
    }
    
    @Override
    public TaskResponse getTaskDetail(Long taskId, Long userId) {
        TaskResponse task = taskMapper.selectTaskById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // 填充额外信息
        fillTaskExtraInfo(task, userId, buildUserInfoMap(java.util.List.of(task)));
        return task;
    }
    
    @Override
    public List<TaskResponse> getTaskList(Long teamId, Integer status, Long userId) {
        List<TaskResponse> tasks = taskMapper.selectTaskList(teamId, status);
        Map<Long, SimpleUserInfo> userInfoMap = buildUserInfoMap(tasks);
        
        // 填充额外信息
        for (TaskResponse task : tasks) {
            fillTaskExtraInfo(task, userId, userInfoMap);
        }
        
        return tasks;
    }
    
    @Override
    public List<TaskResponse> getTodayTasks(Long teamId, Long userId) {
        LocalDate today = LocalDate.now();
        // Java的DayOfWeek: MONDAY=1 ... SUNDAY=7
        int dayOfWeek = today.getDayOfWeek().getValue();
        
        List<TaskResponse> tasks = taskMapper.selectTodayTasks(teamId, today, dayOfWeek);
        Map<Long, SimpleUserInfo> userInfoMap = buildUserInfoMap(tasks);
        
        // 填充额外信息
        for (TaskResponse task : tasks) {
            fillTaskExtraInfo(task, userId, userInfoMap);
        }
        
        return tasks;
    }
    
    /**
     * 填充任务额外信息
     */
    private void fillTaskExtraInfo(TaskResponse task, Long userId, Map<Long, SimpleUserInfo> userInfoMap) {
        LocalDate today = LocalDate.now();
        
        // 任务类型名称
        TaskType taskType = TaskType.getByCode(task.getTaskType());
        task.setTaskTypeName(taskType != null ? taskType.getName() : "未知");
        
        // 重复类型名称
        RepeatType repeatType = RepeatType.getByCode(task.getRepeatType());
        task.setRepeatTypeName(repeatType != null ? repeatType.getName() : "未知");
        
        // 今日打卡人数
        Integer todayCount = taskMapper.countTodayCheckins(task.getTaskId(), today);
        task.setTodayCheckinCount(todayCount != null ? todayCount : 0);
        
        // 当前用户今日是否已打卡
        Integer userCheckin = taskMapper.checkUserTodayCheckin(task.getTaskId(), userId, today);
        task.setTodayCheckedIn(userCheckin != null && userCheckin > 0);
        
        // 创建人信息
        if (task.getCreateBy() != null) {
            SimpleUserInfo creatorInfo = userInfoMap.get(task.getCreateBy());
            if (creatorInfo != null) {
                task.setCreatorName(creatorInfo.getDisplayName());
            }
        }
    }
    
    /**
     * 检查管理员权限（组长或管理员）
     */
    private void checkAdminPermission(Long teamId, Long userId) {
        StudyTeamMember member = memberMapper.selectByTeamIdAndUserId(teamId, userId);
        
        if (member == null || MemberStatus.QUIT.getCode().equals(member.getStatus())) {
            throw new BusinessException("您不是小组成员");
        }
        
        if (member.getRole() != MemberRole.LEADER.getCode() && 
            member.getRole() != MemberRole.ADMIN.getCode()) {
            throw new BusinessException("您没有权限执行此操作");
        }
    }

    private Map<Long, SimpleUserInfo> buildUserInfoMap(List<TaskResponse> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = tasks.stream()
                .map(TaskResponse::getCreateBy)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userInfoApiService.getSimpleUserInfoBatch(userIds);
    }
}
