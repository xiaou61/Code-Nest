package com.xiaou.web.learning.service;

import com.xiaou.flashcard.service.FlashcardStudyService;
import com.xiaou.interview.service.InterviewLearnRecordService;
import com.xiaou.interview.service.InterviewMasteryService;
import com.xiaou.mockinterview.service.CareerLoopService;
import com.xiaou.oj.service.OjRankingService;
import com.xiaou.oj.service.OjSubmissionService;
import com.xiaou.plan.service.PlanService;
import com.xiaou.points.service.PointsService;
import com.xiaou.resilience.ResilientExecutor;
import com.xiaou.web.learning.port.LearningRankSnapshotPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningCockpitServiceTest {

    @Mock
    private PlanService planService;
    @Mock
    private PointsService pointsService;
    @Mock
    private FlashcardStudyService flashcardStudyService;
    @Mock
    private InterviewLearnRecordService interviewLearnRecordService;
    @Mock
    private InterviewMasteryService interviewMasteryService;
    @Mock
    private OjSubmissionService ojSubmissionService;
    @Mock
    private OjRankingService ojRankingService;
    @Mock
    private CareerLoopService careerLoopService;
    @Mock
    private LearningRankSnapshotPort rankSnapshotPort;

    private ExecutorService requestExecutor;
    private ExecutorService overviewExecutor;

    @BeforeEach
    void setUp() {
        requestExecutor = Executors.newSingleThreadExecutor();
        overviewExecutor = Executors.newFixedThreadPool(8);
    }

    @AfterEach
    void tearDown() {
        requestExecutor.shutdownNow();
        overviewExecutor.shutdownNow();
    }

    @Test
    void shouldStartIndependentOverviewQueriesInParallel() throws Exception {
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        when(planService.getStatsOverview(7L)).thenAnswer(invocation -> {
            started.countDown();
            release.await(2, TimeUnit.SECONDS);
            return null;
        });
        when(pointsService.getPointsBalance(7L)).thenAnswer(invocation -> {
            started.countDown();
            release.await(2, TimeUnit.SECONDS);
            return null;
        });
        when(ojRankingService.getRanking(anyString())).thenReturn(Collections.emptyList());

        LearningCockpitService service = new LearningCockpitService(
                planService,
                pointsService,
                flashcardStudyService,
                interviewLearnRecordService,
                interviewMasteryService,
                ojSubmissionService,
                ojRankingService,
                careerLoopService,
                rankSnapshotPort,
                new ResilientExecutor(),
                overviewExecutor
        );

        Future<?> request = requestExecutor.submit(() -> service.getOverview(7L, "Java", 8));
        boolean bothStarted = started.await(500, TimeUnit.MILLISECONDS);
        release.countDown();
        request.get(5, TimeUnit.SECONDS);

        assertThat(bothStarted).isTrue();
    }
}
