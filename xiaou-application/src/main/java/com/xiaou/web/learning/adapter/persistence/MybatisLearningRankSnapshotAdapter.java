package com.xiaou.web.learning.adapter.persistence;

import com.xiaou.plan.domain.LearningCockpitRankSnapshot;
import com.xiaou.plan.mapper.LearningCockpitRankSnapshotMapper;
import com.xiaou.web.learning.port.LearningRankSnapshotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * MyBatis 排名快照适配器。
 */
@Component
@RequiredArgsConstructor
public class MybatisLearningRankSnapshotAdapter implements LearningRankSnapshotPort {

    private final LearningCockpitRankSnapshotMapper mapper;

    @Override
    public void upsert(RankSnapshotData source) {
        LearningCockpitRankSnapshot snapshot = new LearningCockpitRankSnapshot();
        snapshot.setUserId(source.userId());
        snapshot.setWeekStart(source.weekStart());
        snapshot.setWeekEnd(source.weekEnd());
        snapshot.setWeeklyRank(source.weeklyRank());
        snapshot.setAllRank(source.allRank());
        snapshot.setWeeklyPopulation(source.weeklyPopulation());
        snapshot.setAllPopulation(source.allPopulation());
        mapper.upsert(snapshot);
    }

    @Override
    public RankSnapshotData latestBeforeWeek(Long userId, java.time.LocalDate weekStart) {
        return toData(mapper.selectLatestBeforeWeek(userId, weekStart));
    }

    @Override
    public List<RankSnapshotData> recentByUser(Long userId, int limit) {
        List<LearningCockpitRankSnapshot> snapshots = mapper.selectRecentByUser(userId, limit);
        if (snapshots == null) {
            return List.of();
        }
        return snapshots.stream()
                .filter(Objects::nonNull)
                .map(this::toData)
                .toList();
    }

    private RankSnapshotData toData(LearningCockpitRankSnapshot source) {
        if (source == null) {
            return null;
        }
        return new RankSnapshotData(
                source.getUserId(),
                source.getWeekStart(),
                source.getWeekEnd(),
                source.getWeeklyRank(),
                source.getAllRank(),
                source.getWeeklyPopulation(),
                source.getAllPopulation()
        );
    }
}
