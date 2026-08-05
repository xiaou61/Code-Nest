package com.xiaou.community.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.SaTokenUserUtil;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.common.utils.SensitiveWordUtils;
import com.xiaou.community.domain.CommunityComment;
import com.xiaou.community.domain.CommunityCommentLike;
import com.xiaou.community.domain.CommunityPost;
import com.xiaou.community.domain.CommunityUserStatus;
import com.xiaou.community.dto.AdminCommentQueryRequest;
import com.xiaou.community.dto.CommunityCommentQueryRequest;
import com.xiaou.community.dto.CommunityCommentCreateRequest;
import com.xiaou.community.dto.CommunityCommentReplyRequest;
import com.xiaou.community.dto.CommunityCommentResponse;
import com.xiaou.community.mapper.CommunityCommentMapper;
import com.xiaou.community.mapper.CommunityCommentLikeMapper;
import com.xiaou.community.mapper.CommunityPostMapper;
import com.xiaou.community.service.CommunityCommentService;
import com.xiaou.community.service.CommunityUserStatusService;
import com.xiaou.notification.api.NotificationCommand;
import com.xiaou.notification.api.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 社区评论Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityCommentServiceImpl implements CommunityCommentService {
    
    private final CommunityCommentMapper communityCommentMapper;
    private final CommunityCommentLikeMapper communityCommentLikeMapper;
    private final CommunityPostMapper communityPostMapper;
    private final CommunityUserStatusService communityUserStatusService;
    private final NotificationPublisher notificationPublisher;
    
    @Override
    public PageResult<CommunityComment> getAdminCommentList(AdminCommentQueryRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> 
            communityCommentMapper.selectAdminCommentList(request)
        );
    }
    
    @Override
    public CommunityComment getById(Long id) {
        CommunityComment comment = communityCommentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        return comment;
    }
    
    @Override
    public void deleteComment(Long id) {
        CommunityComment comment = getById(id);
        
        int result = communityCommentMapper.deleteComment(id);
        if (result <= 0) {
            throw new BusinessException("删除评论失败");
        }
        
        // 减少用户评论数统计
        communityUserStatusService.decrementCommentCount(comment.getAuthorId());
        
        log.info("评论删除成功，评论ID: {}", id);
    }
    
    @Override
    public List<CommunityComment> getCommentsByUserId(Long userId) {
        return communityCommentMapper.selectByUserId(userId);
    }
    
    // 前台接口实现
    
    @Override
    public PageResult<CommunityCommentResponse> getPostComments(Long postId, CommunityCommentQueryRequest request) {
        // 先获取分页的原始评论数据（只查询一级评论）
        PageResult<CommunityComment> pageResult = PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            return communityCommentMapper.selectPostCommentList(postId, request);
        });
        
        // 在分页外进行DTO转换，并加载二级回复
        List<CommunityCommentResponse> responseList = pageResult.getRecords().stream()
            .map(this::convertToResponseWithReplies)  // 使用新方法加载二级回复
            .collect(Collectors.toList());
        
        // 构造返回结果，保持分页信息
        return PageResult.of(
            pageResult.getPageNum(),
            pageResult.getPageSize(),
            pageResult.getTotal(),
            responseList
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComment(Long postId, CommunityCommentCreateRequest request) {
        // 检查用户是否被封禁
        communityUserStatusService.checkUserBanStatus();
        
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 检查帖子是否存在
        if (communityPostMapper.selectById(postId) == null) {
            throw new BusinessException("帖子不存在");
        }
        
        // 敏感词检测
        String content = request.getContent();
        try {
            SensitiveWordUtils.SensitiveCheckResult sensitiveResult = 
                SensitiveWordUtils.checkText(content, "community", postId, currentUserId);
            
            if (!sensitiveResult.getAllowed()) {
                throw new BusinessException("评论包含违规内容，禁止发布");
            }
            
            // 使用处理后的文本（替换敏感词）
            content = sensitiveResult.getProcessedText();
            
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            log.warn("敏感词检测异常，使用原始内容：{}", e.getMessage());
        }
        
        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setParentId(request.getParentId());
        comment.setContent(content); // 使用处理后的内容
        comment.setAuthorId(currentUserId);
        // Sa-Token: 从工具类获取用户名
        String username = SaTokenUserUtil.getCurrentUserUsername("用户" + currentUserId);
        comment.setAuthorName(username);
        comment.setLikeCount(0);
        comment.setReplyToId(null);  // 一级评论没有回复对象
        comment.setReplyToUserId(null);
        comment.setReplyToUserName(null);
        comment.setReplyCount(0);  // 初始回复数为0
        comment.setStatus(1);  // 正常状态
        
        int result = communityCommentMapper.insert(comment);
        if (result <= 0) {
            throw new BusinessException("发表评论失败");
        }
        
        // 更新帖子评论数
        communityPostMapper.updateCommentCount(postId, 1);
        
        // 更新用户评论数
        communityUserStatusService.incrementCommentCount(currentUserId);
        
        // 发送消息通知
        try {
            if (request.getParentId() == null) {
                // 对帖子的评论，通知帖子作者
                CommunityPost post = communityPostMapper.selectById(postId);
                if (post != null && !currentUserId.equals(post.getAuthorId())) {
                    notificationPublisher.publish(NotificationCommand.community(
                        post.getAuthorId(),
                        "您的帖子收到新评论",
                        "用户 " + username + " 评论了您的帖子《" + post.getTitle() + "》",
                        postId.toString()
                    ));
                }
            } else {
                // 对评论的回复，通知被回复的评论作者
                CommunityComment parentComment = communityCommentMapper.selectById(request.getParentId());
                if (parentComment != null && !currentUserId.equals(parentComment.getAuthorId())) {
                    notificationPublisher.publish(NotificationCommand.community(
                        parentComment.getAuthorId(),
                        "您的评论收到新回复",
                        "用户 " + username + " 回复了您的评论",
                        comment.getId().toString()
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("发送评论通知失败，用户ID: {}, 帖子ID: {}, 错误: {}", 
                    currentUserId, postId, e.getMessage());
        }
        
        log.info("用户发表评论成功，用户ID: {}, 帖子ID: {}, 评论ID: {}", 
                currentUserId, postId, comment.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId) {
        // 检查用户是否被封禁
        communityUserStatusService.checkUserBanStatus();
        
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 检查评论是否存在
        CommunityComment comment = getById(commentId);
        
        // 检查是否已点赞
        CommunityCommentLike existingLike = communityCommentLikeMapper.selectByCommentIdAndUserId(commentId, currentUserId);
        if (existingLike != null) {
            throw new BusinessException("已经点赞过了");
        }
        
        // 添加点赞记录
        CommunityCommentLike like = new CommunityCommentLike();
        like.setCommentId(commentId);
        like.setUserId(currentUserId);
        // Sa-Token: 从工具类获取用户名
        String username = SaTokenUserUtil.getCurrentUserUsername("用户" + currentUserId);
        like.setUserName(username);
        
        int result = communityCommentLikeMapper.insert(like);
        if (result <= 0) {
            throw new BusinessException("点赞失败");
        }
        
        // 更新评论点赞数
        communityCommentMapper.updateLikeCount(commentId, 1);

        // 更新用户点赞数统计
        communityUserStatusService.incrementLikeCount(currentUserId);

        // 发送消息通知：通知评论作者
        if (!currentUserId.equals(comment.getAuthorId())) {
            try {
                notificationPublisher.publish(NotificationCommand.community(
                    comment.getAuthorId(),
                    "您的评论收到新点赞",
                    "用户 " + username + " 点赞了您的评论",
                    commentId.toString()
                ));
            } catch (Exception e) {
                log.warn("发送评论点赞通知失败，用户ID: {}, 评论ID: {}, 错误: {}", 
                        currentUserId, commentId, e.getMessage());
            }
        }

        log.info("用户点赞评论成功，用户ID: {}, 评论ID: {}", currentUserId, commentId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 检查评论是否存在
        getById(commentId);
        
        // 检查是否已点赞
        CommunityCommentLike existingLike = communityCommentLikeMapper.selectByCommentIdAndUserId(commentId, currentUserId);
        if (existingLike == null) {
            throw new BusinessException("还没有点赞");
        }
        
        // 删除点赞记录
        int result = communityCommentLikeMapper.delete(commentId, currentUserId);
        if (result <= 0) {
            throw new BusinessException("取消点赞失败");
        }
        
        // 更新评论点赞数
        communityCommentMapper.updateLikeCount(commentId, -1);

        // 更新用户点赞数统计
        communityUserStatusService.decrementLikeCount(currentUserId);

        log.info("用户取消点赞评论成功，用户ID: {}, 评论ID: {}", currentUserId, commentId);
    }
    
    @Override
    public PageResult<CommunityCommentResponse> getUserComments(CommunityCommentQueryRequest request) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 先获取分页的原始评论数据
        PageResult<CommunityComment> pageResult = PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            return communityCommentMapper.selectUserCommentList(currentUserId, request);
        });
        
        // 在分页外进行DTO转换，避免额外查询干扰分页计数
        List<CommunityCommentResponse> responseList = pageResult.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        // 构造返回结果，保持分页信息
        return PageResult.of(
            pageResult.getPageNum(),
            pageResult.getPageSize(),
            pageResult.getTotal(),
            responseList
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyComment(Long commentId, CommunityCommentReplyRequest request) {
        // 检查用户是否被封禁
        communityUserStatusService.checkUserBanStatus();
        
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 检查被回复的评论是否存在
        CommunityComment parentComment = getById(commentId);
        
        // 敏感词检测
        String content = request.getContent();
        try {
            SensitiveWordUtils.SensitiveCheckResult sensitiveResult = 
                SensitiveWordUtils.checkText(content, "community", parentComment.getPostId(), currentUserId);
            
            if (!sensitiveResult.getAllowed()) {
                throw new BusinessException("回复包含违规内容，禁止发布");
            }
            
            content = sensitiveResult.getProcessedText();
            
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            log.warn("敏感词检测异常，使用原始内容：{}", e.getMessage());
        }
        
        // 创建回复
        CommunityComment reply = new CommunityComment();
        reply.setPostId(parentComment.getPostId());
        reply.setParentId(parentComment.getParentId() == 0 ? commentId : parentComment.getParentId()); // 都挂在一级评论下
        reply.setContent(content);
        reply.setAuthorId(currentUserId);
        String username = SaTokenUserUtil.getCurrentUserUsername("用户" + currentUserId);
        reply.setAuthorName(username);
        reply.setReplyToId(commentId);
        reply.setReplyToUserId(request.getReplyToUserId());
        
        // 查询被回复用户的用户名（从数据库中获取）
        String replyToUserName = "用户" + request.getReplyToUserId(); // 默认值
        try {
            // 从用户状态表获取用户名
            CommunityUserStatus targetUserStatus = communityUserStatusService.getByUserId(request.getReplyToUserId());
            if (targetUserStatus != null && targetUserStatus.getUserName() != null) {
                replyToUserName = targetUserStatus.getUserName();
            }
        } catch (Exception e) {
            log.warn("获取被回复用户名失败，使用默认值。用户ID: {}", request.getReplyToUserId());
        }
        reply.setReplyToUserName(replyToUserName);
        reply.setLikeCount(0);
        reply.setReplyCount(0);
        reply.setStatus(1);
        
        int result = communityCommentMapper.insert(reply);
        if (result <= 0) {
            throw new BusinessException("回复评论失败");
        }
        
        // 更新一级评论的回复数
        Long parentCommentId = parentComment.getParentId() == 0 ? commentId : parentComment.getParentId();
        communityCommentMapper.updateReplyCount(parentCommentId, 1);
        
        // 更新帖子评论数
        communityPostMapper.updateCommentCount(parentComment.getPostId(), 1);
        
        // 更新用户评论数统计
        communityUserStatusService.incrementCommentCount(currentUserId);
        
        // 发送消息通知：通知被回复的用户
        if (!currentUserId.equals(request.getReplyToUserId())) {
            try {
                notificationPublisher.publish(NotificationCommand.community(
                    request.getReplyToUserId(),
                    "您的评论收到新回复",
                    "用户 " + username + " 回复了您：" + content,
                    parentComment.getPostId().toString()
                ));
            } catch (Exception e) {
                log.warn("发送评论回复通知失败，用户ID: {}, 评论ID: {}, 错误: {}", 
                        currentUserId, commentId, e.getMessage());
            }
        }
        
        log.info("用户回复评论成功，用户ID: {}, 评论ID: {}", currentUserId, commentId);
    }
    
    @Override
    public PageResult<CommunityCommentResponse> getCommentReplies(Long commentId, CommunityCommentQueryRequest request) {
        // 检查评论是否存在
        getById(commentId);
        
        // 分页查询回复列表
        PageResult<CommunityComment> pageResult = PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            return communityCommentMapper.selectAllRepliesByCommentId(commentId);
        });
        
        // 转换为响应DTO
        List<CommunityCommentResponse> responseList = pageResult.getRecords().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return PageResult.of(
            pageResult.getPageNum(),
            pageResult.getPageSize(),
            pageResult.getTotal(),
            responseList
        );
    }
    
    /**
     * 转换为响应DTO
     */
    private CommunityCommentResponse convertToResponse(CommunityComment comment) {
        CommunityCommentResponse response = BeanUtil.copyProperties(comment, CommunityCommentResponse.class);
        
        // 设置用户点赞状态
        if (StpUserUtil.isLogin()) {
            Long userId = StpUserUtil.getLoginIdAsLong();
            CommunityCommentLike like = communityCommentLikeMapper.selectByCommentIdAndUserId(comment.getId(), userId);
            response.setIsLiked(like != null);
        } else {
            response.setIsLiked(false);
        }
        
        return response;
    }
    
    /**
     * 转换为带回复的响应DTO（用于一级评论）
     */
    private CommunityCommentResponse convertToResponseWithReplies(CommunityComment comment) {
        CommunityCommentResponse response = convertToResponse(comment);
        
        // 查询最多2条回复
        List<CommunityComment> replies = communityCommentMapper.selectRepliesByCommentId(comment.getId(), 2);
        if (replies != null && !replies.isEmpty()) {
            List<CommunityCommentResponse> replyResponses = replies.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            response.setReplies(replyResponses);
            
            // 检查是否有更多回复
            response.setHasMoreReplies(comment.getReplyCount() != null && comment.getReplyCount() > 2);
        }
        
        return response;
    }
}
