package com.xiaou.web.growthcoach.scheduler;

import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import com.xiaou.web.growthcoach.port.GrowthEvidenceSourceCatalog;
import com.xiaou.web.growthcoach.service.GrowthEvidenceProjectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 补偿应用重启或按需投影遗漏的近期成长证据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrowthEvidenceCompensationScheduler {

    private final GrowthEvidenceSourceCatalog sourceCatalog;
    private final GrowthEvidenceProjectorService projectorService;
    private final GrowthCoachProperties properties;

    @Scheduled(cron = "${xiaou.growth-coach.evidence-projection.compensation-cron:0 */10 * * * ?}")
    public void refreshRecentlyChangedUsers() {
        GrowthCoachProperties.EvidenceProjection config = properties.getEvidenceProjection();
        if (config == null || !config.isEnabled() || !config.isCompensationEnabled()) {
            return;
        }
        int limit = normalizedBatchSize(config.getCompensationUserBatchSize());
        LocalDateTime updatedAfter = LocalDateTime.now().minusMinutes(normalizedLookbackMinutes(config.getCompensationLookbackMinutes()));
        List<Long> userIds = sourceCatalog.recentlyChangedUserIds(updatedAfter, limit);
        for (Long userId : userIds) {
            try {
                projectorService.refreshForUser(userId);
            } catch (RuntimeException exception) {
                log.warn("成长证据补偿投影失败: {}", exception.getClass().getSimpleName());
            }
        }
    }

    private int normalizedBatchSize(int configured) {
        return configured <= 0 ? 100 : Math.min(configured, 500);
    }

    private int normalizedLookbackMinutes(int configured) {
        return configured <= 0 ? 1440 : Math.min(configured, 60 * 24 * 30);
    }
}
