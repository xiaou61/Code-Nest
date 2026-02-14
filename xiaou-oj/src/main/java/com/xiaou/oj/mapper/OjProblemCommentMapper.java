package com.xiaou.oj.mapper;

import com.xiaou.oj.domain.OjProblemComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OJ题目评论Mapper
 *
 * @author xiaou
 */
@Mapper
public interface OjProblemCommentMapper {

    /** 插入评论 */
    int insert(OjProblemComment comment);

    /** 根据ID查询 */
    OjProblemComment selectById(Long id);

    /** 分页查询题目的一级评论 */
    List<OjProblemComment> selectByProblemId(@Param("problemId") Long problemId,
                                             @Param("sort") String sort);

    /** 查询题目评论总数（一级） */
    Long selectCountByProblemId(@Param("problemId") Long problemId);

    /** 查询评论的回复列表（最多N条） */
    List<OjProblemComment> selectRepliesByCommentId(@Param("commentId") Long commentId,
                                                     @Param("limit") Integer limit);

    /** 查询评论的所有回复 */
    List<OjProblemComment> selectAllRepliesByCommentId(@Param("commentId") Long commentId);

    /** 更新点赞数 */
    int updateLikeCount(@Param("id") Long id, @Param("count") int count);

    /** 更新回复数 */
    int updateReplyCount(@Param("id") Long id, @Param("count") int count);

    /** 软删除评论 */
    int deleteComment(Long id);
}
