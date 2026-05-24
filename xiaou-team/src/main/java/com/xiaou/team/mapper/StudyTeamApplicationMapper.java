package com.xiaou.team.mapper;

import com.xiaou.team.domain.StudyTeamApplication;
import com.xiaou.team.dto.TeamValueStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 小组申请Mapper接口
 * 
 * @author xiaou
 */
@Mapper
public interface StudyTeamApplicationMapper {
    
    /**
     * 插入申请
     */
    int insert(StudyTeamApplication application);
    
    /**
     * 根据ID查询申请
     */
    StudyTeamApplication selectById(@Param("id") Long id);
    
    /**
     * 查询小组的待审核申请列表
     */
    List<StudyTeamApplication> selectPendingByTeamId(@Param("teamId") Long teamId);
    
    /**
     * 查询小组的所有申请列表
     */
    List<StudyTeamApplication> selectByTeamId(@Param("teamId") Long teamId);
    
    /**
     * 查询用户的申请记录
     */
    List<StudyTeamApplication> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户对某个小组的待审核申请
     */
    StudyTeamApplication selectPendingByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);
    
    /**
     * 更新申请状态（审批）
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status,
                     @Param("reviewerId") Long reviewerId, @Param("rejectReason") String rejectReason);
    
    /**
     * 取消申请
     */
    int cancel(@Param("id") Long id, @Param("userId") Long userId);
    
    /**
     * 统计用户待审核的申请数
     */
    int countPendingByUserId(@Param("userId") Long userId);
    
    /**
     * 统计小组待审核的申请数
     */
    int countPendingByTeamId(@Param("teamId") Long teamId);

    /**
     * 批量统计小组待审核的申请数
     */
    List<TeamValueStat> countPendingByTeamIds(@Param("teamIds") List<Long> teamIds);
}
