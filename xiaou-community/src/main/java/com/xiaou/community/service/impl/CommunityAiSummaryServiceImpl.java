package com.xiaou.community.service.impl;


import com.xiaou.ai.dto.community.PostSummaryResult;
import com.xiaou.ai.service.AiCommunityService;
import com.xiaou.common.cache.CacheStore;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.utils.JsonUtils;
import com.xiaou.community.config.CommunityProperties;
import com.xiaou.community.domain.CommunityPost;
import com.xiaou.community.mapper.CommunityPostMapper;
import com.xiaou.community.service.CommunityAiSummaryService;
import com.xiaou.community.service.CommunityCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.time.Duration;

/**
 * 社区AI摘要Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityAiSummaryServiceImpl implements CommunityAiSummaryService {
    
    private final CommunityPostMapper communityPostMapper;
    private final CommunityProperties communityProperties;
    private final AiCommunityService aiCommunityService;
    private final CacheStore cacheStore;
    private final CommunityCacheService communityCacheService;
    
    private static final String SUMMARY_CACHE_KEY = "community:post:summary:";
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateSummary(Long postId, boolean forceRefresh) {
        // 检查是否启用AI功能
        if (!communityProperties.getAi().getEnabled()) {
            throw new BusinessException("AI功能未启用");
        }
        
        // 查询帖子
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        
        String cacheKey = SUMMARY_CACHE_KEY + postId;
        
        // 如果不是强制刷新，先从缓存获取
        if (!forceRefresh) {
            String cachedData = getCachedSummary(cacheKey);
            if (cachedData != null) {
                log.info("从缓存获取帖子摘要，帖子ID: {}", postId);
                return JsonUtils.parseMap(cachedData);
            }
            
            // 缓存不存在，从数据库获取
            if (post.getAiSummary() != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("summary", post.getAiSummary());
                result.put("keywords", post.getAiKeywords() != null ? 
                    post.getAiKeywords().split(",") : new String[0]);
                
                // 回填缓存
                cacheSummary(cacheKey, result);
                
                log.info("从数据库获取帖子摘要，帖子ID: {}", postId);
                return result;
            }
        }
        
        // 调用AI服务生成摘要
        try {
            log.info("调用AI服务生成摘要，帖子ID: {}", postId);
            
            // 调用AI服务
            PostSummaryResult aiResult = aiCommunityService.generatePostSummary(
                post.getTitle(), post.getContent());
            
            if (aiResult.isFallback()) {
                log.warn("AI服务返回降级结果，帖子ID: {}", postId);
            }
            
            String summary = aiResult.getSummary();
            String[] keywords = aiResult.getKeywords() != null 
                ? aiResult.getKeywords().toArray(new String[0]) 
                : new String[0];
            
            // 保存到数据库
            String keywordsStr = String.join(",", keywords);
            communityPostMapper.updateAiSummary(postId, summary, keywordsStr);
            
            // 删除帖子详情缓存，确保下次查询时返回最新数据
            communityCacheService.evictPost(postId);
            
            // 保存到AI摘要缓存
            Map<String, Object> result = new HashMap<>();
            result.put("summary", summary);
            result.put("keywords", keywords);
            
            cacheSummary(cacheKey, result);
            
            log.info("AI摘要生成成功并已清除帖子缓存，帖子ID: {}", postId);
            return result;
            
        } catch (Exception e) {
            log.error("生成AI摘要失败，帖子ID: {}", postId, e);
            throw new BusinessException("生成AI摘要失败：" + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> getSummary(Long postId) {
        String cacheKey = SUMMARY_CACHE_KEY + postId;
        
        // 先从缓存获取
        String cachedData = getCachedSummary(cacheKey);
        if (cachedData != null) {
            return JsonUtils.parseMap(cachedData);
        }
        
        // 从数据库获取
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null || post.getAiSummary() == null) {
            return null;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("summary", post.getAiSummary());
        result.put("keywords", post.getAiKeywords() != null ? 
            post.getAiKeywords().split(",") : new String[0]);
        
        // 回填缓存
        cacheSummary(cacheKey, result);
        
        return result;
    }

    private String getCachedSummary(String cacheKey) {
        try {
            return cacheStore.find(cacheKey, String.class).orElse(null);
        } catch (RuntimeException error) {
            log.warn("读取帖子摘要缓存失败，key={}", cacheKey, error);
            return null;
        }
    }

    private void cacheSummary(String cacheKey, Map<String, Object> value) {
        try {
            cacheStore.put(
                    cacheKey,
                    JsonUtils.toJsonString(value),
                    Duration.ofSeconds(communityProperties.getAi().getSummaryCacheTtl())
            );
        } catch (RuntimeException error) {
            log.warn("写入帖子摘要缓存失败，key={}", cacheKey, error);
        }
    }
}
