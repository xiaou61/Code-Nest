package com.xiaou.team.mapper;

import com.xiaou.team.domain.StudyTeamDiscussionLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 讨论点赞Mapper
 *
 * @author xiaou
 */
@Mapper
public interface StudyTeamDiscussionLikeMapper {

    /**
     * 插入点赞记录
     */
    int insert(StudyTeamDiscussionLike like);

    /**
     * 删除点赞记录
     */
    int delete(@Param("discussionId") Long discussionId, @Param("userId") Long userId);

    /**
     * 检查用户是否已点赞
     */
    Integer checkExists(@Param("discussionId") Long discussionId, @Param("userId") Long userId);

    /**
     * 批量查询用户已点赞的讨论ID
     */
    List<Long> selectLikedDiscussionIds(@Param("userId") Long userId,
                                        @Param("discussionIds") List<Long> discussionIds);
}
