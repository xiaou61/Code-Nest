package com.xiaou.sensitive.scheduler;

import com.xiaou.sensitive.domain.SensitiveSource;
import com.xiaou.sensitive.mapper.SensitiveSourceMapper;
import com.xiaou.sensitive.service.SensitiveSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 敏感词来源定时同步任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveSourceSyncScheduler {

    private static final int DEFAULT_SYNC_INTERVAL_HOURS = 24;

    private final SensitiveSourceMapper sourceMapper;
    private final SensitiveSourceService sourceService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 扫描并同步到期来源（默认每小时扫描一次）
     */
    @Scheduled(
            initialDelayString = "${xiaou.sensitive.source-sync.initial-delay:60000}",
            fixedDelayString = "${xiaou.sensitive.source-sync.fixed-delay:3600000}"
    )
    public void syncDueSources() {
        if (!running.compareAndSet(false, true)) {
            log.info("敏感词来源同步任务仍在执行，跳过本轮");
            return;
        }

        try {
            List<SensitiveSource> sources = sourceMapper.selectEnabledSources();
            if (sources == null || sources.isEmpty()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            for (SensitiveSource source : sources) {
                if (!shouldSync(source, now)) {
                    continue;
                }
                try {
                    SensitiveSourceService.SyncResult result = sourceService.syncSource(source.getId());
                    if (!result.isSuccess()) {
                        log.warn("自动同步来源失败: sourceId={}, sourceName={}, message={}",
                                source.getId(), source.getSourceName(), result.getMessage());
                    } else {
                        log.info("自动同步来源成功: sourceId={}, sourceName={}, added={}, updated={}, failed={}",
                                source.getId(), source.getSourceName(), result.getAddedCount(),
                                result.getUpdatedCount(), result.getFailedCount());
                    }
                } catch (Exception ex) {
                    log.error("自动同步来源异常: sourceId={}, sourceName={}",
                            source.getId(), source.getSourceName(), ex);
                }
            }
        } finally {
            running.set(false);
        }
    }

    private boolean shouldSync(SensitiveSource source, LocalDateTime now) {
        if (source == null || source.getStatus() == null || source.getStatus() != 1) {
            return false;
        }
        int intervalHours = source.getSyncInterval() == null || source.getSyncInterval() <= 0
                ? DEFAULT_SYNC_INTERVAL_HOURS
                : source.getSyncInterval();
        LocalDateTime lastSyncTime = source.getLastSyncTime();
        if (lastSyncTime == null) {
            return true;
        }
        LocalDateTime nextSyncTime = lastSyncTime.plusHours(intervalHours);
        return !nextSyncTime.isAfter(now);
    }
}
