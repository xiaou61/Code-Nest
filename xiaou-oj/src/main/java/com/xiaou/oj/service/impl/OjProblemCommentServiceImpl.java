package com.xiaou.oj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.github.pagehelper.PageHelper;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.SaTokenUserUtil;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.oj.domain.OjProblemComment;
import com.xiaou.oj.domain.OjProblemCommentLike;
import com.xiaou.oj.dto.OjCommentCreateRequest;
import com.xiaou.oj.dto.OjCommentQueryRequest;
import com.xiaou.oj.dto.OjCommentReplyRequest;
import com.xiaou.oj.dto.OjCommentResponse;
import com.xiaou.oj.mapper.OjProblemCommentLikeMapper;
import com.xiaou.oj.mapper.OjProblemCommentMapper;
import com.xiaou.oj.service.OjProblemCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OJ题目评论Service实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OjProblemCommentServiceImpl implements OjProblemCommentService {

    private final OjProblemCommentMapper commentMapper;
    private final OjProblemCommentLikeMapper likeMapper;

    @Override
    public PageResult<OjCommentResponse> getComments(Long problemId, OjCommentQueryRequest request) {
        // 查总数
        Long total = commentMapper.selectCountByProblemId(problemId);

        // 分页查询一级评论
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<OjProblemComment> comments = commentMapper.selectByProblemId(problemId, request.getSort());

        // 转换 + 加载二级回复
        List<OjCommentResponse> responseList = comments.stream()
                .map(this::convertToResponseWithReplies)
                .collect(Collectors.toList());

        return PageResult.of(request.getPageNum(), request.getPageSize(), total, responseList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComment(Long problemId, OjCommentCreateRequest request) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        String username = SaTokenUserUtil.getCurrentUserUsername("用户" + currentUserId);

        OjProblemComment comment = new OjProblemComment();
        comment.setProblemId(problemId);
        comment.setParentId(0L);
        comment.setContent(request.getContent());
        comment.setAuthorId(currentUserId);
        comment.setAuthorName(username);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);

        int result = commentMapper.insert(comment);
        if (result <= 0) {
            throw new BusinessException("发表评论失败");
        }

        log.info("OJ评论成功，用户ID: {}, 题目ID: {}, 评论ID: {}", currentUserId, problemId, comment.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyComment(Long commentId, OjCommentReplyRequest request) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        String username = SaTokenUserUtil.getCurrentUserUsername("用户" + currentUserId);

        // 检查被回复的评论是否存在
        OjProblemComment parentComment = commentMapper.selectById(commentId);
        if (parentComment == null || parentComment.getStatus() != 1) {
            throw new BusinessException("评论不存在");
        }

        // 确定一级评论ID：如果父评论本身就是一级，则 parentId = commentId，否则归到父评论的 parentId
        Long topCommentId = parentComment.getParentId() == 0 ? commentId : parentComment.getParentId();

        // 获取被回复用户的用户名
        String replyToUserName = "用户" + request.getReplyToUserId();
        OjProblemComment replyTarget = commentMapper.selectById(commentId);
        if (replyTarget != null) {
            replyToUserName = replyTarget.getAuthorName();
        }

        OjProblemComment reply = new OjProblemComment();
        reply.setProblemId(parentComment.getProblemId());
        reply.setParentId(topCommentId);
        reply.setContent(request.getContent());
        reply.setAuthorId(currentUserId);
        reply.setAuthorName(username);
        reply.setReplyToId(commentId);
        reply.setReplyToUserId(request.getReplyToUserId());
        reply.setReplyToUserName(replyToUserName);
        reply.setLikeCount(0);
        reply.setReplyCount(0);
        reply.setStatus(1);

        int result = commentMapper.insert(reply);
        if (result <= 0) {
            throw new BusinessException("回复评论失败");
        }

        // 更新一级评论的回复数
        commentMapper.updateReplyCount(topCommentId, 1);

        log.info("OJ回复评论成功，用户ID: {}, 评论ID: {}", currentUserId, commentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();

        OjProblemComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new BusinessException("评论不存在");
        }

        // 检查是否已点赞
        OjProblemCommentLike existingLike = likeMapper.selectByCommentIdAndUserId(commentId, currentUserId);
        if (existingLike != null) {
            throw new BusinessException("已经点赞过了");
        }

        OjProblemCommentLike like = new OjProblemCommentLike();
        like.setCommentId(commentId);
        like.setUserId(currentUserId);

        likeMapper.insert(like);
        commentMapper.updateLikeCount(commentId, 1);

        log.info("OJ评论点赞成功，用户ID: {}, 评论ID: {}", currentUserId, commentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();

        OjProblemComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new BusinessException("评论不存在");
        }

        OjProblemCommentLike existingLike = likeMapper.selectByCommentIdAndUserId(commentId, currentUserId);
        if (existingLike == null) {
            throw new BusinessException("还没有点赞");
        }

        likeMapper.delete(commentId, currentUserId);
        commentMapper.updateLikeCount(commentId, -1);

        log.info("OJ评论取消点赞，用户ID: {}, 评论ID: {}", currentUserId, commentId);
    }

    @Override
    public PageResult<OjCommentResponse> getCommentReplies(Long commentId, OjCommentQueryRequest request) {
        OjProblemComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<OjProblemComment> replies = commentMapper.selectAllRepliesByCommentId(commentId);
        long total = comment.getReplyCount() != null ? comment.getReplyCount() : 0;

        List<OjCommentResponse> responseList = replies.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(request.getPageNum(), request.getPageSize(), total, responseList);
    }

    // ========== 私有方法 ==========

    private OjCommentResponse convertToResponse(OjProblemComment comment) {
        OjCommentResponse response = BeanUtil.copyProperties(comment, OjCommentResponse.class);

        // 设置当前用户点赞状态
        if (StpUserUtil.isLogin()) {
            Long userId = StpUserUtil.getLoginIdAsLong();
            OjProblemCommentLike like = likeMapper.selectByCommentIdAndUserId(comment.getId(), userId);
            response.setIsLiked(like != null);
        } else {
            response.setIsLiked(false);
        }

        return response;
    }

    private OjCommentResponse convertToResponseWithReplies(OjProblemComment comment) {
        OjCommentResponse response = convertToResponse(comment);

        // 加载最多2条回复
        List<OjProblemComment> replies = commentMapper.selectRepliesByCommentId(comment.getId(), 2);
        if (replies != null && !replies.isEmpty()) {
            List<OjCommentResponse> replyResponses = replies.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            response.setReplies(replyResponses);
            response.setHasMoreReplies(comment.getReplyCount() != null && comment.getReplyCount() > 2);
        }

        return response;
    }
}
