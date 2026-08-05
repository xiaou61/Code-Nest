package com.xiaou.web.growthcoach.service;

import com.xiaou.web.growthcoach.dto.GrowthWeeklyReviewResponse;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort.WeeklyEventData;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort.WeeklyPlanData;
import com.xiaou.web.growthcoach.port.GrowthCareerDataPort.WeeklyTaskData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 基于当前周已持久化的任务与事件生成节奏复盘。
 *
 * 复盘只提出可确认的调整建议，不自动改写任务或推断不可回溯的目标变化。
 */
@Service
@RequiredArgsConstructor
public class GrowthWeeklyReviewService {

    private static final int EVENT_LIMIT = 100;
    private static final int MULTIPLE_POSTPONES = 2;

    private final GrowthCareerDataPort careerDataPort;

    public GrowthWeeklyReviewResponse getCurrentReview(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        LocalDate today = LocalDate.now();
        WeeklyPlanData goal = careerDataPort.weeklyPlan(userId, currentWeekStart(today), EVENT_LIMIT);
        if (goal == null || !Objects.equals(goal.userId(), userId)) {
            return null;
        }

        List<WeeklyTaskData> tasks = goal.tasks().stream()
                .filter(this::isActiveTask)
                .toList();
        List<WeeklyEventData> events = goal.events();

        int totalTasks = tasks.size();
        int completedTasks = countStatus(tasks, "done");
        int missedTasks = countStatus(tasks, "missed");
        int overdueTasks = (int) tasks.stream()
                .filter(task -> hasStatus(task, "todo"))
                .filter(task -> task.taskDate() != null && task.taskDate().isBefore(today))
                .count();
        int postponedCount = (int) events.stream()
                .filter(event -> event != null && "postpone".equalsIgnoreCase(event.eventType()))
                .count();
        int targetChangeCount = (int) events.stream()
                .filter(event -> event != null && "target_change".equalsIgnoreCase(event.eventType()))
                .count();
        int remainingMinutes = tasks.stream()
                .filter(task -> hasStatus(task, "todo"))
                .mapToInt(task -> nvl(task.plannedMinutes()))
                .sum();
        int remainingCapacity = remainingCapacityMinutes(goal, today);
        int completionRate = totalTasks == 0 ? 0 : (int) Math.round(completedTasks * 100.0 / totalTasks);

        List<GrowthWeeklyReviewResponse.RiskSignal> signals = buildSignals(
                missedTasks, overdueTasks, postponedCount, targetChangeCount, remainingMinutes, remainingCapacity
        );
        GrowthWeeklyReviewResponse response = new GrowthWeeklyReviewResponse();
        response.setWeekStart(goal.weekStart());
        response.setWeekEnd(goal.weekEnd());
        response.setReviewedAt(LocalDateTime.now());
        response.setTotalTasks(totalTasks);
        response.setCompletedTasks(completedTasks);
        response.setMissedTasks(missedTasks);
        response.setOverdueTasks(overdueTasks);
        response.setPostponedCount(postponedCount);
        response.setTargetChangeCount(targetChangeCount);
        response.setCompletionRate(completionRate);
        response.setRemainingMinutes(remainingMinutes);
        response.setRemainingCapacityMinutes(remainingCapacity);
        response.setSignals(signals);
        applySummary(response);
        return response;
    }

    private List<GrowthWeeklyReviewResponse.RiskSignal> buildSignals(
            int missedTasks,
            int overdueTasks,
            int postponedCount,
            int targetChangeCount,
            int remainingMinutes,
            int remainingCapacity
    ) {
        List<GrowthWeeklyReviewResponse.RiskSignal> signals = new ArrayList<>();
        if (remainingMinutes > remainingCapacity) {
            signals.add(signal(
                    "OVERLOAD",
                    "urgent",
                    "剩余任务超过本周可用容量",
                    "待完成任务约 " + remainingMinutes + " 分钟，按本周剩余天数估算可投入约 "
                            + remainingCapacity + " 分钟。"
            ));
        }
        if (overdueTasks > 0) {
            signals.add(signal(
                    "OVERDUE",
                    "urgent",
                    "存在逾期任务",
                    "当前有 " + overdueTasks + " 个待办已超过原计划日期。"
            ));
        }
        if (missedTasks > 0) {
            signals.add(signal(
                    "MISSED",
                    missedTasks >= 2 ? "urgent" : "warning",
                    "本周已有错过任务",
                    "已有 " + missedTasks + " 个任务被标记为错过，需要重新选择保留范围。"
            ));
        }
        if (postponedCount >= MULTIPLE_POSTPONES) {
            signals.add(signal(
                    "REPEATED_POSTPONEMENT",
                    "warning",
                    "本周出现多次顺延",
                    "本周已记录 " + postponedCount + " 次顺延，建议降低当天任务密度或调整优先级。"
            ));
        }
        if (targetChangeCount >= MULTIPLE_POSTPONES) {
            signals.add(signal(
                    "GOAL_DRIFT",
                    "warning",
                    "本周目标岗位多次变化",
                    "本周已记录 " + targetChangeCount + " 次岗位目标调整，建议先确认一个主目标再继续拆解任务。"
            ));
        }
        return signals;
    }

    private void applySummary(GrowthWeeklyReviewResponse response) {
        boolean urgent = response.getSignals().stream().anyMatch(signal -> "urgent".equals(signal.getLevel()));
        boolean attention = urgent || !response.getSignals().isEmpty();
        if (!attention) {
            response.setLevel("stable");
            response.setTitle("本周节奏稳定");
            response.setSummary("已完成 " + nvl(response.getCompletedTasks()) + " / " + nvl(response.getTotalTasks())
                    + " 个任务，当前不需要调整计划。");
            return;
        }

        response.setLevel(urgent ? "high_risk" : "attention");
        response.setTitle(urgent ? "建议尽快调整本周计划" : "建议检查本周执行节奏");
        response.setSummary("已完成 " + nvl(response.getCompletedTasks()) + " / " + nvl(response.getTotalTasks())
                + " 个任务；先保留高优先级和已完成记录，再处理剩余任务。");
        response.setSuggestedAdjustmentMessage(buildAdjustmentMessage(response));
    }

    private String buildAdjustmentMessage(GrowthWeeklyReviewResponse review) {
        if (nvl(review.getRemainingMinutes()) > nvl(review.getRemainingCapacityMinutes())) {
            return "本周剩余任务约 " + review.getRemainingMinutes() + " 分钟，剩余可用容量约 "
                    + review.getRemainingCapacityMinutes() + " 分钟。请保留已完成任务和高优先级任务，顺延低优先级任务。";
        }
        if (nvl(review.getOverdueTasks()) > 0 || nvl(review.getMissedTasks()) > 0) {
            return "本周已有 " + (nvl(review.getOverdueTasks()) + nvl(review.getMissedTasks()))
                    + " 个逾期或错过任务。请保留最关键任务，并重新安排其余待办。";
        }
        if (nvl(review.getTargetChangeCount()) >= MULTIPLE_POSTPONES) {
            return "本周岗位目标已调整 " + review.getTargetChangeCount()
                    + " 次。请先确定一个主目标岗位，并围绕该目标保留本周最高优先级任务。";
        }
        return "本周已出现多次任务顺延。请降低本周任务密度，优先保留高优先级任务。";
    }

    private GrowthWeeklyReviewResponse.RiskSignal signal(
            String type,
            String level,
            String title,
            String description
    ) {
        GrowthWeeklyReviewResponse.RiskSignal signal = new GrowthWeeklyReviewResponse.RiskSignal();
        signal.setType(type);
        signal.setLevel(level);
        signal.setTitle(title);
        signal.setDescription(description);
        return signal;
    }

    private int remainingCapacityMinutes(WeeklyPlanData goal, LocalDate today) {
        int weeklyMinutes = nvl(goal.weeklyMinutes());
        if (weeklyMinutes <= 0) {
            weeklyMinutes = nvl(goal.weeklyHours()) * 60;
        }
        if (weeklyMinutes <= 0 || goal.weekStart() == null || goal.weekEnd() == null) {
            return 0;
        }

        LocalDate start = today.isBefore(goal.weekStart()) ? goal.weekStart() : today;
        if (start.isAfter(goal.weekEnd())) {
            return 0;
        }
        int remainingDays = (int) java.time.temporal.ChronoUnit.DAYS.between(start, goal.weekEnd()) + 1;
        int dailyCapacity = (int) Math.ceil(weeklyMinutes / 7.0);
        return dailyCapacity * remainingDays;
    }

    private LocalDate currentWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private boolean isActiveTask(WeeklyTaskData task) {
        return hasStatus(task, "todo") || hasStatus(task, "done") || hasStatus(task, "missed");
    }

    private boolean hasStatus(WeeklyTaskData task, String expected) {
        return task != null && expected.equalsIgnoreCase(task.status());
    }

    private int countStatus(List<WeeklyTaskData> tasks, String status) {
        return (int) tasks.stream().filter(task -> hasStatus(task, status)).count();
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
