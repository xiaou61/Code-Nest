package com.xiaou.oj.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.oj.dto.OjCommentCreateRequest;
import com.xiaou.oj.dto.OjCommentQueryRequest;
import com.xiaou.oj.dto.OjCommentReplyRequest;
import com.xiaou.oj.dto.OjCommentResponse;

/**
 * OJ题目评论Service接口
 *
 * @author xiaou
 */
public interface OjProblemCommentService {

    /** 查询题目的评论列表 */
    PageResult<OjCommentResponse> getComments(Long problemId, OjCommentQueryRequest request);

    /** 发表评论 */
    void createComment(Long problemId, OjCommentCreateRequest request);

    /** 回复评论 */
    void replyComment(Long commentId, OjCommentReplyRequest request);

    /** 点赞评论 */
    void likeComment(Long commentId);

    /** 取消点赞 */
    void unlikeComment(Long commentId);

    /** 获取评论的回复列表 */
    PageResult<OjCommentResponse> getCommentReplies(Long commentId, OjCommentQueryRequest request);
}
