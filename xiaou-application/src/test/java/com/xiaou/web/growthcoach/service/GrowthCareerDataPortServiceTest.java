package com.xiaou.web.growthcoach.service;

import com.xiaou.web.growthcoach.dto.GrowthCareerNextActionResponse;
import com.xiaou.web.growthcoach.dto.GrowthWeeklyReviewResponse;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort.CareerActionData;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort.WeeklyEventData;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort.WeeklyPlanData;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort.WeeklyTaskData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthCareerDataPortServiceTest {

    @Mock
    private GrowthCareerDataPort careerDataPort;

    @Test
    void selectsTheHighestPriorityOpenCareerActionFromThePort() {
        when(careerDataPort.currentCareerActions(7L)).thenReturn(List.of(
                action(3L, "doing", "P1", LocalDate.now()),
                action(2L, "todo", "P2", LocalDate.now().plusDays(1)),
                action(1L, "done", "P1", LocalDate.now().minusDays(1))
        ));
        GrowthCareerNextActionService service = new GrowthCareerNextActionService(careerDataPort);

        GrowthCareerNextActionResponse response = service.getNextAction(7L);

        assertThat(response.getActionId()).isEqualTo(2L);
        assertThat(response.getRoutePath()).isEqualTo("/plan");
        service.markDone(7L, 2L);
        verify(careerDataPort).markCareerActionDone(7L, 2L);
    }

    @Test
    void buildsWeeklyRiskReviewWithoutLeakingPlanEntities() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        WeeklyPlanData plan = new WeeklyPlanData(
                11L,
                7L,
                weekStart,
                weekStart.plusDays(6),
                0,
                0,
                List.of(
                        new WeeklyTaskData("todo", today.minusDays(1), 90),
                        new WeeklyTaskData("done", today, 30),
                        new WeeklyTaskData("missed", today, 20)
                ),
                List.of(
                        new WeeklyEventData("postpone"),
                        new WeeklyEventData("postpone"),
                        new WeeklyEventData("target_change")
                )
        );
        when(careerDataPort.weeklyPlan(eq(7L), any(LocalDate.class), eq(100))).thenReturn(plan);
        GrowthWeeklyReviewService service = new GrowthWeeklyReviewService(careerDataPort);

        GrowthWeeklyReviewResponse response = service.getCurrentReview(7L);

        assertThat(response.getTotalTasks()).isEqualTo(3);
        assertThat(response.getCompletedTasks()).isEqualTo(1);
        assertThat(response.getMissedTasks()).isEqualTo(1);
        assertThat(response.getOverdueTasks()).isEqualTo(1);
        assertThat(response.getPostponedCount()).isEqualTo(2);
        assertThat(response.getSignals()).extracting(GrowthWeeklyReviewResponse.RiskSignal::getType)
                .contains("OVERLOAD", "OVERDUE", "MISSED", "REPEATED_POSTPONEMENT");
    }

    private CareerActionData action(Long id, String status, String priority, LocalDate dueDate) {
        return new CareerActionData(
                id,
                "STUDY",
                "study",
                "学习任务",
                "完成计划中的学习任务",
                priority,
                status,
                dueDate
        );
    }
}
