package com.xiaou.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 * 
 * <p>配置说明：</p>
 * <ul>
 *   <li>默认使用 Redisson Spring Boot Starter 自动配置</li>
 *   <li>配置参数在 application.yml 中的 spring.redis.redisson 部分</li>
 *   <li>如需自定义配置，可在此类中添加 @Bean 方法</li>
 * </ul>
 * 
 * <p>配置示例：</p>
 * <pre>
 * spring:
 *   redis:
 *     redisson:
 *       config: |
 *         singleServerConfig:
 *           address: "redis://localhost:6379"
 *           database: 0
 *           connectionMinimumIdleSize: 10
 *           connectionPoolSize: 64
 *           idleConnectionTimeout: 10000
 *           connectTimeout: 10000
 *           timeout: 3000
 *           retryAttempts: 3
 *           retryInterval: 1500
 *         threads: 16
 *         nettyThreads: 32
 * </pre>
 *
 * @author xiaou
 */
@Configuration
public class RedisConfig {
    
    /**
     * Redisson Spring Boot Starter 会自动创建以下 Bean：
     * - RedissonClient
     * - RedissonRxClient  
     * - RedissonReactiveClient
     * - RedisTemplate (如果需要)
     * - ReactiveRedisTemplate (如果需要)
     * 
     * 如果需要自定义配置，可以添加 @Bean 方法，使用 @ConditionalOnMissingBean 确保不冲突
     */
    
    /*
    // 自定义配置示例（如果需要的话）
    @Bean
    @ConditionalOnMissingBean
    public RedissonClient customRedissonClient() {
        Config config = new Config();
        // 自定义配置...
        return Redisson.create(config);
    }
    */
} 
