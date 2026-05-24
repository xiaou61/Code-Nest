package com.xiaou.team.mapper;

import com.xiaou.team.domain.StudyTeam;
import com.xiaou.team.dto.TeamListRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学习小组Mapper接口
 * 
 * @author xiaou
 */
@Mapper
public interface StudyTeamMapper {
    
    /**
     * 插入小组
     */
    int insert(StudyTeam team);
    
    /**
     * 更新小组
     */
    int update(StudyTeam team);
    
    /**
     * 根据ID查询小组
     */
    StudyTeam selectById(@Param("id") Long id);
    
    /**
     * 查询小组列表（广场）
     */
    List<StudyTeam> selectList(TeamListRequest request);
    
    /**
     * 查询用户创建的小组列表
     */
    List<StudyTeam> selectByCreatorId(@Param("creatorId") Long creatorId);
    
    /**
     * 查询用户加入的小组列表
     */
    List<StudyTeam> selectJoinedTeams(@Param("userId") Long userId);
    
    /**
     * 更新小组状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 更新成员数
     */
    int updateMemberCount(@Param("id") Long id, @Param("delta") Integer delta);
    
    /**
     * 更新打卡次数
     */
    int incrementCheckinCount(@Param("id") Long id);

    /**
     * 减少打卡次数
     */
    int decrementCheckinCount(@Param("id") Long id);

    /**
     * 增加活跃天数
     */
    int incrementActiveDays(@Param("id") Long id);

    /**
     * 减少活跃天数
     */
    int decrementActiveDays(@Param("id") Long id);
    
    /**
     * 更新讨论数
     */
    int incrementDiscussionCount(@Param("id") Long id);

    /**
     * 减少讨论数
     */
    int decrementDiscussionCount(@Param("id") Long id);
    
    /**
     * 根据邀请码查询
     */
    StudyTeam selectByInviteCode(@Param("inviteCode") String inviteCode);
    
    /**
     * 更新邀请码
     */
    int updateInviteCode(@Param("id") Long id, @Param("inviteCode") String inviteCode);
    
    /**
     * 统计用户创建的小组数量
     */
    int countByCreatorId(@Param("creatorId") Long creatorId);
    
    /**
     * 查询推荐小组
     */
    List<StudyTeam> selectRecommend(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    /**
     * 查询热门小组
     */
    List<StudyTeam> selectHot(@Param("limit") Integer limit);
}
