package com.xiaou.moyu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 摸鱼工具配置类
 *
 * @author xiaou
 */
@Configuration
@EnableAsync
public class MoyuConfig {
    
    /**
     * RestTemplate Bean
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                     @Value("${hot-topic.api.connect-timeout-millis:2000}") long connectTimeoutMillis,
                                     @Value("${hot-topic.api.read-timeout-millis:5000}") long readTimeoutMillis) {
        return builder
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .readTimeout(Duration.ofMillis(readTimeoutMillis))
                .build();
    }
    
    /**
     * 热榜数据刷新专用异步执行器
     */
    @Bean("hotTopicExecutor")
    public Executor hotTopicExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(3);
        // 最大线程数
        executor.setMaxPoolSize(8);
        // 队列容量
        executor.setQueueCapacity(50);
        // 线程名前缀
        executor.setThreadNamePrefix("hot-topic-");
        // 线程存活时间（秒）
        executor.setKeepAliveSeconds(60);
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
}
