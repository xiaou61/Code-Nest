package com.xiaou.community.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.SaTokenUserUtil;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.common.utils.DateHelper;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.community.domain.CommunityCategory;
import com.xiaou.community.domain.CommunityPost;
import com.xiaou.community.domain.CommunityPostCollect;
import com.xiaou.community.domain.CommunityPostLike;
import com.xiaou.community.domain.CommunityPostTag;
import com.xiaou.community.domain.CommunityTag;
import com.xiaou.community.dto.*;
import com.xiaou.community.mapper.CommunityPostCollectMapper;
import com.xiaou.community.mapper.CommunityPostLikeMapper;
import com.xiaou.community.mapper.CommunityPostMapper;
import com.xiaou.community.mapper.CommunityPostTagMapper;
import com.xiaou.community.mapper.CommunityTagMapper;
import com.xiaou.community.service.CommunityCacheService;
import com.xiaou.community.service.CommunityCategoryService;
import com.xiaou.community.service.CommunityPostService;
import com.xiaou.community.service.CommunityUserStatusService;
import com.xiaou.notification.api.NotificationCommand;
import com.xiaou.notification.api.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 社区帖子Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityPostServiceImpl implements CommunityPostService {
    
    private final CommunityPostMapper communityPostMapper;
    private final CommunityPostLikeMapper communityPostLikeMapper;
    private final CommunityPostCollectMapper communityPostCollectMapper;
    private final CommunityUserStatusService communityUserStatusService;
    private final CommunityCategoryService communityCategoryService;
    private final CommunityPostTagMapper communityPostTagMapper;
    private final CommunityTagMapper communityTagMapper;
    private final CommunityCacheService communityCacheService;
    private final NotificationPublisher notificationPublisher;
    
    @Override
    public PageResult<CommunityPost> getAdminPostList(AdminPostQueryRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> 
            communityPostMapper.selectAdminPostList(request)
        );
    }
    
    @Override
    public CommunityPost getById(Long id) {
        CommunityPost post = communityPostMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        return post;
    }
    
    @Override
    public void topPost(Long id, Integer duration) {
        // 检查帖子是否存在
        getById(id);
        
        // 计算置顶过期时间
        Date expireTime = DateHelper.addHoursFromNow(duration);
        String expireTimeStr = DateHelper.formatDateTime(expireTime);
        
        int result = communityPostMapper.topPost(id, expireTimeStr);
        if (result <= 0) {
            throw new BusinessException("置顶帖子失败");
        }
        
        log.info("帖子置顶成功，帖子ID: {}, 置顶时长: {}小时", id, duration);
    }
    
    @Override
    public void cancelTop(Long id) {
        // 检查帖子是否存在
        getById(id);
        
        int result = communityPostMapper.cancelTop(id);
        if (result <= 0) {
            throw new BusinessException("取消置顶失败");
        }
        
        log.info("取消帖子置顶成功，帖子ID: {}", id);
    }
    
    @Override
    public void disablePost(Long id) {
        // 检查帖子是否存在
        getById(id);
        
        int result = communityPostMapper.disablePost(id);
        if (result <= 0) {
            throw new BusinessException("下架帖子失败");
        }
        
        // 清除帖子缓存
        communityCacheService.evictPost(id);
        
        log.info("帖子下架成功，帖子ID: {}", id);
    }
    
    @Override
    public void deletePost(Long id) {
        CommunityPost post = getById(id);
        
        int result = communityPostMapper.deletePost(id);
        if (result <= 0) {
            throw new BusinessException("删除帖子失败");
        }
        
        // 清除帖子缓存
        communityCacheService.evictPost(id);
        
        // 减少用户发帖数统计
        communityUserStatusService.decrementPostCount(post.getAuthorId());
        
        log.info("帖子删除成功，帖子ID: {}", id);
    }
    
    @Override
    public List<CommunityPost> getPostsByUserId(Long userId) {
        return communityPostMapper.selectByUserId(userId);
    }
    
    // 前台接口实现
    
    @Override
    public PageResult<CommunityPostResponse> getPostList(CommunityPostQueryRequest request) {
        log.info("分页查询帖子列表，查询条件: {}", request);
        
        // 记录搜索关键词
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            try {
                communityCacheService.recordSearchKeyword(request.getKeyword());
            } catch (Exception e) {
                log.warn("记录搜索关键词失败: {}", request.getKeyword(), e);
            }
        }
        
        try {
            // 先获取分页的原始帖子数据
            PageResult<CommunityPost> pageResult = PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
                List<CommunityPost> posts = communityPostMapper.selectPostList(request);
                log.info("查询到帖子数量: {}", posts.size());
                return posts;
            });
            
            // 在分页外进行DTO转换，避免额外查询干扰分页计数
            List<CommunityPostResponse> responseList = pageResult.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            // 构造返回结果，保持分页信息
            return PageResult.of(
                pageResult.getPageNum(),
                pageResult.getPageSize(),
                pageResult.getTotal(),
                responseList
            );
        } catch (Exception e) {
            log.error("分页查询帖子列表失败", e);
            throw new BusinessException("查询帖子列表失败");
        }
    }
    
    @Override
    public CommunityPostResponse getPostDetail(Long id) {
        // 先尝试从缓存获取
        CommunityPost post = communityCacheService.getCachedPost(id);
        if (post == null) {
            // 缓存未命中，从数据库查询
            post = getById(id);
            // 写入缓存
            communityCacheService.cachePost(post);
        }
        
        // 增加浏览次数（异步或后台处理，不影响缓存）
        communityPostMapper.incrementViewCount(id);
        
        return convertToResponse(post);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(CommunityPostCreateRequest request) {
        // 检查用户是否被封禁
        communityUserStatusService.checkUserBanStatus();
        
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 如果指定了分类ID，需要验证分类是否存在且启用
        if (request.getCategoryId() != null) {
            CommunityCategory category = communityCategoryService.getById(request.getCategoryId());
            if (category.getStatus() != 1) {
                throw new BusinessException("所选分类已被禁用");
            }
        }
        
        CommunityPost post = new CommunityPost();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategoryId(request.getCategoryId());
        post.setAuthorId(currentUserId);
        // Sa-Token: 从工具类获取用户名
        String username = SaTokenUserUtil.getCurrentUserUsername("用户" + currentUserId);
        post.setAuthorName(username);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        post.setIsTop(0);
        post.setStatus(1);  // 正常状态
        
        int result = communityPostMapper.insert(post);
        if (result <= 0) {
            throw new BusinessException("发布帖子失败");
        }
        
        // 更新用户发帖数
        communityUserStatusService.incrementPostCount(currentUserId);
        
        // 更新分类下的帖子数量
        if (request.getCategoryId() != null) {
            communityCategoryService.updatePostCount(request.getCategoryId(), 1);
        }
        
        // 处理标签关联
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            // 限制最多5个标签
            List<Long> tagIds = request.getTagIds();
            if (tagIds.size() > 5) {
                tagIds = tagIds.subList(0, 5);
            }
            
            // 验证标签是否存在且启用
            List<CommunityPostTag> postTags = new java.util.ArrayList<>();
            for (Long tagId : tagIds) {
                CommunityTag tag = communityTagMapper.selectById(tagId);
                if (tag != null && tag.getStatus() == 1) {
                    CommunityPostTag postTag = new CommunityPostTag();
                    postTag.setPostId(post.getId());
                    postTag.setTagId(tagId);
                    postTags.add(postTag);
                }
            }
            
            // 批量插入标签关联
            if (!postTags.isEmpty()) {
                communityPostTagMapper.batchInsert(postTags);
                // 更新标签帖子数量
                for (CommunityPostTag postTag : postTags) {
                    communityTagMapper.updatePostCount(postTag.getTagId(), 1);
                }
            }
        }
        
        log.info("用户发布帖子成功，用户ID: {}, 帖子ID: {}, 分类ID: {}", currentUserId, post.getId(), request.getCategoryId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long id) {
        // 检查用户是否被封禁
        communityUserStatusService.checkUserBanStatus();
        
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 检查帖子是否存在
        CommunityPost post = getById(id);
        
        // 检查是否已点赞
        CommunityPostLike existingLike = communityPostLikeMapper.selectByPostIdAndUserId(id, currentUserId);
        if (existingLike != null) {
            throw new BusinessException("已经点赞过了");
        }
        
        // 添加点赞记录
        CommunityPostLike like = new CommunityPostLike();
        like.setPostId(id);
        like.setUserId(currentUserId);
        // Sa-Token: 从工具类获取用户名
        String username = SaTokenUserUtil.getCurrentUserUsername("用户" + currentUserId);
        like.setUserName(username);
        
        int result = communityPostLikeMapper.insert(like);
        if (result <= 0) {
            throw new BusinessException("点赞失败");
        }
        
        // 更新帖子点赞数
        communityPostMapper.updateLikeCount(id, 1);
        
        // 更新用户点赞数统计
        communityUserStatusService.incrementLikeCount(currentUserId);
        
        // 发送消息通知：通知帖子作者
        if (!currentUserId.equals(post.getAuthorId())) {
            try {
                notificationPublisher.publish(NotificationCommand.community(
                    post.getAuthorId(),
                    "您的帖子收到新点赞",
                    "用户 " + username + " 点赞了您的帖子《" + post.getTitle() + "》",
                    id.toString()
                ));
            } catch (Exception e) {
                log.warn("发送帖子点赞通知失败，用户ID: {}, 帖子ID: {}, 错误: {}", 
                        currentUserId, id, e.getMessage());
            }
        }
        
        log.info("用户点赞帖子成功，用户ID: {}, 帖子ID: {}", currentUserId, id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long id) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 检查帖子是否存在
        getById(id);
        
        // 检查是否已点赞
        CommunityPostLike existingLike = communityPostLikeMapper.selectByPostIdAndUserId(id, currentUserId);
        if (existingLike == null) {
            throw new BusinessException("还没有点赞");
        }
        
        // 删除点赞记录
        int result = communityPostLikeMapper.delete(id, currentUserId);
        if (result <= 0) {
            throw new BusinessException("取消点赞失败");
        }
        
        // 更新帖子点赞数
        communityPostMapper.updateLikeCount(id, -1);
        
        // 更新用户点赞数统计
        communityUserStatusService.decrementLikeCount(currentUserId);
        
        log.info("用户取消点赞帖子成功，用户ID: {}, 帖子ID: {}", currentUserId, id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectPost(Long id) {
        // 检查用户是否被封禁
        communityUserStatusService.checkUserBanStatus();
        
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 检查帖子是否存在
        CommunityPost post = getById(id);
        
        // 检查是否已收藏
        CommunityPostCollect existingCollect = communityPostCollectMapper.selectByPostIdAndUserId(id, currentUserId);
        if (existingCollect != null) {
            throw new BusinessException("已经收藏过了");
        }
        
        // 添加收藏记录
        CommunityPostCollect collect = new CommunityPostCollect();
        collect.setPostId(id);
        collect.setUserId(currentUserId);
        // Sa-Token: 从工具类获取用户名
        String username = SaTokenUserUtil.getCurrentUserUsername("用户" + currentUserId);
        collect.setUserName(username);
        
        int result = communityPostCollectMapper.insert(collect);
        if (result <= 0) {
            throw new BusinessException("收藏失败");
        }
        
        // 更新帖子收藏数
        communityPostMapper.updateCollectCount(id, 1);
        
        // 更新用户收藏数
        communityUserStatusService.incrementCollectCount(currentUserId);
        
        // 发送消息通知：通知帖子作者
        if (!currentUserId.equals(post.getAuthorId())) {
            try {
                notificationPublisher.publish(NotificationCommand.community(
                    post.getAuthorId(),
                    "您的帖子被收藏",
                    "用户 " + username + " 收藏了您的帖子《" + post.getTitle() + "》",
                    id.toString()
                ));
            } catch (Exception e) {
                log.warn("发送帖子收藏通知失败，用户ID: {}, 帖子ID: {}, 错误: {}", 
                        currentUserId, id, e.getMessage());
            }
        }
        
        log.info("用户收藏帖子成功，用户ID: {}, 帖子ID: {}", currentUserId, id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectPost(Long id) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 检查帖子是否存在
        getById(id);
        
        // 检查是否已收藏
        CommunityPostCollect existingCollect = communityPostCollectMapper.selectByPostIdAndUserId(id, currentUserId);
        if (existingCollect == null) {
            throw new BusinessException("还没有收藏");
        }
        
        // 删除收藏记录
        int result = communityPostCollectMapper.delete(id, currentUserId);
        if (result <= 0) {
            throw new BusinessException("取消收藏失败");
        }
        
        // 更新帖子收藏数
        communityPostMapper.updateCollectCount(id, -1);
        
        // 更新用户收藏数
        communityUserStatusService.decrementCollectCount(currentUserId);
        
        log.info("用户取消收藏帖子成功，用户ID: {}, 帖子ID: {}", currentUserId, id);
    }
    
    @Override
    public PageResult<CommunityPostResponse> getUserCollections(CommunityPostQueryRequest request) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 先获取分页的原始帖子数据
        PageResult<CommunityPost> pageResult = PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            return communityPostMapper.selectUserCollectionList(currentUserId, request);
        });
        
        // 在分页外进行DTO转换，避免额外查询干扰分页计数
        List<CommunityPostResponse> responseList = pageResult.getRecords().stream()
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
    public PageResult<CommunityPostResponse> getUserPosts(CommunityPostQueryRequest request) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 先获取分页的原始帖子数据
        PageResult<CommunityPost> pageResult = PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> {
            return communityPostMapper.selectUserPostList(currentUserId, request);
        });
        
        // 在分页外进行DTO转换，避免额外查询干扰分页计数
        List<CommunityPostResponse> responseList = pageResult.getRecords().stream()
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
    
    /**
     * 转换为响应DTO
     */
    private CommunityPostResponse convertToResponse(CommunityPost post) {
        CommunityPostResponse response = BeanUtil.copyProperties(post, CommunityPostResponse.class);
        response.setIsTop(post.getIsTop() != null && post.getIsTop() == 1);
        
        // 查询帖子的标签列表，使用批量查询避免N+1问题
        List<Long> tagIds = communityPostTagMapper.selectTagIdsByPostId(post.getId());
        if (tagIds != null && !tagIds.isEmpty()) {
            List<CommunityTag> tags = communityTagMapper.selectBatchIds(tagIds);
            response.setTags(tags != null ? tags : new java.util.ArrayList<>());
        }
        
        // 设置AI摘要
        response.setAiSummary(post.getAiSummary());
        
        // 计算热度分数
        double hotScore = post.getLikeCount() * 3.0 + post.getCommentCount() * 5.0 
                        + post.getCollectCount() * 8.0 + post.getViewCount() * 0.1;
        response.setHotScore(hotScore);
        
        // 设置用户相关状态
        if (StpUserUtil.isLogin()) {
            Long userId = StpUserUtil.getLoginIdAsLong();
            // 检查是否点赞
            CommunityPostLike like = communityPostLikeMapper.selectByPostIdAndUserId(post.getId(), userId);
            response.setIsLiked(like != null);
            
            // 检查是否收藏
            CommunityPostCollect collect = communityPostCollectMapper.selectByPostIdAndUserId(post.getId(), userId);
            response.setIsCollected(collect != null);
        } else {
            response.setIsLiked(false);
            response.setIsCollected(false);
        }
        
        return response;
    }
}
