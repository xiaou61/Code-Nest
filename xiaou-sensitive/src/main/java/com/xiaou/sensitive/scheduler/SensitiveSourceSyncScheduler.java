package com.xiaou.sensitive.scheduler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.xiaou.sensitive.domain.SensitiveSource;
import com.xiaou.sensitive.mapper.SensitiveSourceMapper;
import com.xiaou.sensitive.service.SensitiveSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 敏感词来源定时同步任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveSourceSyncScheduler {

    private static final int DEFAULT_SYNC_INTERVAL_HOURS = 24;

    @Value("${xiaou.sensitive.source-sync.max-retries:2}")
    private int maxRetries;

    @Value("${xiaou.sensitive.source-sync.retry-delay-ms:2000}")
    private long retryDelayMs;

    @Value("${xiaou.sensitive.source-sync.alert-threshold:3}")
    private int alertThreshold;

    @Value("${xiaou.sensitive.source-sync.alert-webhook:}")
    private String alertWebhook;

    private final SensitiveSourceMapper sourceMapper;
    private final SensitiveSourceService sourceService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<Integer, Integer> consecutiveFailureMap = new ConcurrentHashMap<>();

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
                syncWithRetry(source);
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

    private void syncWithRetry(SensitiveSource source) {
        int attempts = Math.max(1, maxRetries + 1);
        String lastMessage = "";
        boolean success = false;
        int added = 0;
        int updated = 0;
        int failed = 0;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                SensitiveSourceService.SyncResult result = sourceService.syncSource(source.getId());
                if (result != null && result.isSuccess()) {
                    success = true;
                    added = result.getAddedCount();
                    updated = result.getUpdatedCount();
                    failed = result.getFailedCount();
                    break;
                }
                lastMessage = result == null ? "同步结果为空" : result.getMessage();
                log.warn("自动同步来源失败，准备重试: sourceId={}, sourceName={}, attempt={}/{}, message={}",
                        source.getId(), source.getSourceName(), attempt, attempts, lastMessage);
            } catch (Exception ex) {
                lastMessage = ex.getMessage();
                log.error("自动同步来源异常，准备重试: sourceId={}, sourceName={}, attempt={}/{}",
                        source.getId(), source.getSourceName(), attempt, attempts, ex);
            }

            if (attempt < attempts && retryDelayMs > 0) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (success) {
            consecutiveFailureMap.remove(source.getId());
            log.info("自动同步来源成功: sourceId={}, sourceName={}, added={}, updated={}, failed={}",
                    source.getId(), source.getSourceName(), added, updated, failed);
            return;
        }

        int failureCount = consecutiveFailureMap.merge(source.getId(), 1, Integer::sum);
        log.error("自动同步来源最终失败: sourceId={}, sourceName={}, consecutiveFailures={}, message={}",
                source.getId(), source.getSourceName(), failureCount, lastMessage);
        if (failureCount >= Math.max(1, alertThreshold)) {
            sendAlert(source, failureCount, lastMessage);
        }
    }

    private void sendAlert(SensitiveSource source, int failureCount, String reason) {
        log.error("敏感词来源同步告警: sourceId={}, sourceName={}, consecutiveFailures={}, reason={}",
                source.getId(), source.getSourceName(), failureCount, reason);

        if (StrUtil.isBlank(alertWebhook)) {
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "sensitive_source_sync_alert");
            payload.put("sourceId", source.getId());
            payload.put("sourceName", source.getSourceName());
            payload.put("sourceType", source.getSourceType());
            payload.put("consecutiveFailures", failureCount);
            payload.put("reason", StrUtil.blankToDefault(reason, "unknown"));
            payload.put("timestamp", LocalDateTime.now().toString());

            HttpResponse response = HttpRequest.post(alertWebhook)
                    .timeout(5000)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(payload))
                    .execute();
            if (!response.isOk()) {
                log.warn("告警webhook发送失败: sourceId={}, status={}", source.getId(), response.getStatus());
            }
        } catch (Exception e) {
            log.error("告警webhook发送异常: sourceId={}", source.getId(), e);
        }
    }
}
