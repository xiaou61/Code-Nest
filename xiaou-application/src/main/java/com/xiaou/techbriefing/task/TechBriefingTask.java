package com.xiaou.techbriefing.task;

import com.xiaou.techbriefing.service.TechBriefingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 科技热点定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TechBriefingTask {

    private final TechBriefingService techBriefingService;

    @Scheduled(fixedRate = 15 * 60 * 1000L)
    public void refreshTechBriefing() {
        log.info("启动科技热点速览定时刷新任务");
        techBriefingService.refreshAllSources();
    }

    @Scheduled(initialDelay = 60 * 1000L, fixedRate = Long.MAX_VALUE)
    public void initializeTechBriefing() {
        log.info("初始化科技热点速览数据");
        techBriefingService.refreshAllSources();
    }
}
