package com.xiaou.plan.growth.planner;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.plan.domain.GrowthAutopilotGoal;
import com.xiaou.plan.domain.GrowthAutopilotTask;
import com.xiaou.plan.dto.GrowthPlanAdjustmentCommand;
import com.xiaou.plan.dto.GrowthPlanAdjustmentPreview;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrowthPlanConstraintPlannerTest {

    private final GrowthPlanConstraintPlanner planner = new GrowthPlanConstraintPlanner();

    @Test
    void keepsCompletedHistoryLimitsRemainingBudgetAndPrioritizesInterviewTasks() {
        LocalDate monday = LocalDate.of(2026, 7, 13);
        GrowthAutopilotGoal goal = goal(monday);
        List<GrowthAutopilotTask> tasks = List.of(
                task(1L, "done", "oj", "P1", 90, monday, "/oj"),
                task(2L, "todo", "oj", "P1", 100, monday, "/oj"),
                task(3L, "todo", "interview", "P2", 80, monday.plusDays(1), "/interview"),
                task(4L, "todo", "flashcard", "P3", 60, monday.plusDays(2), "/flashcard/study")
        );

        GrowthPlanAdjustmentCommand command = new GrowthPlanAdjustmentCommand();
        command.setWeekStart(monday);
        command.setAvailableMinutes(180);
        command.setTargetRole("Java 后端");
        command.setPrioritizeInterview(true);

        GrowthPlanAdjustmentPreview preview = planner.preview(goal, tasks, command, monday);

        assertThat(preview.getBasePlanVersion()).isEqualTo(4);
        assertThat(preview.getPlannedMinutes()).isLessThanOrEqualTo(180);
        assertThat(preview.getChanges())
                .noneMatch(change -> change.getTaskId().equals(1L))
                .anySatisfy(change -> {
                    assertThat(change.getTaskId()).isEqualTo(3L);
                    assertThat(change.getOperation()).isIn("KEEP", "MOVE");
                    assertThat(change.getResourceType()).isEqualTo("route");
                })
                .anySatisfy(change -> {
                    assertThat(change.getTaskId()).isEqualTo(4L);
                    assertThat(change.getOperation()).isEqualTo("SUPERSEDE");
                });
    }

    @Test
    void rejectsAPlanWhenNoPendingTaskHasAStartableResource() {
        LocalDate monday = LocalDate.of(2026, 7, 13);
        GrowthPlanAdjustmentCommand command = new GrowthPlanAdjustmentCommand();
        command.setWeekStart(monday);
        command.setAvailableMinutes(180);

        assertThatThrownBy(() -> planner.preview(
                goal(monday),
                List.of(task(2L, "todo", "oj", "P1", 80, monday, "")),
                command,
                monday
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("可执行资源");
    }

    private GrowthAutopilotGoal goal(LocalDate monday) {
        GrowthAutopilotGoal goal = new GrowthAutopilotGoal();
        goal.setId(100L);
        goal.setUserId(7L);
        goal.setWeekStart(monday);
        goal.setWeekEnd(monday.plusDays(6));
        goal.setTargetRole("后端开发");
        goal.setWeeklyHours(8);
        goal.setWeeklyMinutes(480);
        goal.setPlanVersion(4);
        return goal;
    }

    private GrowthAutopilotTask task(Long id,
                                     String status,
                                     String moduleKey,
                                     String priority,
                                     int minutes,
                                     LocalDate date,
                                     String routePath) {
        GrowthAutopilotTask task = new GrowthAutopilotTask();
        task.setId(id);
        task.setGoalId(100L);
        task.setUserId(7L);
        task.setStatus(status);
        task.setModuleKey(moduleKey);
        task.setModuleName(moduleKey);
        task.setPriority(priority);
        task.setPlannedMinutes(minutes);
        task.setTaskDate(date);
        task.setRoutePath(routePath);
        task.setTitle(moduleKey + " task");
        return task;
    }
}
