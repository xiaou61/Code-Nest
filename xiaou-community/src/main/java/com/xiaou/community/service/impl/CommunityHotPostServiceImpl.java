package com.xiaou.community.service.impl;

import com.xiaou.community.config.CommunityProperties;
import com.xiaou.community.domain.CommunityPost;
import com.xiaou.community.dto.CommunityPostResponse;
import com.xiaou.community.mapper.CommunityPostMapper;
import com.xiaou.community.service.CommunityHotPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 社区热门帖子Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityHotPostServiceImpl implements CommunityHotPostService {
    
    private final CommunityPostMapper communityPostMapper;
    private final CommunityProperties communityProperties;
    private final RedissonClient redissonClient;
    
    private static final String HOT_POSTS_KEY = "community:hot:posts";
    
    @Override
    public List<CommunityPostResponse> getHotPosts(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = communityProperties.getHot().getLimit();
        }
        
        try {
            // 从Redis获取热门帖子ID列表（带分数）
            Map<Object, Double> hotPostMap = getHotPostScores(limit);
            
            if (hotPostMap == null || hotPostMap.isEmpty()) {
                // 缓存为空，刷新缓存
                log.info("热门帖子缓存为空，开始刷新");
                refreshHotPosts();
                // 重新获取
                hotPostMap = getHotPostScores(limit);
            }
            
            if (hotPostMap == null || hotPostMap.isEmpty()) {
                log.warn("刷新后仍无热门帖子");
                return new ArrayList<>();
            }
            
            // 批量查询帖子详情，避免N+1问题
            List<Long> postIds = hotPostMap.keySet().stream()
                    .map(key -> Long.valueOf(key.toString()))
                    .collect(Collectors.toList());
            
            List<CommunityPost> posts = communityPostMapper.selectBatchIds(postIds);
            Map<Long, CommunityPost> postMap = posts.stream()
                    .collect(Collectors.toMap(CommunityPost::getId, p -> p));
            
            List<CommunityPostResponse> hotPosts = new ArrayList<>();
            for (Map.Entry<Object, Double> entry : hotPostMap.entrySet()) {
                Long postId = Long.valueOf(entry.getKey().toString());
                CommunityPost post = postMap.get(postId);
                if (post != null && post.getStatus() == 1) {
                    CommunityPostResponse response = convertToResponse(post);
                    response.setHotScore(entry.getValue());
                    hotPosts.add(response);
                }
            }
            
            return hotPosts;
            
        } catch (Exception e) {
            log.error("获取热门帖子失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public void refreshHotPosts() {
        try {
            log.info("开始刷新热门帖子缓存");
            
            // 计算时间范围
            Integer timeRange = communityProperties.getHot().getTimeRange();
            Date startTime = new Date(System.currentTimeMillis() - timeRange * 60 * 60 * 1000L);
            
            // 查询符合条件的帖子
            List<CommunityPost> posts = communityPostMapper.selectHotPosts(
                startTime, 
                communityProperties.getHot().getMinScore(),
                communityProperties.getHot().getLimit() * 2 // 查询2倍数量，避免过滤后不够
            );
            
            if (posts == null || posts.isEmpty()) {
                log.info("没有符合条件的热门帖子");
                return;
            }
            
            // 清空旧缓存
            RScoredSortedSet<Object> hotPosts = redissonClient.getScoredSortedSet(HOT_POSTS_KEY);
            hotPosts.clear();
            
            // 计算热度分数并存入Redis
            for (CommunityPost post : posts) {
                Double hotScore = calculateHotScoreInternal(post);
                if (hotScore >= communityProperties.getHot().getMinScore()) {
                    hotPosts.add(hotScore, post.getId());
                }
            }
            
            // 保留TOP N
            long size = hotPosts.size();
            if (size > communityProperties.getHot().getLimit()) {
                hotPosts.removeRangeByRank(0, (int) (size - communityProperties.getHot().getLimit() - 1));
            }
            
            // 设置过期时间
            hotPosts.expire(Duration.ofSeconds(communityProperties.getCache().getHotPostsTtl()));
            
            log.info("热门帖子缓存刷新完成，共{}个帖子", hotPosts.size());
            
        } catch (Exception e) {
            log.error("刷新热门帖子缓存失败", e);
        }
    }
    
    @Override
    public Double calculateHotScore(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            return 0.0;
        }
        return calculateHotScoreInternal(post);
    }
    
    /**
     * 计算帖子热度分数
     * 热度分数 = 点赞数 × 3 + 评论数 × 5 + 收藏数 × 8 + 浏览数 × 0.1 - 时间衰减
     * 时间衰减 = (当前时间 - 发布时间) / 24小时 × 10
     */
    private Double calculateHotScoreInternal(CommunityPost post) {
        // 基础分数
        double score = 0.0;
        score += (post.getLikeCount() != null ? post.getLikeCount() : 0) * 3.0;
        score += (post.getCommentCount() != null ? post.getCommentCount() : 0) * 5.0;
        score += (post.getCollectCount() != null ? post.getCollectCount() : 0) * 8.0;
        score += (post.getViewCount() != null ? post.getViewCount() : 0) * 0.1;
        
        // 计算时间衰减
        long timeDiff = System.currentTimeMillis() - post.getCreateTime().getTime();
        double hours = timeDiff / (1000.0 * 60 * 60);
        double timeDecay = (hours / 24.0) * 10.0;
        
        // 最终分数
        double finalScore = score - timeDecay;
        return Math.max(finalScore, 0.0);
    }

    private Map<Object, Double> getHotPostScores(int limit) {
        RScoredSortedSet<Object> hotPosts = redissonClient.getScoredSortedSet(HOT_POSTS_KEY);
        Map<Object, Double> result = new LinkedHashMap<>();
        for (Object value : hotPosts.valueRangeReversed(0, limit - 1)) {
            result.put(value, hotPosts.getScore(value));
        }
        return result;
    }
    
    /**
     * 简化的转换方法（不包含用户状态）
     */
    private CommunityPostResponse convertToResponse(CommunityPost post) {
        CommunityPostResponse response = new CommunityPostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setCategoryId(post.getCategoryId());
        response.setCategoryName(post.getCategoryName());
        response.setAuthorId(post.getAuthorId());
        response.setAuthorName(post.getAuthorName());
        response.setViewCount(post.getViewCount());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setCollectCount(post.getCollectCount());
        response.setIsTop(post.getIsTop() != null && post.getIsTop() == 1);
        response.setCreateTime(post.getCreateTime());
        response.setIsLiked(false);
        response.setIsCollected(false);
        return response;
    }
}

