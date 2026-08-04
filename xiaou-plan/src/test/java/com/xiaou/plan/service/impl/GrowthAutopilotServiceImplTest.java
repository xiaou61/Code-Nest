package com.xiaou.plan.service.impl;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.plan.domain.GrowthAutopilotGoal;
import com.xiaou.plan.domain.GrowthAutopilotTask;
import com.xiaou.plan.mapper.GrowthAutopilotEventMapper;
import com.xiaou.plan.mapper.GrowthAutopilotGoalMapper;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthAutopilotServiceImplTest {

    @Mock
    private GrowthAutopilotGoalMapper goalMapper;
    @Mock
    private GrowthAutopilotTaskMapper taskMapper;
    @Mock
    private GrowthAutopilotEventMapper eventMapper;

    @Test
    void rejectsCompletionWhenThePlanVersionChangedConcurrently() {
        GrowthAutopilotServiceImpl service = new GrowthAutopilotServiceImpl(
                goalMapper, taskMapper, eventMapper
        );
        GrowthAutopilotTask task = new GrowthAutopilotTask();
        task.setId(100L);
        task.setGoalId(10L);
        task.setUserId(7L);
        task.setTitle("Java 集合专项练习");
        task.setStatus("todo");

        GrowthAutopilotGoal goal = new GrowthAutopilotGoal();
        goal.setId(10L);
        goal.setUserId(7L);
        goal.setPlanVersion(4);
        goal.setWeeklyHours(8);
        goal.setWeeklyMinutes(480);
        goal.setTargetRole("Java 后端");

        when(taskMapper.selectById(100L)).thenReturn(task);
        when(goalMapper.selectById(10L)).thenReturn(goal);
        when(taskMapper.markDone(eq(100L), eq(10L), eq(7L), any(LocalDateTime.class))).thenReturn(1);
        when(goalMapper.advancePlanVersion(
                eq(10L), eq(7L), eq(4), eq(5), eq("Java 后端"), eq(480), eq(8), isNull()
        )).thenReturn(0);

        assertThatThrownBy(() -> service.completeTask(7L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("计划已发生变化");

        verify(goalMapper).advancePlanVersion(
                eq(10L), eq(7L), eq(4), eq(5), eq("Java 后端"), eq(480), eq(8), isNull()
        );
        verify(eventMapper, never()).insert(any());
    }
}
