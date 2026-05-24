package com.xiaou.team.service.impl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.team.domain.StudyTeamDiscussion;
import com.xiaou.team.domain.StudyTeamDiscussionLike;
import com.xiaou.team.domain.StudyTeamDailyStats;
import com.xiaou.team.domain.StudyTeamMember;
import com.xiaou.team.dto.DiscussionCreateRequest;
import com.xiaou.team.dto.DiscussionResponse;
import com.xiaou.team.enums.DiscussionCategory;
import com.xiaou.team.enums.MemberStatus;
import com.xiaou.team.enums.MemberRole;
import com.xiaou.team.mapper.StudyTeamDiscussionMapper;
import com.xiaou.team.mapper.StudyTeamDiscussionLikeMapper;
import com.xiaou.team.mapper.StudyTeamDailyStatsMapper;
import com.xiaou.team.mapper.StudyTeamMapper;
import com.xiaou.team.mapper.StudyTeamMemberMapper;
import com.xiaou.team.service.TeamDiscussionService;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 讨论服务实现
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class TeamDiscussionServiceImpl implements TeamDiscussionService {

    private final StudyTeamDiscussionMapper discussionMapper;
    private final StudyTeamDiscussionLikeMapper likeMapper;
    private final StudyTeamMemberMapper memberMapper;
    private final StudyTeamMapper teamMapper;
    private final StudyTeamDailyStatsMapper dailyStatsMapper;
    private final UserInfoApiService userInfoApiService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDiscussion(Long teamId, DiscussionCreateRequest request, Long userId) {
        // 验证用户是小组成员
        StudyTeamMember member = memberMapper.selectByTeamIdAndUserId(teamId, userId);
        if (member == null || MemberStatus.QUIT.getCode().equals(member.getStatus())) {
            throw new BusinessException("您不是小组成员");
        }

        // 检查是否被禁言
        if (member.getMuteEndTime() != null && member.getMuteEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("您已被禁言，无法发布讨论");
        }

        // 公告类只有管理员以上可以发
        if (request.getCategory() != null && request.getCategory() == DiscussionCategory.ANNOUNCEMENT.getCode()) {
            if (member.getRole() != MemberRole.LEADER.getCode() && member.getRole() != MemberRole.ADMIN.getCode()) {
                throw new BusinessException("只有组长或管理员可以发布公告");
            }
        }

        StudyTeamDiscussion discussion = new StudyTeamDiscussion();
        discussion.setTeamId(teamId);
        discussion.setUserId(userId);
        discussion.setCategory(request.getCategory() != null ? request.getCategory() : DiscussionCategory.CHAT.getCode());
        discussion.setTitle(request.getTitle());
        discussion.setContent(request.getContent());

        if (!CollectionUtils.isEmpty(request.getImages())) {
            try {
                discussion.setImages(objectMapper.writeValueAsString(request.getImages()));
            } catch (JsonProcessingException e) {
                discussion.setImages("[]");
            }
        }

        discussion.setViewCount(0);
        discussion.setLikeCount(0);
        discussion.setCommentCount(0);
        discussion.setIsTop(request.getIsTop() != null ? request.getIsTop() : 0);
        discussion.setIsEssence(request.getIsEssence() != null ? request.getIsEssence() : 0);
        discussion.setCreateBy(userId);
        discussion.setCreateTime(LocalDateTime.now());
        discussion.setIsDeleted(0);

        discussionMapper.insert(discussion);
        teamMapper.incrementDiscussionCount(teamId);
        refreshDailyStats(teamId, discussion.getCreateTime().toLocalDate());
        return discussion.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDiscussion(Long discussionId, DiscussionCreateRequest request, Long userId) {
        StudyTeamDiscussion discussion = discussionMapper.selectById(discussionId);
        if (discussion == null || discussion.getIsDeleted() == 1) {
            throw new BusinessException("讨论不存在");
        }

        // 只有作者可以编辑
        if (!discussion.getUserId().equals(userId)) {
            throw new BusinessException("只能编辑自己的讨论");
        }

        discussion.setTitle(request.getTitle());
        discussion.setContent(request.getContent());
        if (!CollectionUtils.isEmpty(request.getImages())) {
            try {
                discussion.setImages(objectMapper.writeValueAsString(request.getImages()));
            } catch (JsonProcessingException e) {
                discussion.setImages("[]");
            }
        }
        discussion.setUpdateBy(userId);
        discussion.setUpdateTime(LocalDateTime.now());

        discussionMapper.update(discussion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDiscussion(Long discussionId, Long userId) {
        StudyTeamDiscussion discussion = discussionMapper.selectById(discussionId);
        if (discussion == null || discussion.getIsDeleted() == 1) {
            throw new BusinessException("讨论不存在");
        }

        // 作者或管理员可以删除
        boolean canDelete = discussion.getUserId().equals(userId);
        if (!canDelete) {
            StudyTeamMember member = memberMapper.selectByTeamIdAndUserId(discussion.getTeamId(), userId);
            if (member != null && !MemberStatus.QUIT.getCode().equals(member.getStatus()) && (member.getRole() == MemberRole.LEADER.getCode() ||
                                   member.getRole() == MemberRole.ADMIN.getCode())) {
                canDelete = true;
            }
        }

        if (!canDelete) {
            throw new BusinessException("您没有权限删除此讨论");
        }

        discussionMapper.deleteById(discussionId);
        teamMapper.decrementDiscussionCount(discussion.getTeamId());
        if (discussion.getCreateTime() != null) {
            refreshDailyStats(discussion.getTeamId(), discussion.getCreateTime().toLocalDate());
        }
    }

    @Override
    public DiscussionResponse getDiscussionDetail(Long discussionId, Long userId) {
        DiscussionResponse discussion = discussionMapper.selectDiscussionById(discussionId);
        if (discussion == null) {
            throw new BusinessException("讨论不存在");
        }

        // 增加浏览量
        discussionMapper.incrementViewCount(discussionId);
        discussion.setViewCount(discussion.getViewCount() + 1);
        Set<Long> likedDiscussionIds = buildLikedDiscussionIds(userId, java.util.List.of(discussion));

        fillDiscussionExtraInfo(discussion, userId,
                buildUserInfoMap(java.util.List.of(discussion)),
                buildMemberMap(discussion.getTeamId()),
                likedDiscussionIds);
        return discussion;
    }

    @Override
    public List<DiscussionResponse> getDiscussionList(Long teamId, Integer category, String keyword,
                                                       Integer page, Integer pageSize, Long userId) {
        int offset = (page - 1) * pageSize;
        List<DiscussionResponse> discussions = discussionMapper.selectDiscussionList(teamId, category, keyword, pageSize, offset);
        Map<Long, SimpleUserInfo> userInfoMap = buildUserInfoMap(discussions);
        Map<Long, StudyTeamMember> memberMap = buildMemberMap(teamId);
        Set<Long> likedDiscussionIds = buildLikedDiscussionIds(userId, discussions);

        for (DiscussionResponse discussion : discussions) {
            fillDiscussionExtraInfo(discussion, userId, userInfoMap, memberMap, likedDiscussionIds);
        }

        return discussions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setTop(Long discussionId, Integer isTop, Long userId) {
        StudyTeamDiscussion discussion = discussionMapper.selectById(discussionId);
        if (discussion == null || discussion.getIsDeleted() == 1) {
            throw new BusinessException("讨论不存在");
        }

        // 检查权限
        checkAdminPermission(discussion.getTeamId(), userId);

        discussionMapper.updateTopStatus(discussionId, isTop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEssence(Long discussionId, Integer isEssence, Long userId) {
        StudyTeamDiscussion discussion = discussionMapper.selectById(discussionId);
        if (discussion == null || discussion.getIsDeleted() == 1) {
            throw new BusinessException("讨论不存在");
        }

        // 检查权限
        checkAdminPermission(discussion.getTeamId(), userId);

        discussionMapper.updateEssenceStatus(discussionId, isEssence);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeDiscussion(Long discussionId, Long userId) {
        StudyTeamDiscussion discussion = discussionMapper.selectById(discussionId);
        if (discussion == null || discussion.getIsDeleted() == 1) {
            throw new BusinessException("讨论不存在");
        }

        Integer liked = likeMapper.checkExists(discussionId, userId);
        if (liked != null && liked > 0) {
            throw new BusinessException("您已点赞过");
        }

        StudyTeamDiscussionLike like = new StudyTeamDiscussionLike();
        like.setDiscussionId(discussionId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        likeMapper.insert(like);
        discussionMapper.updateLikeCount(discussionId, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeDiscussion(Long discussionId, Long userId) {
        StudyTeamDiscussion discussion = discussionMapper.selectById(discussionId);
        if (discussion == null || discussion.getIsDeleted() == 1) {
            throw new BusinessException("讨论不存在");
        }

        int deleted = likeMapper.delete(discussionId, userId);
        if (deleted > 0) {
            discussionMapper.updateLikeCount(discussionId, -1);
        }
    }

    /**
     * 填充讨论额外信息
     */
    private void fillDiscussionExtraInfo(DiscussionResponse discussion,
                                         Long userId,
                                         Map<Long, SimpleUserInfo> userInfoMap,
                                         Map<Long, StudyTeamMember> memberMap,
                                         Set<Long> likedDiscussionIds) {
        // 解析图片JSON
        if (discussion.getImagesJson() != null && !discussion.getImagesJson().isEmpty()) {
            try {
                discussion.setImages(objectMapper.readValue(discussion.getImagesJson(),
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)));
            } catch (Exception e) {
                discussion.setImages(java.util.Collections.emptyList());
            }
        }

        // 用户信息
        if (discussion.getUserId() != null) {
            SimpleUserInfo userInfo = userInfoMap.get(discussion.getUserId());
            if (userInfo != null) {
                discussion.setUserName(userInfo.getDisplayName());
                discussion.setUserAvatar(userInfo.getAvatar());
            }

            // 获取用户在小组中的角色
            StudyTeamMember member = memberMap.get(discussion.getUserId());
            if (member != null) {
                discussion.setUserRole(member.getRole());
                MemberRole role = MemberRole.getByCode(member.getRole());
                discussion.setUserRoleName(role != null ? role.getName() : "成员");
            }
        }

        // 分类名称
        DiscussionCategory category = DiscussionCategory.getByCode(discussion.getCategory());
        discussion.setCategoryName(category != null ? category.getName() : "其他");

        // 是否是作者
        discussion.setIsOwner(userId != null && userId.equals(discussion.getUserId()));

        // 相对时间
        discussion.setRelativeTime(getRelativeTime(discussion.getCreateTime()));

        discussion.setLiked(userId != null && likedDiscussionIds.contains(discussion.getId()));
    }

    /**
     * 检查管理员权限
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

    private Map<Long, SimpleUserInfo> buildUserInfoMap(List<DiscussionResponse> discussions) {
        if (discussions == null || discussions.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = discussions.stream()
                .map(DiscussionResponse::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userInfoApiService.getSimpleUserInfoBatch(userIds);
    }

    private Map<Long, StudyTeamMember> buildMemberMap(Long teamId) {
        if (teamId == null) {
            return Collections.emptyMap();
        }
        return memberMapper.selectActiveByTeamId(teamId).stream()
                .filter(member -> member.getUserId() != null)
                .collect(Collectors.toMap(StudyTeamMember::getUserId, member -> member, (left, right) -> left));
    }

    private Set<Long> buildLikedDiscussionIds(Long userId, List<DiscussionResponse> discussions) {
        if (userId == null || discussions == null || discussions.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> discussionIds = discussions.stream()
                .map(DiscussionResponse::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (discussionIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(likeMapper.selectLikedDiscussionIds(userId, discussionIds));
    }

    private void refreshDailyStats(Long teamId, LocalDate date) {
        StudyTeamDailyStats stats = new StudyTeamDailyStats();
        stats.setTeamId(teamId);
        stats.setStatDate(date);
        Integer memberCount = memberMapper.countActiveMembers(teamId);
        Integer checkinCount = dailyStatsMapper.countCheckinsByDate(teamId, date);
        Integer discussionCount = discussionMapper.countDiscussionsByDate(teamId, date);
        stats.setMemberCount(memberCount != null ? memberCount : 0);
        stats.setCheckinCount(checkinCount != null ? checkinCount : 0);
        if (stats.getMemberCount() > 0) {
            stats.setCheckinRate(BigDecimal.valueOf(stats.getCheckinCount() * 100.0 / stats.getMemberCount()));
        } else {
            stats.setCheckinRate(BigDecimal.ZERO);
        }
        stats.setDiscussionCount(discussionCount != null ? discussionCount : 0);
        StudyTeamDailyStats existing = dailyStatsMapper.selectByTeamIdAndDate(teamId, date);
        stats.setNewMemberCount(existing != null && existing.getNewMemberCount() != null ? existing.getNewMemberCount() : 0);
        dailyStatsMapper.insertOrUpdate(stats);
    }

    /**
     * 获取相对时间
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
}
