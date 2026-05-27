package com.xiaou.moyu.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.utils.RedisUtil;
import com.xiaou.common.utils.ThreadPoolUtils;
import com.xiaou.moyu.domain.HotTopicData;
import com.xiaou.moyu.domain.HotTopicResponse;
import com.xiaou.moyu.enums.HotTopicEnum;
import com.xiaou.moyu.service.HotTopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 热榜服务实现类
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotTopicServiceImpl implements HotTopicService {
    
    private final RedisUtil redisUtil;
    private final RestTemplate restTemplate;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    
    /**
     * 热榜API基础URL
     */
    @Value("${hot-topic.api.base-url:http://113.44.190.45:9996/api}")
    private String baseUrl;

    @Value("${hot-topic.api.cache-expire-minutes:15}")
    private long cacheExpireMinutes;

    @Value("${hot-topic.api.stale-cache-expire-minutes:1440}")
    private long staleCacheExpireMinutes;
    
    /**
     * Redis缓存键前缀
     */
    private static final String CACHE_KEY_PREFIX = "hot_topics:";
    private static final String STALE_CACHE_KEY_PREFIX = "hot_topics:stale:";
    
    @Override
    public HotTopicResponse getHotTopicCategories() {
        try {
            // 先从缓存获取
            String cacheKey = CACHE_KEY_PREFIX + "categories";
            String cachedData = getCachedData(cacheKey);
            
            if (StrUtil.isNotBlank(cachedData)) {
                return JSONUtil.toBean(cachedData, HotTopicResponse.class);
            }
            
            // 缓存不存在，调用API
            HotTopicResponse response = restTemplate.getForObject(baseUrl, HotTopicResponse.class);
            
            if (response != null) {
                // 存入缓存
                cacheHotTopicData(cacheKey, JSONUtil.toJsonStr(response));
            }
            
            return response;
        } catch (Exception e) {
            log.error("获取热榜分类信息失败", e);
            return getStaleData(CACHE_KEY_PREFIX + "categories", HotTopicResponse.class);
        }
    }
    
    @Override
    public HotTopicData getHotTopicData(String platform) {
        try {
            // 先从缓存获取
            String cacheKey = CACHE_KEY_PREFIX + "data:" + platform;
            String cachedData = getCachedData(cacheKey);
            
            if (StrUtil.isNotBlank(cachedData)) {
                return JSONUtil.toBean(cachedData, HotTopicData.class);
            }
            
            // 缓存不存在，调用API
            String url = baseUrl + "/" + platform;
            HotTopicData data = restTemplate.getForObject(url, HotTopicData.class);
            
            if (data != null) {
                // 存入缓存
                cacheHotTopicData(cacheKey, JSONUtil.toJsonStr(data));
            }
            
            return data;
        } catch (Exception e) {
            log.error("获取热榜数据失败, platform: {}", platform, e);
            return getStaleData(CACHE_KEY_PREFIX + "data:" + platform, HotTopicData.class);
        }
    }
    
    @Override
    public Map<String, HotTopicData> getAllHotTopicData() {
        // 使用ThreadPoolUtils并行获取所有平台数据
        List<HotTopicEnum> platforms = Arrays.asList(HotTopicEnum.values());
        
        List<Map.Entry<String, HotTopicData>> results = ThreadPoolUtils.parallelMapIO(
                platforms,
                platform -> {
                    try {
                        HotTopicData data = getHotTopicData(platform.getCode());
                        return data != null ? Map.entry(platform.getCode(), data) : null;
                    } catch (Exception e) {
                        log.error("获取平台热榜数据失败: {}", platform.getCode(), e);
                        return null;
                    }
                }
        );
        
        return results.stream()
                .filter(entry -> entry != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    @Override
    @Async("hotTopicExecutor")
    public void refreshHotTopicData() {
        if (!refreshing.compareAndSet(false, true)) {
            log.info("热榜数据刷新正在执行，跳过本次触发");
            return;
        }
        int totalCount = HotTopicEnum.values().length;
        
        try {
            // 使用ThreadPoolUtils并行刷新所有平台数据，支持超时控制
            List<HotTopicEnum> platforms = Arrays.asList(HotTopicEnum.values());
            
            List<Boolean> results = ThreadPoolUtils.parallelMapWithTimeout(
                    platforms,
                    platform -> {
                        try {
                            String url = baseUrl + "/" + platform.getCode();
                            HotTopicData data = restTemplate.getForObject(url, HotTopicData.class);
                            
                            if (data != null) {
                                String cacheKey = CACHE_KEY_PREFIX + "data:" + platform.getCode();
                                cacheHotTopicData(cacheKey, JSONUtil.toJsonStr(data));
                                return true;
                            }
                            return false;
                        } catch (Exception e) {
                            log.warn("刷新平台[{}]热榜数据失败: {}", platform.getCode(), e.getMessage());
                            return false;
                        }
                    },
                    30, TimeUnit.SECONDS
            );
            
            // 统计结果
            long successCount = results.stream().filter(r -> r != null && r).count();
            int failedCount = totalCount - (int) successCount;
            
            if (failedCount == 0) {
                log.info("热榜数据刷新成功（{}/{})", successCount, totalCount);
            } else {
                log.warn("热榜数据刷新部分失败（成功:{}/失败:{}/总数:{})", successCount, failedCount, totalCount);
            }
        } catch (Exception e) {
            log.error("热榜数据刷新异常: {}", e.getMessage());
        } finally {
            refreshing.set(false);
        }
    }
    
    @Override
    public void initializeHotTopicDataIfNeeded() {
        try {
            // 检查缓存是否已存在数据
            boolean hasCache = false;
            
            // 检查几个主要平台的缓存情况
            String[] checkPlatforms = {"weibo", "zhihu", "douyin", "kuaishou"};
            for (String platform : checkPlatforms) {
                String cacheKey = CACHE_KEY_PREFIX + "data:" + platform;
                if (redisUtil.hasKey(cacheKey)) {
                    hasCache = true;
                    break;
                }
            }
            
            if (hasCache) {
                log.info("热榜数据缓存已存在，跳过初始化");
                return;
            }
            
            log.info("热榜数据缓存为空，开始初始化数据");
            
            // 只有当缓存为空时才进行数据初始化
            int successCount = 0;
            int totalCount = HotTopicEnum.values().length;
            
            // 使用串行方式初始化，避免启动时大量并发请求
            for (HotTopicEnum platform : HotTopicEnum.values()) {
                try {
                    String cacheKey = CACHE_KEY_PREFIX + "data:" + platform.getCode();
                    // 再次检查单个平台缓存，防止在循环过程中其他地方设置了缓存
                    if (!redisUtil.hasKey(cacheKey)) {
                        String url = baseUrl + "/" + platform.getCode();
                        HotTopicData data = restTemplate.getForObject(url, HotTopicData.class);
                        
                        if (data != null) {
                            cacheHotTopicData(cacheKey, JSONUtil.toJsonStr(data));
                            successCount++;
                        }
                    } else {
                        successCount++; // 缓存已存在也算成功
                    }
                    
                    // 添加短暂延迟，避免请求过于频繁
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.warn("初始化平台[{}]热榜数据失败: {}", platform.getCode(), e.getMessage());
                }
            }
            
            log.info("热榜数据初始化完成（成功:{}/总数:{})", successCount, totalCount);
            
        } catch (Exception e) {
            log.error("热榜数据初始化异常: {}", e.getMessage());
        }
    }

    private String getCachedData(String cacheKey) {
        Object cachedData = redisUtil.get(cacheKey);
        return cachedData instanceof String ? (String) cachedData : null;
    }

    private void cacheHotTopicData(String cacheKey, String jsonData) {
        redisUtil.set(cacheKey, jsonData, cacheExpireSeconds());
        redisUtil.set(toStaleCacheKey(cacheKey), jsonData, staleCacheExpireSeconds());
    }

    private <T> T getStaleData(String cacheKey, Class<T> clazz) {
        String staleData = getCachedData(toStaleCacheKey(cacheKey));
        if (StrUtil.isBlank(staleData)) {
            return null;
        }
        log.warn("使用热榜兜底缓存: {}", cacheKey);
        return JSONUtil.toBean(staleData, clazz);
    }

    private String toStaleCacheKey(String cacheKey) {
        if (cacheKey.startsWith(CACHE_KEY_PREFIX)) {
            return STALE_CACHE_KEY_PREFIX + cacheKey.substring(CACHE_KEY_PREFIX.length());
        }
        return STALE_CACHE_KEY_PREFIX + cacheKey;
    }

    private long cacheExpireSeconds() {
        return Math.max(1, cacheExpireMinutes) * 60;
    }

    private long staleCacheExpireSeconds() {
        return Math.max(cacheExpireMinutes, staleCacheExpireMinutes) * 60;
    }
}
