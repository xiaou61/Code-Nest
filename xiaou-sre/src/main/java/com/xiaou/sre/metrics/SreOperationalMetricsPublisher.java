package com.xiaou.sre.metrics;

import com.xiaou.sre.config.SreMetricsProperties;
import com.xiaou.sre.mapper.SreOperationalMetricsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically updates in-memory gauges without querying the database on Prometheus scrape threads.
 *
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SreOperationalMetricsPublisher {

    private final SreMetricsProperties properties;
    private final SreOperationalMetricsMapper metricsMapper;
    private final SreMetricsRecorder metricsRecorder;

    @Scheduled(
            fixedDelayString = "${xiaou.sre.metrics.refresh-ms:10000}",
            initialDelayString = "${xiaou.sre.metrics.initial-delay-ms:15000}"
    )
    public void refresh() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            SreOperationalMetricsRow row = metricsMapper.selectSnapshot();
            if (row != null) {
                metricsRecorder.updateOperationalSnapshot(row.toSnapshot());
            }
        } catch (RuntimeException exception) {
            log.warn("SRE 运行指标快照刷新失败: reason={}", exception.getClass().getSimpleName());
        }
    }
}
