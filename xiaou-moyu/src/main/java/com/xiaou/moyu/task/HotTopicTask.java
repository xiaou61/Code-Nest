package com.xiaou.moyu.task;

import com.xiaou.moyu.service.HotTopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 热榜数据定时任务
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotTopicTask {
    
    private final HotTopicService hotTopicService;
    
    /**
     * 定时刷新热榜数据
     * 每15分钟执行一次
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void refreshHotTopicData() {
        log.info("启动热榜数据定时刷新任务");
        
        try {
            hotTopicService.refreshHotTopicData();
        } catch (Exception e) {
            log.error("热榜数据定时任务异常: {}", e.getMessage());
        }
    }
    
    /**
     * 应用启动后延迟1分钟执行一次智能初始化
     * 只有Redis缓存为空时才请求API
     */
    @Scheduled(initialDelay = 60 * 1000, fixedRate = Long.MAX_VALUE)
    public void initializeHotTopicData() {
        log.info("启动热榜数据智能初始化任务");
        
        try {
            hotTopicService.initializeHotTopicDataIfNeeded();
        } catch (Exception e) {
            log.error("热榜数据初始化任务异常: {}", e.getMessage());
        }
    }
}
