package com.xiaou.web.growthcoach.evidence;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 将真实业务记录投影为成长证据的来源 Adapter。
 */
public interface GrowthEvidenceAdapter {

    String sourceKey();

    List<GrowthEvidenceProjection> loadChanges(Long userId,
                                                LocalDateTime afterUpdateTime,
                                                Long afterSourceId,
                                                int limit);
}
