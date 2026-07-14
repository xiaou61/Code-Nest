package com.xiaou.community.service.impl;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.common.cache.RedisValueStore;
import com.xiaou.community.config.CommunityProperties;
import com.xiaou.community.domain.CommunityCategory;
import com.xiaou.community.domain.CommunityPost;
import com.xiaou.community.domain.CommunityTag;
import com.xiaou.community.service.CommunityCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 社区缓存Service实现类
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityCacheServiceImpl implements CommunityCacheService {
    
    private final RedisValueStore redisValueStore;
    private final RedissonClient redissonClient;
    private final CommunityProperties communityProperties;
    
    private static final String POST_DETAIL_KEY = "community:post:detail:";
    private static final String CATEGORIES_KEY = "community:categories:enabled";
    private static final String TAGS_KEY = "community:tags:enabled";
    private static final String USER_LIKE_KEY = "community:post:like:";
    private static final String USER_COLLECT_KEY = "community:post:collect:";
    private static final String HOT_SEARCH_KEY = "community:hot:keywords";
    
    // 热门搜索词TTL：7天
    private static final long HOT_SEARCH_TTL = 7 * 24 * 60 * 60;
    
    @Override
    public void cachePost(CommunityPost post) {
        String key = POST_DETAIL_KEY + post.getId();
        cacheWrite("cache post " + post.getId(), () -> redisValueStore.put(
                key,
                JsonUtils.toJsonString(post),
                Duration.ofSeconds(communityProperties.getCache().getPostDetailTtl())
        ));
        log.debug("缓存帖子详情，帖子ID: {}", post.getId());
    }
    
    @Override
    public CommunityPost getCachedPost(Long postId) {
        String key = POST_DETAIL_KEY + postId;
        String cachedData = cacheRead("read post " + postId,
                () -> redisValueStore.find(key, String.class).orElse(null), null);
        if (cachedData != null) {
            log.debug("从缓存获取帖子详情，帖子ID: {}", postId);
            return JsonUtils.parseObject(cachedData, CommunityPost.class);
        }
        return null;
    }
    
    @Override
    public void evictPost(Long postId) {
        String key = POST_DETAIL_KEY + postId;
        cacheWrite("evict post " + postId, () -> redisValueStore.delete(key));
        log.debug("清除帖子缓存，帖子ID: {}", postId);
    }
    
    @Override
    public void cacheCategories(List<CommunityCategory> categories) {
        cacheWrite("cache categories", () -> redisValueStore.put(
                CATEGORIES_KEY,
                JsonUtils.toJsonString(categories),
                Duration.ofSeconds(communityProperties.getCache().getTagsTtl())
        ));
        log.debug("缓存分类列表，数量: {}", categories.size());
    }
    
    @Override
    public List<CommunityCategory> getCachedCategories() {
        String cachedData = cacheRead("read categories",
                () -> redisValueStore.find(CATEGORIES_KEY, String.class).orElse(null), null);
        if (cachedData != null) {
            log.debug("从缓存获取分类列表");
            return JsonUtils.parseArray(cachedData, CommunityCategory.class);
        }
        return null;
    }
    
    @Override
    public void evictCategories() {
        cacheWrite("evict categories", () -> redisValueStore.delete(CATEGORIES_KEY));
        log.debug("清除分类缓存");
    }
    
    @Override
    public void cacheTags(List<CommunityTag> tags) {
        cacheWrite("cache tags", () -> redisValueStore.put(
                TAGS_KEY,
                JsonUtils.toJsonString(tags),
                Duration.ofSeconds(communityProperties.getCache().getTagsTtl())
        ));
        log.debug("缓存标签列表，数量: {}", tags.size());
    }
    
    @Override
    public List<CommunityTag> getCachedTags() {
        String cachedData = cacheRead("read tags",
                () -> redisValueStore.find(TAGS_KEY, String.class).orElse(null), null);
        if (cachedData != null) {
            log.debug("从缓存获取标签列表");
            return JsonUtils.parseArray(cachedData, CommunityTag.class);
        }
        return null;
    }
    
    @Override
    public void evictTags() {
        cacheWrite("evict tags", () -> redisValueStore.delete(TAGS_KEY));
        log.debug("清除标签缓存");
    }
    
    @Override
    public void cacheUserLikeStatus(Long userId, Set<Long> likedPostIds) {
        String key = USER_LIKE_KEY + userId;
        cacheWrite("cache user likes " + userId, () -> {
            redisValueStore.delete(key);
            if (likedPostIds != null && !likedPostIds.isEmpty()) {
                RSet<Object> likes = redissonClient.getSet(key);
                likedPostIds.forEach(likes::add);
                likes.expire(Duration.ofSeconds(communityProperties.getCache().getUserInfoTtl()));
            }
        });
        log.debug("缓存用户点赞状态，用户ID: {}, 点赞数: {}", userId, likedPostIds != null ? likedPostIds.size() : 0);
    }
    
    @Override
    public Set<Long> getUserLikedPostIds(Long userId) {
        String key = USER_LIKE_KEY + userId;
        Set<Object> members = cacheRead("read user likes " + userId,
                () -> redissonClient.<Object>getSet(key).readAll(), null);
        if (members != null) {
            return members.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(Collectors.toSet());
        }
        return null;
    }
    
    @Override
    public void addUserLikedPost(Long userId, Long postId) {
        String key = USER_LIKE_KEY + userId;
        cacheWrite("add user like " + userId, () -> {
            RSet<Object> likes = redissonClient.getSet(key);
            likes.add(postId);
            likes.expire(Duration.ofSeconds(communityProperties.getCache().getUserInfoTtl()));
        });
    }
    
    @Override
    public void removeUserLikedPost(Long userId, Long postId) {
        String key = USER_LIKE_KEY + userId;
        cacheWrite("remove user like " + userId, () -> redissonClient.getSet(key).remove(postId));
    }
    
    @Override
    public void cacheUserCollectStatus(Long userId, Set<Long> collectedPostIds) {
        String key = USER_COLLECT_KEY + userId;
        cacheWrite("cache user collections " + userId, () -> {
            redisValueStore.delete(key);
            if (collectedPostIds != null && !collectedPostIds.isEmpty()) {
                RSet<Object> collections = redissonClient.getSet(key);
                collectedPostIds.forEach(collections::add);
                collections.expire(Duration.ofSeconds(communityProperties.getCache().getUserInfoTtl()));
            }
        });
        log.debug("缓存用户收藏状态，用户ID: {}, 收藏数: {}", userId, collectedPostIds != null ? collectedPostIds.size() : 0);
    }
    
    @Override
    public Set<Long> getUserCollectedPostIds(Long userId) {
        String key = USER_COLLECT_KEY + userId;
        Set<Object> members = cacheRead("read user collections " + userId,
                () -> redissonClient.<Object>getSet(key).readAll(), null);
        if (members != null) {
            return members.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .collect(Collectors.toSet());
        }
        return null;
    }
    
    @Override
    public void addUserCollectedPost(Long userId, Long postId) {
        String key = USER_COLLECT_KEY + userId;
        cacheWrite("add user collection " + userId, () -> {
            RSet<Object> collections = redissonClient.getSet(key);
            collections.add(postId);
            collections.expire(Duration.ofSeconds(communityProperties.getCache().getUserInfoTtl()));
        });
    }
    
    @Override
    public void removeUserCollectedPost(Long userId, Long postId) {
        String key = USER_COLLECT_KEY + userId;
        cacheWrite("remove user collection " + userId, () -> redissonClient.getSet(key).remove(postId));
    }
    
    @Override
    public void recordSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        
        keyword = keyword.trim();
        
        try {
            // 获取当前分数，如果不存在则为0
            RScoredSortedSet<Object> hotSearches = redissonClient.getScoredSortedSet(HOT_SEARCH_KEY);
            Double currentScore = hotSearches.getScore(keyword);
            double newScore = (currentScore != null ? currentScore : 0) + 1;
            
            // 使用Sorted Set记录搜索词，score为搜索次数
            hotSearches.add(newScore, keyword);
            
            // 设置过期时间
            hotSearches.expire(Duration.ofSeconds(HOT_SEARCH_TTL));
            log.debug("记录搜索关键词: {}, 搜索次数: {}", keyword, newScore);
        } catch (Exception e) {
            log.error("记录搜索关键词失败: {}", keyword, e);
        }
    }
    
    @Override
    public List<String> getHotSearchKeywords(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        
        try {
            // 获取score最高的N个关键词（倒序）
            Collection<Object> keywords = redissonClient.<Object>getScoredSortedSet(HOT_SEARCH_KEY)
                    .valueRangeReversed(0, limit - 1);
            if (keywords != null && !keywords.isEmpty()) {
                List<String> result = keywords.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
                log.debug("获取热门搜索词，数量: {}", result.size());
                return result;
            }
        } catch (Exception e) {
            log.error("获取热门搜索词失败", e);
        }
        
        return Collections.emptyList();
    }
    
    @Override
    public void clearHotSearchKeywords() {
        try {
            redisValueStore.delete(HOT_SEARCH_KEY);
            log.debug("清除热门搜索词缓存");
        } catch (Exception e) {
            log.error("清除热门搜索词缓存失败", e);
        }
    }

    private void cacheWrite(String operation, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            log.warn("社区缓存写入失败: {}", operation, error);
        }
    }

    private <T> T cacheRead(String operation, Supplier<T> action, T fallback) {
        try {
            return action.get();
        } catch (RuntimeException error) {
            log.warn("社区缓存读取失败: {}", operation, error);
            return fallback;
        }
    }
}

