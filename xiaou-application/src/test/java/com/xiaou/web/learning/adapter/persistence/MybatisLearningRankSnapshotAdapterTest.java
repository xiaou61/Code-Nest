package com.xiaou.web.learning.adapter.persistence;

import com.xiaou.plan.domain.LearningCockpitRankSnapshot;
import com.xiaou.plan.mapper.LearningCockpitRankSnapshotMapper;
import com.xiaou.web.learning.port.LearningRankSnapshotPort.RankSnapshotData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MybatisLearningRankSnapshotAdapterTest {

    @Mock
    private LearningCockpitRankSnapshotMapper mapper;

    @Test
    void convertsRankSnapshotsAtThePersistenceBoundary() {
        MybatisLearningRankSnapshotAdapter adapter = new MybatisLearningRankSnapshotAdapter(mapper);
        LocalDate weekStart = LocalDate.of(2026, 8, 3);
        RankSnapshotData source = new RankSnapshotData(7L, weekStart, weekStart.plusDays(6), 12, 30, 80, 500);

        adapter.upsert(source);

        ArgumentCaptor<LearningCockpitRankSnapshot> captor = ArgumentCaptor.forClass(LearningCockpitRankSnapshot.class);
        verify(mapper).upsert(captor.capture());
        assertThat(captor.getValue().getWeeklyRank()).isEqualTo(12);

        LearningCockpitRankSnapshot stored = captor.getValue();
        when(mapper.selectRecentByUser(7L, 6)).thenReturn(List.of(stored));
        assertThat(adapter.recentByUser(7L, 6))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.weekStart()).isEqualTo(weekStart);
                    assertThat(item.allPopulation()).isEqualTo(500);
                });
    }
}
