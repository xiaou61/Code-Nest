package com.xiaou.web.growthcoach.service;

import com.xiaou.resilience.ResilientExecutor;
import com.xiaou.web.growthcoach.dto.GrowthApplicationOutcomeResponse;
import com.xiaou.web.growthcoach.dto.GrowthCoachBriefingResponse;
import com.xiaou.web.growthcoach.dto.GrowthCoachTodayActionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthCoachBriefingServiceTest {

    @Mock
    private GrowthCoachApplicationService growthCoachApplicationService;
    @Mock
    private GrowthWeeklyReviewService growthWeeklyReviewService;
    @Mock
    private GrowthApplicationOutcomeService growthApplicationOutcomeService;
    @Mock
    private GrowthCareerNextActionService growthCareerNextActionService;
    @Mock
    private GrowthJobBattleGapService growthJobBattleGapService;
    @Mock
    private GrowthSkillInsightService growthSkillInsightService;
    @Mock
    private GrowthJobMarketSignalService growthJobMarketSignalService;
    @Mock
    private GrowthCoachMetricsRecorder metricsRecorder;

    @Test
    void prioritizesDueApplicationAndKeepsItsDailyTrackingIdStable() {
        GrowthApplicationOutcomeResponse outcome = new GrowthApplicationOutcomeResponse();
        outcome.setDueFollowUpCount(2);
        GrowthApplicationOutcomeResponse.NextAction next = new GrowthApplicationOutcomeResponse.NextAction();
        next.setTitle("更新两条投递进展");
        next.setRoutePath("/career-loop?focus=applications");
        next.setDueDate(LocalDate.now());
        outcome.setNextAction(next);
        when(growthApplicationOutcomeService.getForUser(7L)).thenReturn(outcome);

        GrowthCoachTodayActionResponse today = new GrowthCoachTodayActionResponse();
        today.setTaskId(99L);
        today.setTitle("完成普通计划任务");
        when(growthCoachApplicationService.getTodayAction(7L)).thenReturn(today);

        GrowthCoachBriefingService service = newService();
        GrowthCoachBriefingResponse first = service.getForUser(7L);
        GrowthCoachBriefingResponse second = service.getForUser(7L);

        assertThat(first.getPrimaryAction().getActionType()).isEqualTo("APPLICATION_FOLLOW_UP");
        assertThat(first.getPrimaryAction().getSource()).isEqualTo("application_outcomes");
        assertThat(first.getPrimaryAction().getTrackingId()).startsWith("gpa-");
        assertThat(second.getPrimaryAction().getTrackingId()).isEqualTo(first.getPrimaryAction().getTrackingId());
    }

    @Test
    void carriesThePlanTaskIdIntoTheGlobalPrimaryAction() {
        GrowthCoachTodayActionResponse today = new GrowthCoachTodayActionResponse();
        today.setTaskId(99L);
        today.setTitle("完成普通计划任务");
        when(growthCoachApplicationService.getTodayAction(7L)).thenReturn(today);

        GrowthCoachBriefingResponse response = newService().getForUser(7L);

        assertThat(response.getPrimaryAction().getActionType()).isEqualTo("TODAY_TASK");
        assertThat(response.getPrimaryAction().getActionId()).isEqualTo(99L);
    }

    private GrowthCoachBriefingService newService() {
        return new GrowthCoachBriefingService(
                growthCoachApplicationService,
                growthWeeklyReviewService,
                growthApplicationOutcomeService,
                growthCareerNextActionService,
                growthJobBattleGapService,
                growthSkillInsightService,
                growthJobMarketSignalService,
                new com.xiaou.web.growthcoach.config.GrowthCoachProperties(),
                new ResilientExecutor(),
                (Executor) Runnable::run,
                metricsRecorder
        );
    }
}
