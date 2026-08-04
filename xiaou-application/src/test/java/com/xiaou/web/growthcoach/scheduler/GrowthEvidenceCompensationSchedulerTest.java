package com.xiaou.web.growthcoach.scheduler;

import com.xiaou.interview.mapper.InterviewMasteryMapper;
import com.xiaou.mockinterview.mapper.CareerApplicationRecordMapper;
import com.xiaou.mockinterview.mapper.CareerLoopStageLogMapper;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import com.xiaou.oj.mapper.OjSubmissionMapper;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import com.xiaou.sqloptimizer.mapper.SqlOptimizeRecordMapper;
import com.xiaou.web.growthcoach.config.GrowthCoachProperties;
import com.xiaou.web.growthcoach.mapper.GrowthCodeArtifactMapper;
import com.xiaou.web.growthcoach.mapper.GrowthCodeReviewRecordMapper;
import com.xiaou.web.growthcoach.service.GrowthEvidenceProjectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthEvidenceCompensationSchedulerTest {

    @Mock
    private GrowthAutopilotTaskMapper taskMapper;
    @Mock
    private MockInterviewSessionMapper sessionMapper;
    @Mock
    private CareerApplicationRecordMapper careerApplicationRecordMapper;
    @Mock
    private CareerLoopStageLogMapper careerLoopStageLogMapper;
    @Mock
    private InterviewMasteryMapper masteryMapper;
    @Mock
    private OjSubmissionMapper submissionMapper;
    @Mock
    private SqlOptimizeRecordMapper sqlOptimizeRecordMapper;
    @Mock
    private GrowthCodeArtifactMapper codeArtifactMapper;
    @Mock
    private GrowthCodeReviewRecordMapper codeReviewRecordMapper;
    @Mock
    private GrowthEvidenceProjectorService projectorService;

    private GrowthCoachProperties properties;
    private GrowthEvidenceCompensationScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new GrowthCoachProperties();
        properties.getEvidenceProjection().setCompensationUserBatchSize(10);
        properties.getEvidenceProjection().setCompensationLookbackMinutes(120);
        scheduler = new GrowthEvidenceCompensationScheduler(
                taskMapper,
                sessionMapper,
                careerApplicationRecordMapper,
                careerLoopStageLogMapper,
                masteryMapper,
                submissionMapper,
                sqlOptimizeRecordMapper,
                codeArtifactMapper,
                codeReviewRecordMapper,
                projectorService,
                properties
        );
    }

    @Test
    void refreshesEachRecentlyChangedUserOnlyOnce() {
        when(taskMapper.selectUserIdsChangedForEvidence(any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(7L, 8L));
        when(sessionMapper.selectUserIdsChangedForEvidence(any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(8L, 9L));

        scheduler.refreshRecentlyChangedUsers();

        verify(projectorService).refreshForUser(7L);
        verify(projectorService).refreshForUser(8L);
        verify(projectorService).refreshForUser(9L);
    }

    @Test
    void doesNothingWhenEvidenceProjectionIsDisabled() {
        properties.getEvidenceProjection().setEnabled(false);

        scheduler.refreshRecentlyChangedUsers();

        verifyNoInteractions(taskMapper, sessionMapper, projectorService);
    }

    @Test
    void continuesWithOtherUsersWhenOneProjectionFails() {
        when(taskMapper.selectUserIdsChangedForEvidence(any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of(7L, 8L));
        when(sessionMapper.selectUserIdsChangedForEvidence(any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of());
        org.mockito.Mockito.doThrow(new IllegalStateException("broken user projection"))
                .when(projectorService).refreshForUser(7L);

        scheduler.refreshRecentlyChangedUsers();

        verify(projectorService).refreshForUser(8L);
        verify(projectorService, never()).refreshForUser(9L);
    }
}
