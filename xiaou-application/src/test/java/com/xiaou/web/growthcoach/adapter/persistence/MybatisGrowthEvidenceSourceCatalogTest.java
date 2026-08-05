package com.xiaou.web.growthcoach.adapter.persistence;

import com.xiaou.interview.mapper.InterviewMasteryMapper;
import com.xiaou.mockinterview.mapper.CareerApplicationRecordMapper;
import com.xiaou.mockinterview.mapper.CareerLoopStageLogMapper;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import com.xiaou.sqloptimizer.mapper.SqlOptimizeRecordMapper;
import com.xiaou.web.growthcoach.evidence.GrowthEvidenceAdapter;
import com.xiaou.web.growthcoach.mapper.GrowthCodeArtifactMapper;
import com.xiaou.web.growthcoach.mapper.GrowthCodeReviewRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MybatisGrowthEvidenceSourceCatalogTest {

    @Mock private GrowthEvidenceAdapter adapter;
    @Mock private GrowthAutopilotTaskMapper taskMapper;
    @Mock private MockInterviewSessionMapper sessionMapper;
    @Mock private CareerApplicationRecordMapper careerApplicationRecordMapper;
    @Mock private CareerLoopStageLogMapper careerLoopStageLogMapper;
    @Mock private InterviewMasteryMapper masteryMapper;
    @Mock private OjSubmissionMapper submissionMapper;
    @Mock private SqlOptimizeRecordMapper sqlOptimizeRecordMapper;
    @Mock private GrowthCodeArtifactMapper codeArtifactMapper;
    @Mock private GrowthCodeReviewRecordMapper codeReviewRecordMapper;

    private MybatisGrowthEvidenceSourceCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new MybatisGrowthEvidenceSourceCatalog(
                List.of(adapter),
                taskMapper,
                sessionMapper,
                careerApplicationRecordMapper,
                careerLoopStageLogMapper,
                masteryMapper,
                submissionMapper,
                sqlOptimizeRecordMapper,
                codeArtifactMapper,
                codeReviewRecordMapper
        );
    }

    @Test
    void exposesAnImmutableSnapshotOfEvidenceAdapters() {
        assertThat(catalog.adapters()).containsExactly(adapter);
        assertThat(catalog.adapters()).isUnmodifiable();
    }

    @Test
    void deduplicatesValidUsersAndIsolatesSourceFailures() {
        when(taskMapper.selectUserIdsChangedForEvidence(any(LocalDateTime.class), eq(10)))
                .thenReturn(java.util.Arrays.asList(7L, 8L, null, -1L));
        when(sessionMapper.selectUserIdsChangedForEvidence(any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(8L, 9L));
        when(careerApplicationRecordMapper.selectUserIdsChangedForEvidence(any(LocalDateTime.class), eq(10)))
                .thenThrow(new IllegalStateException("source unavailable"));

        List<Long> users = catalog.recentlyChangedUserIds(LocalDateTime.of(2026, 8, 4, 12, 0), 10);

        assertThat(users).containsExactly(7L, 8L, 9L);
    }
}
