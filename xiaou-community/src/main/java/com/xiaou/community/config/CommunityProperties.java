package com.xiaou.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 社区模块配置属性
 * 
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "community")
public class CommunityProperties {
    
    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();
    
    /**
     * 热门帖子配置
     */
    private HotConfig hot = new HotConfig();
    
    /**
     * AI配置
     */
    private AiConfig ai = new AiConfig();
    
    @Data
    public static class CacheConfig {
        /**
         * 热门帖子缓存时间（秒）
         */
        private Long hotPostsTtl = 600L;
        
        /**
         * 帖子详情缓存时间（秒）
         */
        private Long postDetailTtl = 1800L;
        
        /**
         * 用户信息缓存时间（秒）
         */
        private Long userInfoTtl = 3600L;
        
        /**
         * 标签列表缓存时间（秒）
         */
        private Long tagsTtl = 86400L;
    }
    
    @Data
    public static class HotConfig {
        /**
         * 热门帖子时间范围（小时）
         */
        private Integer timeRange = 72;
        
        /**
         * 最低热度分数
         */
        private Double minScore = 30.0;
        
        /**
         * 热门帖子数量限制
         */
        private Integer limit = 10;
        
        /**
         * 定时任务执行间隔（分钟）
         */
        private Integer refreshInterval = 10;
    }
    
    @Data
    public static class AiConfig {
        /**
         * 是否启用AI摘要功能
         */
        private Boolean enabled = false;

        /**
         * 自动生成摘要的最小字数
         */
        private Integer minContentLength = 500;
        
        /**
         * 摘要缓存时间（秒）
         */
        private Long summaryCacheTtl = 2592000L;
    }
}

