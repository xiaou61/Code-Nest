package com.xiaou.team.service.impl;

import cn.hutool.core.util.StrUtil;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.team.domain.StudyTeam;
import com.xiaou.team.domain.StudyTeamApplication;
import com.xiaou.team.domain.StudyTeamMember;
import com.xiaou.team.dto.ApplicationResponse;
import com.xiaou.team.dto.JoinRequest;
import com.xiaou.team.dto.MemberResponse;
import com.xiaou.team.enums.*;
import com.xiaou.team.mapper.StudyTeamApplicationMapper;
import com.xiaou.team.mapper.StudyTeamMapper;
import com.xiaou.team.mapper.StudyTeamMemberMapper;
import com.xiaou.team.service.TeamMemberService;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 小组成员服务实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamMemberServiceImpl implements TeamMemberService {
    
    private final StudyTeamMapper teamMapper;
    private final StudyTeamMemberMapper memberMapper;
    private final StudyTeamApplicationMapper applicationMapper;
    private final UserInfoApiService userInfoApiService;
    
    /**
     * 最大加入小组数
     */
    private static final int MAX_JOIN_TEAMS = 10;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyJoin(Long userId, JoinRequest request) {
        Long teamId = request.getTeamId();
        
        // 检查小组是否存在
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        if (team.getStatus().equals(TeamStatus.DISSOLVED.getCode())) {
            throw new BusinessException("小组已解散");
        }
        if (team.getCurrentMembers() >= team.getMaxMembers()) {
            throw new BusinessException("小组已满员");
        }
        
        // 检查是否已经是成员
        StudyTeamMember existMember = memberMapper.selectByTeamIdAndUserId(teamId, userId);
        if (isActiveMember(existMember)) {
            throw new BusinessException("您已经是该小组成员");
        }
        
        // 检查加入小组数量限制
        int joinedCount = memberMapper.countJoinedTeams(userId);
        if (joinedCount >= MAX_JOIN_TEAMS) {
            throw new BusinessException("每个用户最多加入" + MAX_JOIN_TEAMS + "个小组");
        }
        
        // 检查是否有待审核的申请
        StudyTeamApplication pendingApp = applicationMapper.selectPendingByTeamIdAndUserId(teamId, userId);
        if (pendingApp != null) {
            throw new BusinessException("您已提交过申请，请等待审核");
        }
        
        // 检查小组加入方式
        if (team.getJoinType().equals(JoinType.PUBLIC.getCode())) {
            // 公开小组直接加入
            return addMember(teamId, userId, null);
        }
        
        if (team.getJoinType().equals(JoinType.INVITE.getCode())) {
            throw new BusinessException("该小组仅限邀请加入");
        }
        
        // 申请加入
        StudyTeamApplication application = new StudyTeamApplication();
        application.setTeamId(teamId);
        application.setUserId(userId);
        application.setApplyReason(request.getApplyReason());
        application.setStatus(ApplicationStatus.PENDING.getCode());
        application.setCreateTime(LocalDateTime.now());
        applicationMapper.insert(application);
        
        log.info("用户{}申请加入小组{}成功", userId, teamId);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean directJoin(Long userId, JoinRequest request) {
        Long teamId = request.getTeamId();
        
        // 检查小组是否存在
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("小组不存在");
        }
        if (!team.getJoinType().equals(JoinType.PUBLIC.getCode())) {
            throw new BusinessException("该小组不支持直接加入");
        }
        
        return addMember(teamId, userId, null);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinByInviteCode(Long userId, String inviteCode) {
        if (StrUtil.isBlank(inviteCode)) {
            throw new BusinessException("邀请码不能为空");
        }
        
        StudyTeam team = teamMapper.selectByInviteCode(inviteCode);
        if (team == null) {
            throw new BusinessException("邀请码无效");
        }
        
        return addMember(team.getId(), userId, null);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(Long userId, Long teamId) {
        StudyTeamMember member = memberMapper.selectByTeamIdAndUserId(teamId, userId);
        if (!isActiveMember(member)) {
            throw new BusinessException("您不是该小组成员");
        }
        
        // 组长不能直接退出
        if (member.getRole().equals(MemberRole.LEADER.getCode())) {
            throw new BusinessException("组长不能直接退出，请先转让组长");
        }
        
        memberMapper.updateStatus(teamId, userId, MemberStatus.QUIT.getCode());
        teamMapper.updateMemberCount(teamId, -1);
        
        log.info("用户{}退出小组{}", userId, teamId);
        return true;
    }
    
    @Override
    public List<MemberResponse> getMemberList(Long teamId) {
        List<StudyTeamMember> members = memberMapper.selectActiveByTeamId(teamId);
        Map<Long, SimpleUserInfo> userInfoMap = getUserInfoMap(
                members.stream().map(StudyTeamMember::getUserId).collect(Collectors.toList())
        );
        return members.stream()
                .map(member -> convertToMemberResponse(member, userInfoMap))
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveApplication(Long reviewerId, Long applicationId) {
        StudyTeamApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        if (!application.getStatus().equals(ApplicationStatus.PENDING.getCode())) {
            throw new BusinessException("申请已处理");
        }
        
        // 检查审核权限
        checkReviewPermission(reviewerId, application.getTeamId());
        
        // 添加成员
        boolean added = addMember(application.getTeamId(), application.getUserId(), application.getApplyReason());
        if (added) {
            applicationMapper.updateStatus(applicationId, ApplicationStatus.APPROVED.getCode(), reviewerId, null);
            log.info("审核人{}通过了申请{}", reviewerId, applicationId);
        }
        
        return added;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectApplication(Long reviewerId, Long applicationId, String rejectReason) {
        StudyTeamApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        if (!application.getStatus().equals(ApplicationStatus.PENDING.getCode())) {
            throw new BusinessException("申请已处理");
        }
        
        // 检查审核权限
        checkReviewPermission(reviewerId, application.getTeamId());
        
        applicationMapper.updateStatus(applicationId, ApplicationStatus.REJECTED.getCode(), reviewerId, rejectReason);
        log.info("审核人{}拒绝了申请{}", reviewerId, applicationId);
        
        return true;
    }
    
    @Override
    public List<ApplicationResponse> getApplicationList(Long userId, Long teamId) {
        // 检查权限
        checkReviewPermission(userId, teamId);
        
        List<StudyTeamApplication> applications = applicationMapper.selectPendingByTeamId(teamId);
        StudyTeam team = teamMapper.selectById(teamId);
        Map<Long, SimpleUserInfo> userInfoMap = getUserInfoMap(
                applications.stream()
                        .flatMap(app -> java.util.stream.Stream.of(app.getUserId(), app.getReviewerId()))
                        .collect(Collectors.toList())
        );
        
        return applications.stream()
                .map(app -> convertToApplicationResponse(app, team, userInfoMap))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ApplicationResponse> getMyApplications(Long userId) {
        List<StudyTeamApplication> applications = applicationMapper.selectByUserId(userId);
        Map<Long, StudyTeam> teamMap = applications.stream()
                .map(StudyTeamApplication::getTeamId)
                .filter(Objects::nonNull)
                .distinct()
                .map(teamMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(StudyTeam::getId, Function.identity(), (left, right) -> left));
        Map<Long, SimpleUserInfo> userInfoMap = getUserInfoMap(
                applications.stream()
                        .flatMap(app -> java.util.stream.Stream.of(app.getUserId(), app.getReviewerId()))
                        .collect(Collectors.toList())
        );
        return applications.stream()
                .map(app -> convertToApplicationResponse(app, teamMap.get(app.getTeamId()), userInfoMap))
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelApplication(Long userId, Long applicationId) {
        int result = applicationMapper.cancel(applicationId, userId);
        if (result > 0) {
            log.info("用户{}取消了申请{}", userId, applicationId);
            return true;
        }
        throw new BusinessException("取消申请失败");
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeMember(Long operatorId, Long teamId, Long targetUserId) {
        // 检查操作者权限
        Integer operatorRole = memberMapper.selectRole(teamId, operatorId);
        if (operatorRole == null || operatorRole.equals(MemberRole.MEMBER.getCode())) {
            throw new BusinessException("您没有权限移除成员");
        }
        
        // 获取目标成员
        StudyTeamMember targetMember = memberMapper.selectByTeamIdAndUserId(teamId, targetUserId);
        if (!isActiveMember(targetMember)) {
            throw new BusinessException("该成员不存在");
        }
        
        // 不能移除组长
        if (targetMember.getRole().equals(MemberRole.LEADER.getCode())) {
            throw new BusinessException("不能移除组长");
        }
        
        // 管理员不能移除管理员
        if (operatorRole.equals(MemberRole.ADMIN.getCode()) && targetMember.getRole().equals(MemberRole.ADMIN.getCode())) {
            throw new BusinessException("管理员不能移除其他管理员");
        }
        
        memberMapper.updateStatus(teamId, targetUserId, MemberStatus.QUIT.getCode());
        teamMapper.updateMemberCount(teamId, -1);
        
        log.info("操作者{}移除了小组{}的成员{}", operatorId, teamId, targetUserId);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setMemberRole(Long operatorId, Long teamId, Long targetUserId, Integer role) {
        // 只有组长可以设置角色
        Integer operatorRole = memberMapper.selectRole(teamId, operatorId);
        if (operatorRole == null || !operatorRole.equals(MemberRole.LEADER.getCode())) {
            throw new BusinessException("只有组长可以设置成员角色");
        }
        
        // 不能修改自己
        if (operatorId.equals(targetUserId)) {
            throw new BusinessException("不能修改自己的角色");
        }
        
        // 检查目标成员
        StudyTeamMember targetMember = memberMapper.selectByTeamIdAndUserId(teamId, targetUserId);
        if (!isActiveMember(targetMember)) {
            throw new BusinessException("该成员不存在");
        }
        
        // 只能设置为管理员或成员
        if (!role.equals(MemberRole.ADMIN.getCode()) && !role.equals(MemberRole.MEMBER.getCode())) {
            throw new BusinessException("角色无效");
        }
        
        memberMapper.updateRole(teamId, targetUserId, role);
        log.info("组长{}设置小组{}成员{}的角色为{}", operatorId, teamId, targetUserId, role);
        
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean transferLeader(Long currentLeaderId, Long teamId, Long newLeaderId) {
        // 检查当前用户是否是组长
        Integer currentRole = memberMapper.selectRole(teamId, currentLeaderId);
        if (currentRole == null || !currentRole.equals(MemberRole.LEADER.getCode())) {
            throw new BusinessException("只有组长可以转让组长");
        }
        
        // 检查新组长是否是成员
        StudyTeamMember newLeader = memberMapper.selectByTeamIdAndUserId(teamId, newLeaderId);
        if (!isActiveMember(newLeader)) {
            throw new BusinessException("目标用户不是小组成员");
        }
        
        // 转让组长
        memberMapper.updateRole(teamId, currentLeaderId, MemberRole.MEMBER.getCode());
        memberMapper.updateRole(teamId, newLeaderId, MemberRole.LEADER.getCode());
        
        log.info("小组{}的组长从{}转让给{}", teamId, currentLeaderId, newLeaderId);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean muteMember(Long operatorId, Long teamId, Long targetUserId, Integer minutes) {
        // 检查权限
        Integer operatorRole = memberMapper.selectRole(teamId, operatorId);
        if (operatorRole == null || operatorRole.equals(MemberRole.MEMBER.getCode())) {
            throw new BusinessException("您没有禁言权限");
        }
        
        // 检查目标
        StudyTeamMember target = memberMapper.selectByTeamIdAndUserId(teamId, targetUserId);
        if (!isActiveMember(target)) {
            throw new BusinessException("成员不存在");
        }
        if (target.getRole().equals(MemberRole.LEADER.getCode())) {
            throw new BusinessException("不能禁言组长");
        }
        if (operatorRole.equals(MemberRole.ADMIN.getCode()) && target.getRole().equals(MemberRole.ADMIN.getCode())) {
            throw new BusinessException("管理员不能禁言其他管理员");
        }
        
        // 最长禁言7天
        if (minutes > 7 * 24 * 60) {
            throw new BusinessException("禁言时长最长7天");
        }
        
        target.setStatus(MemberStatus.NORMAL.getCode());
        target.setMuteEndTime(LocalDateTime.now().plusMinutes(minutes));
        memberMapper.update(target);
        
        log.info("操作者{}禁言小组{}成员{}，时长{}分钟", operatorId, teamId, targetUserId, minutes);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unmuteMember(Long operatorId, Long teamId, Long targetUserId) {
        // 检查权限
        Integer operatorRole = memberMapper.selectRole(teamId, operatorId);
        if (operatorRole == null || operatorRole.equals(MemberRole.MEMBER.getCode())) {
            throw new BusinessException("您没有解除禁言权限");
        }
        
        StudyTeamMember target = memberMapper.selectByTeamIdAndUserId(teamId, targetUserId);
        if (!isActiveMember(target)) {
            throw new BusinessException("成员不存在");
        }
        
        target.setStatus(MemberStatus.NORMAL.getCode());
        target.setMuteEndTime(null);
        memberMapper.update(target);
        
        log.info("操作者{}解除小组{}成员{}的禁言", operatorId, teamId, targetUserId);
        return true;
    }
    
    @Override
    public int getPendingApplicationCount(Long userId, Long teamId) {
        // 检查权限
        Integer role = memberMapper.selectRole(teamId, userId);
        if (role == null || role.equals(MemberRole.MEMBER.getCode())) {
            return 0;
        }
        return applicationMapper.countPendingByTeamId(teamId);
    }
    
    /**
     * 添加成员
     */
    private boolean addMember(Long teamId, Long userId, String joinReason) {
        // 检查小组状态
        StudyTeam team = teamMapper.selectById(teamId);
        if (team == null || team.getStatus().equals(TeamStatus.DISSOLVED.getCode())) {
            throw new BusinessException("小组不存在或已解散");
        }
        if (team.getCurrentMembers() >= team.getMaxMembers()) {
            throw new BusinessException("小组已满员");
        }
        
        // 检查是否已是成员
        StudyTeamMember existMember = memberMapper.selectByTeamIdAndUserId(teamId, userId);
        if (existMember != null) {
            if (isActiveMember(existMember)) {
                throw new BusinessException("您已经是该小组成员");
            }
            // 重新加入
            existMember.setRole(MemberRole.MEMBER.getCode());
            existMember.setStatus(MemberStatus.NORMAL.getCode());
            existMember.setJoinTime(LocalDateTime.now());
            existMember.setJoinReason(joinReason);
            memberMapper.update(existMember);
        } else {
            // 新增成员
            StudyTeamMember member = new StudyTeamMember();
            member.setTeamId(teamId);
            member.setUserId(userId);
            member.setRole(MemberRole.MEMBER.getCode());
            member.setJoinReason(joinReason);
            member.setTotalCheckins(0);
            member.setCurrentStreak(0);
            member.setMaxStreak(0);
            member.setTotalLikesReceived(0);
            member.setContributionPoints(0);
            member.setStatus(MemberStatus.NORMAL.getCode());
            member.setJoinTime(LocalDateTime.now());
            member.setLastActiveTime(LocalDateTime.now());
            memberMapper.insert(member);
        }
        
        // 更新小组成员数
        teamMapper.updateMemberCount(teamId, 1);
        
        log.info("用户{}加入小组{}", userId, teamId);
        return true;
    }
    
    /**
     * 检查审核权限
     */
    private void checkReviewPermission(Long userId, Long teamId) {
        Integer role = memberMapper.selectRole(teamId, userId);
        if (role == null || role.equals(MemberRole.MEMBER.getCode())) {
            throw new BusinessException("您没有审核权限");
        }
    }
    
    /**
     * 转换为成员响应DTO
     */
    private MemberResponse convertToMemberResponse(StudyTeamMember member, Map<Long, SimpleUserInfo> userInfoMap) {
        MemberResponse response = new MemberResponse();
        response.setId(member.getId());
        response.setTeamId(member.getTeamId());
        response.setUserId(member.getUserId());
        response.setRole(member.getRole());
        
        MemberRole role = MemberRole.getByCode(member.getRole());
        if (role != null) {
            response.setRoleName(role.getName());
        }
        
        response.setNickname(member.getNickname());
        response.setTotalCheckins(member.getTotalCheckins());
        response.setCurrentStreak(member.getCurrentStreak());
        response.setMaxStreak(member.getMaxStreak());
        response.setTotalLikesReceived(member.getTotalLikesReceived());
        response.setContributionPoints(member.getContributionPoints());
        response.setStatus(member.getStatus());
        response.setMuteEndTime(member.getMuteEndTime());
        
        MemberStatus status = MemberStatus.getByCode(member.getStatus());
        if (status != null) {
            response.setStatusName(status.getName());
        }
        
        response.setJoinTime(member.getJoinTime());
        response.setLastActiveTime(member.getLastActiveTime());
        
        // 用户信息
        SimpleUserInfo userInfo = userInfoMap.get(member.getUserId());
        if (userInfo != null) {
            response.setUserName(userInfo.getDisplayName());
            response.setUserAvatar(userInfo.getAvatar());
        }
        
        return response;
    }
    
    /**
     * 转换为申请响应DTO
     */
    private ApplicationResponse convertToApplicationResponse(StudyTeamApplication app,
                                                            StudyTeam team,
                                                            Map<Long, SimpleUserInfo> userInfoMap) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(app.getId());
        response.setTeamId(app.getTeamId());
        response.setTeamName(team != null ? team.getTeamName() : null);
        response.setTeamAvatar(team != null ? team.getTeamAvatar() : null);
        response.setUserId(app.getUserId());
        response.setApplyReason(app.getApplyReason());
        response.setStatus(app.getStatus());
        
        ApplicationStatus status = ApplicationStatus.getByCode(app.getStatus());
        if (status != null) {
            response.setStatusName(status.getName());
        }
        
        response.setReviewerId(app.getReviewerId());
        response.setReviewTime(app.getReviewTime());
        response.setRejectReason(app.getRejectReason());
        response.setCreateTime(app.getCreateTime());
        
        // 申请人信息
        SimpleUserInfo userInfo = userInfoMap.get(app.getUserId());
        if (userInfo != null) {
            response.setUserName(userInfo.getDisplayName());
            response.setUserAvatar(userInfo.getAvatar());
        }
        
        // 审核人信息
        if (app.getReviewerId() != null) {
            SimpleUserInfo reviewer = userInfoMap.get(app.getReviewerId());
            if (reviewer != null) {
                response.setReviewerName(reviewer.getDisplayName());
            }
        }
        
        return response;
    }

    private Map<Long, SimpleUserInfo> getUserInfoMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userInfoApiService.getSimpleUserInfoBatch(userIds);
    }

    private boolean isActiveMember(StudyTeamMember member) {
        return member != null && !MemberStatus.QUIT.getCode().equals(member.getStatus());
    }
}
