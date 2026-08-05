package com.xiaou.web.growthcoach.adapter.persistence;

import com.xiaou.mockinterview.domain.CareerLoopAction;
import com.xiaou.mockinterview.dto.response.CareerLoopCurrentResponse;
import com.xiaou.mockinterview.mapper.JobBattleMatchRecordMapper;
import com.xiaou.mockinterview.mapper.JobBattlePlanRecordMapper;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import com.xiaou.mockinterview.service.CareerLoopService;
import com.xiaou.mockinterview.service.MockInterviewService;
import com.xiaou.plan.domain.GrowthAutopilotEvent;
import com.xiaou.plan.domain.GrowthAutopilotGoal;
import com.xiaou.plan.domain.GrowthAutopilotTask;
import com.xiaou.plan.mapper.GrowthAutopilotEventMapper;
import com.xiaou.plan.mapper.GrowthAutopilotGoalMapper;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort.WeeklyPlanData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MybatisGrowthCareerDataAdapterTest {

    @Mock private CareerLoopService careerLoopService;
    @Mock private JobBattleMatchRecordMapper matchRecordMapper;
    @Mock private JobBattlePlanRecordMapper planRecordMapper;
    @Mock private MockInterviewSessionMapper mockInterviewSessionMapper;
    @Mock private MockInterviewService mockInterviewService;
    @Mock private GrowthAutopilotGoalMapper goalMapper;
    @Mock private GrowthAutopilotTaskMapper taskMapper;
    @Mock private GrowthAutopilotEventMapper eventMapper;

    private MybatisGrowthCareerDataAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MybatisGrowthCareerDataAdapter(
                careerLoopService,
                matchRecordMapper,
                planRecordMapper,
                mockInterviewSessionMapper,
                mockInterviewService,
                goalMapper,
                taskMapper,
                eventMapper
        );
    }

    @Test
    void mapsCurrentActionsAndDelegatesCompletion() {
        CareerLoopAction action = new CareerLoopAction();
        action.setId(12L);
        action.setActionType("study");
        action.setStatus("todo");
        CareerLoopCurrentResponse current = new CareerLoopCurrentResponse();
        current.setActions(List.of(action));
        when(careerLoopService.findCurrentIfPresent(7L)).thenReturn(current);

        assertThat(adapter.currentCareerActions(7L))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.id()).isEqualTo(12L);
                    assertThat(item.actionType()).isEqualTo("study");
                    assertThat(item.status()).isEqualTo("todo");
                });

        adapter.markCareerActionDone(7L, 12L);
        verify(careerLoopService).markExistingActionDone(7L, 12L);
    }

    @Test
    void assemblesWeeklyPlanAsAnImmutableReadModel() {
        LocalDate weekStart = LocalDate.of(2026, 8, 3);
        GrowthAutopilotGoal goal = new GrowthAutopilotGoal();
        goal.setId(11L);
        goal.setUserId(7L);
        goal.setWeekStart(weekStart);
        goal.setWeekEnd(weekStart.plusDays(6));
        goal.setWeeklyMinutes(420);
        GrowthAutopilotTask task = new GrowthAutopilotTask();
        task.setStatus("todo");
        task.setTaskDate(weekStart);
        task.setPlannedMinutes(30);
        GrowthAutopilotEvent event = new GrowthAutopilotEvent();
        event.setEventType("postpone");
        when(goalMapper.selectByUserAndWeek(7L, weekStart)).thenReturn(goal);
        when(taskMapper.selectByGoalId(11L)).thenReturn(List.of(task));
        when(eventMapper.selectLatestByGoalId(11L, 100)).thenReturn(List.of(event));

        WeeklyPlanData result = adapter.weeklyPlan(7L, weekStart, 100);

        assertThat(result.weeklyMinutes()).isEqualTo(420);
        assertThat(result.tasks()).singleElement().satisfies(item -> assertThat(item.plannedMinutes()).isEqualTo(30));
        assertThat(result.events()).singleElement().satisfies(item -> assertThat(item.eventType()).isEqualTo("postpone"));
        assertThat(result.tasks()).isUnmodifiable();
    }
}
