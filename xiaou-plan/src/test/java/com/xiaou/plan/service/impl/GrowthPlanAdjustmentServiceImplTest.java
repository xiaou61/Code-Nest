package com.xiaou.plan.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.plan.domain.GrowthAutopilotGoal;
import com.xiaou.plan.domain.GrowthAutopilotTask;
import com.xiaou.plan.dto.GrowthPlanAdjustmentApplyResult;
import com.xiaou.plan.dto.GrowthPlanAdjustmentPreview;
import com.xiaou.plan.dto.GrowthPlanTaskChange;
import com.xiaou.plan.mapper.GrowthAutopilotEventMapper;
import com.xiaou.plan.mapper.GrowthAutopilotGoalMapper;
import com.xiaou.plan.mapper.GrowthAutopilotRevisionMapper;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthPlanAdjustmentServiceImplTest {

    @Mock
    private GrowthAutopilotGoalMapper goalMapper;
    @Mock
    private GrowthAutopilotTaskMapper taskMapper;
    @Mock
    private GrowthAutopilotEventMapper eventMapper;
    @Mock
    private GrowthAutopilotRevisionMapper revisionMapper;

    @Test
    void returnsAConflictBeforeWritingWhenAPreviewedTaskWasCompleted() {
        GrowthPlanAdjustmentServiceImpl service = new GrowthPlanAdjustmentServiceImpl(
                goalMapper,
                taskMapper,
                eventMapper,
                revisionMapper,
                new ObjectMapper().findAndRegisterModules()
        );
        GrowthAutopilotGoal goal = new GrowthAutopilotGoal();
        goal.setId(100L);
        goal.setUserId(7L);
        goal.setPlanVersion(4);
        goal.setTargetRole("Java 后端");

        GrowthAutopilotTask completedTask = new GrowthAutopilotTask();
        completedTask.setId(200L);
        completedTask.setGoalId(100L);
        completedTask.setUserId(7L);
        completedTask.setStatus("done");

        when(goalMapper.selectById(100L)).thenReturn(goal);
        when(taskMapper.selectByGoalIdForUpdate(100L)).thenReturn(List.of(completedTask));

        GrowthPlanAdjustmentApplyResult result = service.apply(7L, previewFor(200L), "growth-run-1");

        assertThat(result.isVersionConflict()).isTrue();
        assertThat(result.getMessage()).contains("计划在预览后已变化");
        verify(goalMapper, never()).advancePlanVersion(anyLong(), anyLong(), anyInt(), anyInt(), any(), any(), any(), any());
        verify(taskMapper, never()).applyPlanAdjustmentTask(anyLong(), anyLong(), anyLong(), any(), anyInt(), any(), any(), any(), any());
        verify(taskMapper, never()).supersedePlanAdjustmentTask(anyLong(), anyLong(), anyLong(), anyInt(), any());
        verify(eventMapper, never()).insert(any());
        verify(revisionMapper, never()).insert(any());
    }

    private GrowthPlanAdjustmentPreview previewFor(Long taskId) {
        GrowthPlanTaskChange change = new GrowthPlanTaskChange();
        change.setTaskId(taskId);
        change.setOperation("KEEP");
        change.setToDate(LocalDate.of(2026, 7, 20));
        change.setResourceType("route");
        change.setResourceId("/oj/1");

        GrowthPlanAdjustmentPreview preview = new GrowthPlanAdjustmentPreview();
        preview.setGoalId(100L);
        preview.setBasePlanVersion(4);
        preview.setBudgetMinutes(180);
        preview.setTargetRole("Java 后端");
        preview.setChanges(List.of(change));
        return preview;
    }
}
