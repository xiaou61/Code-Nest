package com.xiaou.oj.mapper;

import com.xiaou.oj.domain.OjProblemCommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * OJ题目评论点赞Mapper
 *
 * @author xiaou
 */
@Mapper
public interface OjProblemCommentLikeMapper {

    /** 插入点赞记录 */
    int insert(OjProblemCommentLike like);

    /** 删除点赞记录 */
    int delete(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /** 查询是否已点赞 */
    OjProblemCommentLike selectByCommentIdAndUserId(@Param("commentId") Long commentId,
                                                     @Param("userId") Long userId);
}
