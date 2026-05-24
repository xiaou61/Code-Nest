package com.xiaou.team.mapper;

import com.xiaou.team.domain.StudyTeamMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 小组成员Mapper接口
 * 
 * @author xiaou
 */
@Mapper
public interface StudyTeamMemberMapper {
    
    /**
     * 插入成员
     */
    int insert(StudyTeamMember member);
    
    /**
     * 更新成员
     */
    int update(StudyTeamMember member);
    
    /**
     * 根据小组ID和用户ID查询成员
     */
    StudyTeamMember selectByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);
    
    /**
     * 查询小组成员列表
     */
    List<StudyTeamMember> selectByTeamId(@Param("teamId") Long teamId);
    
    /**
     * 查询小组有效成员列表
     */
    List<StudyTeamMember> selectActiveByTeamId(@Param("teamId") Long teamId);

    /**
     * 查询用户在多个小组中的有效成员记录
     */
    List<StudyTeamMember> selectActiveByUserIdAndTeamIds(@Param("userId") Long userId,
                                                         @Param("teamIds") Collection<Long> teamIds);
    
    /**
     * 更新成员状态
     */
    int updateStatus(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("status") Integer status);
    
    /**
     * 更新成员角色
     */
    int updateRole(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("role") Integer role);
    
    /**
     * 更新打卡统计
     */
    int updateCheckinStats(@Param("teamId") Long teamId, @Param("userId") Long userId,
                           @Param("totalCheckins") Integer totalCheckins,
                           @Param("currentStreak") Integer currentStreak,
                           @Param("maxStreak") Integer maxStreak);
    
    /**
     * 增加获得点赞数
     */
    int incrementLikesReceived(@Param("teamId") Long teamId, @Param("userId") Long userId);
    
    /**
     * 统计小组有效成员数
     */
    int countActiveMembers(@Param("teamId") Long teamId);
    
    /**
     * 统计用户加入的小组数
     */
    int countJoinedTeams(@Param("userId") Long userId);
    
    /**
     * 查询用户在小组中的角色
     */
    Integer selectRole(@Param("teamId") Long teamId, @Param("userId") Long userId);
    
    /**
     * 查询成员头像列表
     */
    List<String> selectMemberAvatars(@Param("teamId") Long teamId, @Param("limit") Integer limit);
    
    /**
     * 删除成员（物理删除）
     */
    int deleteByTeamId(@Param("teamId") Long teamId);
    
    /**
     * 统计小组成员数（用于selectCount）
     */
    Long selectCount(@Param("teamId") Long teamId);
    
    /**
     * 查询单个成员（兼容方法）
     */
    StudyTeamMember selectOne(@Param("teamId") Long teamId, @Param("userId") Long userId);
}
