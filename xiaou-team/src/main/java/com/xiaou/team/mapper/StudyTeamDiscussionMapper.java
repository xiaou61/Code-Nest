package com.xiaou.team.mapper;

import com.xiaou.team.domain.StudyTeamDiscussion;
import com.xiaou.team.dto.DiscussionResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 讨论Mapper
 * 
 * @author xiaou
 */
@Mapper
public interface StudyTeamDiscussionMapper {
    
    /**
     * 插入讨论
     */
    int insert(StudyTeamDiscussion discussion);
    
    /**
     * 更新讨论
     */
    int update(StudyTeamDiscussion discussion);
    
    /**
     * 根据ID查询讨论
     */
    StudyTeamDiscussion selectById(@Param("id") Long id);
    
    /**
     * 删除讨论
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新置顶状态
     */
    int updateTopStatus(@Param("id") Long id, @Param("isTop") Integer isTop);
    
    /**
     * 更新精华状态
     */
    int updateEssenceStatus(@Param("id") Long id, @Param("isEssence") Integer isEssence);
    
    /**
     * 更新点赞数
     */
    int updateLikeCount(@Param("id") Long id, @Param("delta") Integer delta);
    
    /**
     * 查询讨论列表
     * 
     * @param teamId 小组ID
     * @param category 分类（可选）
     * @param keyword 关键词（可选）
     * @param limit 条数
     * @param offset 偏移
     * @return 讨论列表
     */
    List<DiscussionResponse> selectDiscussionList(@Param("teamId") Long teamId,
                                                   @Param("category") Integer category,
                                                   @Param("keyword") String keyword,
                                                   @Param("limit") Integer limit,
                                                   @Param("offset") Integer offset);
    
    /**
     * 查询讨论详情
     * 
     * @param discussionId 讨论ID
     * @return 讨论详情
     */
    DiscussionResponse selectDiscussionById(@Param("discussionId") Long discussionId);
    
    /**
     * 统计讨论数量
     * 
     * @param teamId 小组ID
     * @param category 分类（可选）
     * @return 数量
     */
    Integer countDiscussions(@Param("teamId") Long teamId, @Param("category") Integer category);

    /**
     * 统计某日新增讨论数
     */
    Integer countDiscussionsByDate(@Param("teamId") Long teamId, @Param("date") java.time.LocalDate date);
    
    /**
     * 增加浏览量
     * 
     * @param discussionId 讨论ID
     */
    void incrementViewCount(@Param("discussionId") Long discussionId);
}
