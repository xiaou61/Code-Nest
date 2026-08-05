package com.xiaou.web.growthcoach.port;

import com.xiaou.web.growthcoach.evidence.GrowthEvidenceAdapter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 为成长证据编排提供稳定的来源目录，隔离各业务模块的持久化实现。
 */
public interface GrowthEvidenceSourceCatalog {

    List<GrowthEvidenceAdapter> adapters();

    List<Long> recentlyChangedUserIds(LocalDateTime updatedAfter, int limit);
}
