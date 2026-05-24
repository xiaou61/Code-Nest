package com.xiaou.team.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.team.domain.StudyTeam;
import com.xiaou.team.domain.StudyTeamMember;
import com.xiaou.team.dto.TeamValueStat;
import com.xiaou.team.dto.*;
import com.xiaou.team.enums.*;
import com.xiaou.team.mapper.StudyTeamCheckinMapper;
import com.xiaou.team.mapper.StudyTeamMapper;
import com.xiaou.team.mapper.StudyTeamMemberMapper;
import com.xiaou.team.mapper.StudyTeamApplicationMapper;
import com.xiaou.team.service.StudyTeamService;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学习小组服务实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyTeamServiceImpl implements StudyTeamService {
    
    private final StudyTeamMapper teamMapper;
    private final StudyTeamMemberMapper memberMapper;
    private final StudyTeamCheckinMapper checkinMapper;
    private final StudyTeamApplicationMapper applicationMapper;
    private final UserInfoApiService userInfoApiService;
    
    /**
     * 最大创建小组数
     */
    private static final int MAX_CREATE_TEAMS = 3;
    
    /**
     * 最大加入小组数
     */
    private static final int MAX_JOIN_TEAMS = 10;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamResponse createTeam(Long userId, TeamCreateRequest request) {
        // 参数校验
        if (StrUtil.isBlank(request.getTeamName())) {
            throw new BusinessException("小组名称不能为空");
        }
        if (request.getTeamName().length() < 2 || request.getTeamName().length() > 50) {
            throw new BusinessException("小组名称长度需要在2-50个字符之间");
        }
        if (request.getTeamType() == null) {
            throw new BusinessException("小组类型不能为空");
        }
        
        // 检查创建数量限制
        int createdCount = teamMapper.countByCreatorId(userId);
        if (createdCount >= MAX_CREATE_TEAMS) {
            throw new BusinessException("每个用户最多创建" + MAX_CREATE_TEAMS + "个小组");
        }
        
        // 构建小组实体
        StudyTeam team = new StudyTeam();
        team.setTeamName(request.getTeamName());
        team.setTeamDesc(request.getTeamDesc());
        team.setTeamAvatar(request.getTeamAvatar());
        team.setTeamType(request.getTeamType());
        team.setTags(request.getTags());
        team.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 20);
        team.setCurrentMembers(1); // 创建者自动成为成员
        team.setJoinType(request.getJoinType() != null ? request.getJoinType() : JoinType.APPLY.getCode());
        team.setInviteCode(generateInviteCode());
        team.setGoalTitle(request.getGoalTitle());
        team.setGoalDesc(request.getGoalDesc());
        team.setGoalStartDate(request.getGoalStartDate());
        team.setGoalEndDate(request.getGoalEndDate());
        team.setDailyTarget(request.getDailyTarget());
        team.setTotalCheckins(0);
        team.setTotalDiscussions(0);
        team.setActiveDays(0);
        team.setCreatorId(userId);
        team.setStatus(TeamStatus.NORMAL.getCode());
        team.setCreateTime(LocalDateTime.now());
        team.setUpdateTime(LocalDateTime.now());
        
        teamMapper.insert(team);
        
        // 创建者自动成为组长
        StudyTeamMember member = new StudyTeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole(MemberRole.LEADER.getCode());
        member.setTotalCheckins(0);
        member.setCurrentStreak(0);
        member.setMaxStreak(0);
        member.setTotalLikesReceived(0);
        member.setContributionPoints(0);
        member.setStatus(MemberStatus.NORMAL.getCode());
        member.setJoinTime(LocalDateTime.now());
        member.setLastActiveTime(LocalDateTime.now());
        memberMapper.insert(member);
        
        log.info("用户{}创建小组成功，小组ID：{}", userId, team.getId());
        
        return convertToTeamResponse(team, userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamResponse updateTeam(Long userId, Long teamId, TeamCreateRequest request) {
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        
        // 检查权限（只有组长可以修改）
        Integer role = memberMapper.selectRole(teamId, userId);
        if (role == null || !role.equals(MemberRole.LEADER.getCode())) {
            throw new BusinessException("只有组长可以修改小组信息");
        }
        
        // 更新字段
        if (StrUtil.isNotBlank(request.getTeamName())) {
            team.setTeamName(request.getTeamName());
        }
        team.setTeamDesc(request.getTeamDesc());
        team.setTeamAvatar(request.getTeamAvatar());
        if (request.getTeamType() != null) {
            team.setTeamType(request.getTeamType());
        }
        team.setTags(request.getTags());
        if (request.getMaxMembers() != null) {
            if (request.getMaxMembers() < team.getCurrentMembers()) {
                throw new BusinessException("最大人数不能小于当前成员数");
            }
            team.setMaxMembers(request.getMaxMembers());
        }
        if (request.getJoinType() != null) {
            team.setJoinType(request.getJoinType());
        }
        team.setGoalTitle(request.getGoalTitle());
        team.setGoalDesc(request.getGoalDesc());
        team.setGoalStartDate(request.getGoalStartDate());
        team.setGoalEndDate(request.getGoalEndDate());
        team.setDailyTarget(request.getDailyTarget());
        
        teamMapper.update(team);
        log.info("用户{}更新小组成功，小组ID：{}", userId, teamId);
        
        return convertToTeamResponse(team, userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dissolveTeam(Long userId, Long teamId) {
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        
        // 检查权限（只有组长可以解散）
        if (!team.getCreatorId().equals(userId)) {
            throw new BusinessException("只有组长可以解散小组");
        }
        
        // 检查成员数量限制
        if (team.getCurrentMembers() > 10) {
            throw new BusinessException("小组成员超过10人，不可直接解散，请先移除成员");
        }
        
        teamMapper.updateStatus(teamId, TeamStatus.DISSOLVED.getCode());
        log.info("用户{}解散小组成功，小组ID：{}", userId, teamId);
        
        return true;
    }
    
    @Override
    public TeamDetailResponse getTeamDetail(Long userId, Long teamId) {
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        
        TeamDetailResponse response = new TeamDetailResponse();
        response.setId(team.getId());
        response.setTeamName(team.getTeamName());
        response.setTeamDesc(team.getTeamDesc());
        response.setTeamAvatar(team.getTeamAvatar());
        response.setTeamType(team.getTeamType());
        
        TeamType teamType = TeamType.getByCode(team.getTeamType());
        if (teamType != null) {
            response.setTeamTypeName(teamType.getName());
            response.setTeamTypeIcon(teamType.getIcon());
        }
        
        if (StrUtil.isNotBlank(team.getTags())) {
            response.setTagList(Arrays.asList(team.getTags().split(",")));
        }
        
        response.setMaxMembers(team.getMaxMembers());
        response.setCurrentMembers(team.getCurrentMembers());
        response.setJoinType(team.getJoinType());
        
        JoinType joinType = JoinType.getByCode(team.getJoinType());
        if (joinType != null) {
            response.setJoinTypeName(joinType.getName());
        }
        
        // 目标信息
        response.setGoalTitle(team.getGoalTitle());
        response.setGoalDesc(team.getGoalDesc());
        response.setGoalStartDate(team.getGoalStartDate());
        response.setGoalEndDate(team.getGoalEndDate());
        response.setDailyTarget(team.getDailyTarget());
        
        // 计算目标进度
        if (team.getGoalStartDate() != null && team.getGoalEndDate() != null) {
            LocalDate today = LocalDate.now();
            long totalDays = ChronoUnit.DAYS.between(team.getGoalStartDate(), team.getGoalEndDate());
            long passedDays = ChronoUnit.DAYS.between(team.getGoalStartDate(), today);
            if (totalDays > 0) {
                response.setGoalProgress((int) (passedDays * 100 / totalDays));
            }
            long remainingDays = ChronoUnit.DAYS.between(today, team.getGoalEndDate());
            response.setGoalRemainingDays((int) Math.max(0, remainingDays));
        }
        
        // 统计数据
        response.setTotalCheckins(team.getTotalCheckins());
        response.setTotalDiscussions(team.getTotalDiscussions());
        response.setActiveDays(team.getActiveDays());
        response.setCheckinRate(calculateCheckinRate(teamId, team.getCurrentMembers()));
        
        // 创建者信息
        response.setCreatorId(team.getCreatorId());
        SimpleUserInfo creator = userInfoApiService.getSimpleUserInfo(team.getCreatorId());
        if (creator != null) {
            response.setCreatorName(creator.getDisplayName());
            response.setCreatorAvatar(creator.getAvatar());
        }
        
        response.setStatus(team.getStatus());
        response.setCreateTime(team.getCreateTime());
        
        // 当前用户角色
        if (userId != null) {
            Integer myRole = memberMapper.selectRole(teamId, userId);
            response.setJoined(myRole != null);
            response.setMyRole(myRole);
            if (myRole != null) {
                MemberRole role = MemberRole.getByCode(myRole);
                response.setMyRoleName(role != null ? role.getName() : null);
                // 只有成员才能看到邀请码
                response.setInviteCode(team.getInviteCode());
            }
        }
        
        // 成员头像
        List<String> avatars = memberMapper.selectMemberAvatars(teamId, 5);
        response.setMemberAvatars(avatars);
        
        return response;
    }
    
    @Override
    public PageResult<TeamResponse> getTeamList(Long userId, TeamListRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            List<StudyTeam> teams = teamMapper.selectList(request);
            return convertToTeamResponses(teams, userId);
        });
    }
    
    @Override
    public List<TeamResponse> getMyTeams(Long userId) {
        List<StudyTeam> teams = teamMapper.selectJoinedTeams(userId);
        return convertToTeamResponses(teams, userId);
    }
    
    @Override
    public List<TeamResponse> getCreatedTeams(Long userId) {
        List<StudyTeam> teams = teamMapper.selectByCreatorId(userId);
        return convertToTeamResponses(teams, userId);
    }
    
    @Override
    public List<TeamResponse> getRecommendTeams(Long userId) {
        List<StudyTeam> teams = teamMapper.selectRecommend(userId, 6);
        return convertToTeamResponses(teams, userId);
    }
    
    @Override
    public String getInviteCode(Long userId, Long teamId) {
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        
        // 检查是否是成员
        Integer role = memberMapper.selectRole(teamId, userId);
        if (role == null) {
            throw new BusinessException("您不是该小组成员");
        }
        
        return team.getInviteCode();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String refreshInviteCode(Long userId, Long teamId) {
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        
        // 检查权限（只有组长和管理员可以刷新）
        Integer role = memberMapper.selectRole(teamId, userId);
        if (role == null || role.equals(MemberRole.MEMBER.getCode())) {
            throw new BusinessException("只有组长或管理员可以刷新邀请码");
        }
        
        String newCode = generateInviteCode();
        teamMapper.updateInviteCode(teamId, newCode);
        log.info("用户{}刷新小组邀请码，小组ID：{}", userId, teamId);
        
        return newCode;
    }
    
    @Override
    public TeamResponse getTeamByInviteCode(String inviteCode) {
        if (StrUtil.isBlank(inviteCode)) {
            throw new BusinessException("邀请码不能为空");
        }
        
        StudyTeam team = teamMapper.selectByInviteCode(inviteCode);
        if (team == null) {
            throw new BusinessException("邀请码无效");
        }
        
        return convertToTeamResponse(team, null);
    }
    
    /**
     * 生成邀请码
     */
    private String generateInviteCode() {
        return RandomUtil.randomString(8).toUpperCase();
    }
    
    /**
     * 计算7日打卡率
     */
    private Integer calculateCheckinRate(Long teamId, Integer memberCount) {
        if (teamId == null || memberCount == null || memberCount <= 0) {
            return 0;
        }
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        List<TeamValueStat> stats = checkinMapper.countRecentCheckinUsersByTeamIds(
                Collections.singletonList(teamId), startDate, endDate);
        if (stats.isEmpty()) {
            return 0;
        }
        int totalActiveUsers = stats.get(0).getValue() != null ? stats.get(0).getValue() : 0;
        return Math.min(100, totalActiveUsers * 100 / (memberCount * 7));
    }
    
    /**
     * 转换为响应DTO
     */
    private TeamResponse convertToTeamResponse(StudyTeam team, Long userId) {
        return convertToTeamResponse(team, userId, Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap());
    }

    private TeamResponse convertToTeamResponse(StudyTeam team,
                                               Long userId,
                                               Map<Long, SimpleUserInfo> creatorMap,
                                               Map<Long, Integer> roleMap,
                                               Map<Long, Integer> checkinRateMap,
                                               Map<Long, Integer> pendingApplicationMap) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setTeamName(team.getTeamName());
        response.setTeamDesc(team.getTeamDesc());
        response.setTeamAvatar(team.getTeamAvatar());
        response.setTeamType(team.getTeamType());
        
        TeamType teamType = TeamType.getByCode(team.getTeamType());
        if (teamType != null) {
            response.setTeamTypeName(teamType.getName());
            response.setTeamTypeIcon(teamType.getIcon());
        }
        
        if (StrUtil.isNotBlank(team.getTags())) {
            response.setTagList(Arrays.asList(team.getTags().split(",")));
        }
        
        response.setMaxMembers(team.getMaxMembers());
        response.setCurrentMembers(team.getCurrentMembers());
        response.setJoinType(team.getJoinType());
        
        JoinType joinType = JoinType.getByCode(team.getJoinType());
        if (joinType != null) {
            response.setJoinTypeName(joinType.getName());
        }
        
        response.setGoalTitle(team.getGoalTitle());
        response.setGoalStartDate(team.getGoalStartDate());
        response.setGoalEndDate(team.getGoalEndDate());
        response.setTotalCheckins(team.getTotalCheckins());
        response.setTotalDiscussions(team.getTotalDiscussions());
        response.setActiveDays(team.getActiveDays());
        
        // 计算目标进度
        if (team.getGoalStartDate() != null && team.getGoalEndDate() != null) {
            LocalDate today = LocalDate.now();
            long totalDays = ChronoUnit.DAYS.between(team.getGoalStartDate(), team.getGoalEndDate());
            long passedDays = ChronoUnit.DAYS.between(team.getGoalStartDate(), today);
            if (totalDays > 0) {
                response.setGoalProgress((int) (passedDays * 100 / totalDays));
            }
        }
        
        Integer checkinRate = checkinRateMap.get(team.getId());
        response.setCheckinRate(checkinRate != null ? checkinRate : calculateCheckinRate(team.getId(), team.getCurrentMembers()));
        response.setCreatorId(team.getCreatorId());
        
        // 创建者信息
        SimpleUserInfo creator = creatorMap.get(team.getCreatorId());
        if (creator == null && team.getCreatorId() != null) {
            creator = userInfoApiService.getSimpleUserInfo(team.getCreatorId());
        }
        if (creator != null) {
            response.setCreatorName(creator.getDisplayName());
            response.setCreatorAvatar(creator.getAvatar());
        }
        
        response.setStatus(team.getStatus());
        TeamStatus status = TeamStatus.getByCode(team.getStatus());
        if (status != null) {
            response.setStatusName(status.getName());
        }
        
        response.setCreateTime(team.getCreateTime());
        
        // 当前用户角色
        if (userId != null) {
            Integer myRole = roleMap.containsKey(team.getId()) ? roleMap.get(team.getId()) : memberMapper.selectRole(team.getId(), userId);
            response.setJoined(myRole != null);
            response.setMyRole(myRole);
        }

        if (team.getId() != null && team.getCreatorId() != null && team.getCreatorId().equals(userId)) {
            if (pendingApplicationMap.containsKey(team.getId())) {
                response.setPendingApplications(pendingApplicationMap.get(team.getId()));
            } else {
                response.setPendingApplications(applicationMapper.countPendingByTeamId(team.getId()));
            }
        }
        
        return response;
    }

    private List<TeamResponse> convertToTeamResponses(List<StudyTeam> teams, Long userId) {
        if (teams == null || teams.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> creatorIds = teams.stream()
                .map(StudyTeam::getCreatorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SimpleUserInfo> creatorMap = creatorIds.isEmpty()
                ? Collections.emptyMap()
                : userInfoApiService.getSimpleUserInfoBatch(creatorIds);

        Map<Long, Integer> roleMap = new HashMap<>();
        if (userId != null) {
            List<Long> teamIds = teams.stream()
                    .map(StudyTeam::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<StudyTeamMember> memberships = memberMapper.selectActiveByUserIdAndTeamIds(userId, teamIds);
            for (StudyTeamMember membership : memberships) {
                roleMap.put(membership.getTeamId(), membership.getRole());
            }
        }

        Map<Long, Integer> checkinRateMap = buildCheckinRateMap(teams);
        Map<Long, Integer> pendingApplicationMap = buildPendingApplicationMap(teams, userId);
        return teams.stream()
                .map(team -> convertToTeamResponse(team, userId, creatorMap, roleMap, checkinRateMap, pendingApplicationMap))
                .collect(Collectors.toList());
    }

    private Map<Long, Integer> buildPendingApplicationMap(List<StudyTeam> teams, Long userId) {
        if (teams == null || teams.isEmpty() || userId == null) {
            return Collections.emptyMap();
        }
        List<Long> ownedTeamIds = teams.stream()
                .filter(team -> team.getId() != null && userId.equals(team.getCreatorId()))
                .map(StudyTeam::getId)
                .collect(Collectors.toList());
        if (ownedTeamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> pendingMap = applicationMapper.countPendingByTeamIds(ownedTeamIds).stream()
                .filter(stat -> stat.getTeamId() != null)
                .collect(Collectors.toMap(TeamValueStat::getTeamId,
                        stat -> stat.getValue() != null ? stat.getValue() : 0,
                        (left, right) -> left));
        for (Long teamId : ownedTeamIds) {
            pendingMap.putIfAbsent(teamId, 0);
        }
        return pendingMap;
    }

    private Map<Long, Integer> buildCheckinRateMap(List<StudyTeam> teams) {
        if (teams == null || teams.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> teamIds = teams.stream()
                .map(StudyTeam::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (teamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        Map<Long, Integer> memberCountMap = teams.stream()
                .filter(team -> team.getId() != null)
                .collect(Collectors.toMap(StudyTeam::getId,
                        team -> team.getCurrentMembers() != null ? team.getCurrentMembers() : 0,
                        (left, right) -> left));
        Map<Long, Integer> rateMap = new HashMap<>();
        List<TeamValueStat> stats = checkinMapper.countRecentCheckinUsersByTeamIds(teamIds, startDate, endDate);
        Set<Long> statTeamIds = stats.stream()
                .map(TeamValueStat::getTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (TeamValueStat stat : stats) {
            Long teamId = stat.getTeamId();
            int memberCount = memberCountMap.getOrDefault(teamId, 0);
            if (teamId != null) {
                if (memberCount <= 0) {
                    rateMap.put(teamId, 0);
                } else {
                    int totalActiveUsers = stat.getValue() != null ? stat.getValue() : 0;
                    rateMap.put(teamId, Math.min(100, totalActiveUsers * 100 / (memberCount * 7)));
                }
            }
        }
        for (Long teamId : teamIds) {
            if (!statTeamIds.contains(teamId)) {
                rateMap.put(teamId, 0);
            }
        }
        return rateMap;
    }
}
