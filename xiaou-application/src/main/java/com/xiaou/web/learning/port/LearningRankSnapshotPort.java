package com.xiaou.web.learning.port;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习驾驶舱排名历史的持久化边界。
 */
public interface LearningRankSnapshotPort {

    void upsert(RankSnapshotData snapshot);

    RankSnapshotData latestBeforeWeek(Long userId, LocalDate weekStart);

    List<RankSnapshotData> recentByUser(Long userId, int limit);

    record RankSnapshotData(
            Long userId,
            LocalDate weekStart,
            LocalDate weekEnd,
            Integer weeklyRank,
            Integer allRank,
            Integer weeklyPopulation,
            Integer allPopulation
    ) {
    }
}
