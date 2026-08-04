package com.xiaou.plan.growth.planner;

import com.xiaou.common.exception.BusinessException;
import com.xiaou.plan.domain.GrowthAutopilotGoal;
import com.xiaou.plan.domain.GrowthAutopilotTask;
import com.xiaou.plan.dto.GrowthPlanAdjustmentCommand;
import com.xiaou.plan.dto.GrowthPlanAdjustmentPreview;
import com.xiaou.plan.dto.GrowthPlanTaskChange;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 将已有可执行任务调整到用户当前可投入时间内。
 */
public class GrowthPlanConstraintPlanner {

    private static final int MIN_BUDGET_MINUTES = 15;
    private static final int MAX_BUDGET_MINUTES = 2_400;

    public GrowthPlanAdjustmentPreview preview(GrowthAutopilotGoal goal,
                                               List<GrowthAutopilotTask> tasks,
                                               GrowthPlanAdjustmentCommand command,
                                               LocalDate today) {
        if (goal == null) {
            throw new BusinessException("当前周尚未生成自动驾驶计划");
        }
        if (command == null || command.getAvailableMinutes() == null) {
            throw new BusinessException("请提供本周可投入时间");
        }

        int budgetMinutes = command.getAvailableMinutes();
        if (budgetMinutes < MIN_BUDGET_MINUTES || budgetMinutes > MAX_BUDGET_MINUTES) {
            throw new BusinessException("可投入时间需要在 15 到 2400 分钟之间");
        }

        LocalDate scheduleStart = max(today, goal.getWeekStart());
        if (scheduleStart == null || goal.getWeekEnd() == null || scheduleStart.isAfter(goal.getWeekEnd())) {
            throw new BusinessException("当前周已结束，无法调整计划");
        }

        List<LocalDate> scheduleDays = listDates(scheduleStart, goal.getWeekEnd());
        List<GrowthAutopilotTask> pendingTasks = safeTasks(tasks).stream()
                .filter(task -> "todo".equalsIgnoreCase(task.getStatus()))
                .toList();
        List<GrowthAutopilotTask> schedulableTasks = pendingTasks.stream()
                .filter(task -> task.getTaskDate() != null && !task.getTaskDate().isBefore(scheduleStart))
                .toList();
        Set<String> preferredModuleKeys = normalizePreferredModuleKeys(command.getPreferredModuleKeys());

        List<GrowthAutopilotTask> candidates = schedulableTasks.stream()
                .filter(this::hasStartableResource)
                .filter(task -> positiveMinutes(task) > 0)
                .sorted(candidateComparator(command.isPrioritizeInterview(), preferredModuleKeys))
                .toList();
        if (candidates.isEmpty()) {
            throw new BusinessException("当前计划没有可执行资源，无法安全调整");
        }

        GrowthPlanAdjustmentPreview preview = new GrowthPlanAdjustmentPreview();
        preview.setGoalId(goal.getId());
        preview.setWeekStart(goal.getWeekStart());
        preview.setBasePlanVersion(normalizePlanVersion(goal.getPlanVersion()));
        preview.setBudgetMinutes(budgetMinutes);
        preview.setTargetRole(normalizeRole(command.getTargetRole(), goal.getTargetRole()));
        preview.setCompletedTasksPreserved(true);
        preview.setAllTasksResourceBacked(true);

        List<Long> selectedTaskIds = new ArrayList<>();
        int remainingMinutes = budgetMinutes;
        int scheduleIndex = 0;
        for (GrowthAutopilotTask task : candidates) {
            int minutes = positiveMinutes(task);
            if (minutes > remainingMinutes) {
                continue;
            }
            LocalDate targetDate = scheduleDays.get(scheduleIndex % scheduleDays.size());
            scheduleIndex++;
            preview.getChanges().add(activeChange(task, targetDate, command.isPrioritizeInterview(), preferredModuleKeys));
            selectedTaskIds.add(task.getId());
            remainingMinutes -= minutes;
        }

        if (selectedTaskIds.isEmpty()) {
            throw new BusinessException("当前预算不足以安排任一可执行任务");
        }

        for (GrowthAutopilotTask task : pendingTasks) {
            if (!selectedTaskIds.contains(task.getId())) {
                preview.getChanges().add(supersedeChange(task, budgetMinutes));
            }
        }
        preview.setPlannedMinutes(budgetMinutes - remainingMinutes);
        return preview;
    }

    private GrowthPlanTaskChange activeChange(GrowthAutopilotTask task,
                                               LocalDate targetDate,
                                               boolean prioritizeInterview,
                                               Set<String> preferredModuleKeys) {
        GrowthPlanTaskChange change = baseChange(task);
        change.setFromDate(task.getTaskDate());
        change.setToDate(targetDate);
        change.setOperation(targetDate.equals(task.getTaskDate()) ? "KEEP" : "MOVE");
        change.setReason(prioritizeInterview && isInterviewTask(task)
                ? "面试临近，优先保留该任务"
                : isPreferredTask(task, preferredModuleKeys)
                ? "近期表现证据显示" + displayModuleName(task) + "需要巩固，优先保留该任务"
                : "在当前预算内保留的可执行任务");
        return change;
    }

    private GrowthPlanTaskChange supersedeChange(GrowthAutopilotTask task, int budgetMinutes) {
        GrowthPlanTaskChange change = baseChange(task);
        change.setFromDate(task.getTaskDate());
        change.setOperation("SUPERSEDE");
        change.setReason("为满足 " + budgetMinutes + " 分钟预算，暂不纳入本次计划版本");
        return change;
    }

    private GrowthPlanTaskChange baseChange(GrowthAutopilotTask task) {
        GrowthPlanTaskChange change = new GrowthPlanTaskChange();
        change.setTaskId(task.getId());
        change.setTitle(task.getTitle());
        change.setModuleName(task.getModuleName());
        change.setPlannedMinutes(positiveMinutes(task));
        change.setRoutePath(task.getRoutePath());
        change.setResourceType(StringUtils.hasText(task.getResourceType()) ? task.getResourceType() : "route");
        change.setResourceId(StringUtils.hasText(task.getResourceId()) ? task.getResourceId() : task.getRoutePath());
        change.setResourceVersion(task.getResourceVersion());
        return change;
    }

    private Comparator<GrowthAutopilotTask> candidateComparator(boolean prioritizeInterview,
                                                                 Set<String> preferredModuleKeys) {
        return Comparator
                .comparingInt((GrowthAutopilotTask task) -> prioritizeInterview && isInterviewTask(task) ? 0 : 1)
                .thenComparingInt(task -> isPreferredTask(task, preferredModuleKeys) ? 0 : 1)
                .thenComparingInt(task -> priorityWeight(task.getPriority()))
                .thenComparing(GrowthAutopilotTask::getTaskDate)
                .thenComparing(GrowthAutopilotTask::getId, Comparator.nullsLast(Long::compareTo));
    }

    private boolean hasStartableResource(GrowthAutopilotTask task) {
        return StringUtils.hasText(task.getResourceId()) || StringUtils.hasText(task.getRoutePath());
    }

    private boolean isInterviewTask(GrowthAutopilotTask task) {
        String module = normalizeModuleKey(task.getModuleKey());
        return "interview".equals(module) || "mock".equals(module);
    }

    private Set<String> normalizePreferredModuleKeys(List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            String moduleKey = normalizeModuleKey(value);
            if (StringUtils.hasText(moduleKey)) {
                normalized.add(moduleKey);
            }
            if (normalized.size() >= 6) {
                break;
            }
        }
        return normalized;
    }

    private boolean isPreferredTask(GrowthAutopilotTask task, Set<String> preferredModuleKeys) {
        return preferredModuleKeys != null
                && !preferredModuleKeys.isEmpty()
                && preferredModuleKeys.contains(normalizeModuleKey(task.getModuleKey()));
    }

    private String displayModuleName(GrowthAutopilotTask task) {
        return StringUtils.hasText(task.getModuleName()) ? task.getModuleName() : task.getModuleKey();
    }

    private String normalizeModuleKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int priorityWeight(String priority) {
        if ("P1".equalsIgnoreCase(priority)) {
            return 1;
        }
        if ("P2".equalsIgnoreCase(priority)) {
            return 2;
        }
        return 3;
    }

    private int positiveMinutes(GrowthAutopilotTask task) {
        return task.getPlannedMinutes() == null ? 0 : Math.max(task.getPlannedMinutes(), 0);
    }

    private int normalizePlanVersion(Integer value) {
        return value == null || value <= 0 ? 1 : value;
    }

    private String normalizeRole(String requestedRole, String fallbackRole) {
        return StringUtils.hasText(requestedRole) ? requestedRole.trim() : fallbackRole;
    }

    private LocalDate max(LocalDate left, LocalDate right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private List<LocalDate> listDates(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private List<GrowthAutopilotTask> safeTasks(List<GrowthAutopilotTask> tasks) {
        return tasks == null ? List.of() : tasks;
    }
}
