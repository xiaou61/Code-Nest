package com.xiaou.plan.service.impl;

import cn.hutool.core.util.StrUtil;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.plan.domain.GrowthAutopilotEvent;
import com.xiaou.plan.domain.GrowthAutopilotGoal;
import com.xiaou.plan.domain.GrowthAutopilotTask;
import com.xiaou.plan.dto.GrowthAutopilotDashboardResponse;
import com.xiaou.plan.dto.GrowthAutopilotGenerateRequest;
import com.xiaou.plan.dto.GrowthAutopilotReplanRequest;
import com.xiaou.plan.mapper.GrowthAutopilotEventMapper;
import com.xiaou.plan.mapper.GrowthAutopilotGoalMapper;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import com.xiaou.plan.service.GrowthAutopilotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成长闭环自动驾驶服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthAutopilotServiceImpl implements GrowthAutopilotService {

    private static final int DEFAULT_WEEKLY_HOURS = 8;
    private static final int MIN_WEEKLY_HOURS = 3;
    private static final int MAX_WEEKLY_HOURS = 40;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GrowthAutopilotGoalMapper goalMapper;
    private final GrowthAutopilotTaskMapper taskMapper;
    private final GrowthAutopilotEventMapper eventMapper;

    @Override
    public GrowthAutopilotDashboardResponse getDashboard(Long userId, LocalDate weekStart) {
        LocalDate normalizedWeekStart = normalizeWeekStart(weekStart);
        GrowthAutopilotGoal goal = goalMapper.selectByUserAndWeek(userId, normalizedWeekStart);
        if (goal == null) {
            return emptyDashboard(normalizedWeekStart);
        }
        List<GrowthAutopilotTask> tasks = taskMapper.selectByGoalId(goal.getId());
        return assembleDashboard(goal, tasks);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthAutopilotDashboardResponse generateWeeklyPlan(Long userId, GrowthAutopilotGenerateRequest request) {
        GrowthAutopilotGenerateRequest body = request == null ? new GrowthAutopilotGenerateRequest() : request;
        LocalDate weekStart = normalizeWeekStart(body.getWeekStart());
        LocalDate weekEnd = weekStart.plusDays(6);
        String targetRole = normalizeRole(body.getTargetRole());
        Integer weeklyHours = normalizeWeeklyHours(body.getWeeklyHours());
        LocalDateTime now = LocalDateTime.now();

        GrowthAutopilotGoal goal = goalMapper.selectByUserAndWeek(userId, weekStart);
        if (goal == null) {
            goal = new GrowthAutopilotGoal();
            goal.setUserId(userId);
            goal.setWeekStart(weekStart);
            goal.setWeekEnd(weekEnd);
            goal.setTargetRole(targetRole);
            goal.setWeeklyHours(weeklyHours);
            goal.setTotalScoreTarget(0);
            goal.setTotalScoreCompleted(0);
            goal.setTotalTasks(0);
            goal.setCompletedTasks(0);
            goal.setCompletionRate(0);
            goal.setRiskLevel("low");
            goal.setStatus("active");
            goal.setGeneratedAt(now);
            goalMapper.insert(goal);
        } else {
            goal.setWeekEnd(weekEnd);
            goal.setTargetRole(targetRole);
            goal.setWeeklyHours(weeklyHours);
            goal.setStatus("active");
            goal.setGeneratedAt(now);
            taskMapper.deleteByGoalId(goal.getId());
            goalMapper.updateById(goal);
        }

        List<GrowthAutopilotTask> generatedTasks = buildWeeklyTasks(goal, LocalDate.now());
        if (!generatedTasks.isEmpty()) {
            taskMapper.batchInsert(generatedTasks);
        }
        refreshGoalMetrics(goal.getId());
        writeEvent(goal.getId(), userId, "generate", "系统已生成本周自动驾驶计划");
        return getDashboard(userId, goal.getWeekStart());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthAutopilotDashboardResponse completeTask(Long userId, Long taskId) {
        if (taskId == null) {
            throw new BusinessException("任务ID不能为空");
        }
        GrowthAutopilotTask task = taskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getUserId(), userId)) {
            throw new BusinessException("任务不存在");
        }
        GrowthAutopilotGoal goal = goalMapper.selectById(task.getGoalId());
        if (goal == null || !Objects.equals(goal.getUserId(), userId)) {
            throw new BusinessException("周计划不存在");
        }

        if (!"done".equalsIgnoreCase(task.getStatus())) {
            taskMapper.markDone(taskId, goal.getId(), userId, LocalDateTime.now());
            refreshGoalMetrics(goal.getId());
            writeEvent(goal.getId(), userId, "complete", "完成任务：" + task.getTitle());
        }
        return getDashboard(userId, goal.getWeekStart());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthAutopilotDashboardResponse completeTodayTasks(Long userId, LocalDate weekStart) {
        LocalDate normalizedWeekStart = normalizeWeekStart(weekStart);
        GrowthAutopilotGoal goal = goalMapper.selectByUserAndWeek(userId, normalizedWeekStart);
        if (goal == null) {
            throw new BusinessException("当前周尚未生成自动驾驶计划");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(goal.getWeekStart()) || today.isAfter(goal.getWeekEnd())) {
            throw new BusinessException("今天不在当前周计划范围内");
        }
        int count = taskMapper.markDoneByDate(goal.getId(), userId, today, LocalDateTime.now());
        if (count > 0) {
            writeEvent(goal.getId(), userId, "complete_batch", "批量完成今日任务 " + count + " 个");
        }
        refreshGoalMetrics(goal.getId());
        return getDashboard(userId, goal.getWeekStart());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthAutopilotDashboardResponse postponeTask(Long userId, Long taskId) {
        if (taskId == null) {
            throw new BusinessException("任务ID不能为空");
        }
        GrowthAutopilotTask task = taskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getUserId(), userId)) {
            throw new BusinessException("任务不存在");
        }
        if (!"todo".equalsIgnoreCase(task.getStatus())) {
            throw new BusinessException("仅待完成任务支持顺延");
        }

        GrowthAutopilotGoal goal = goalMapper.selectById(task.getGoalId());
        if (goal == null || !Objects.equals(goal.getUserId(), userId)) {
            throw new BusinessException("周计划不存在");
        }
        if (task.getTaskDate() == null) {
            throw new BusinessException("任务日期异常，无法顺延");
        }
        LocalDate nextDate = task.getTaskDate().plusDays(1);
        if (nextDate.isAfter(goal.getWeekEnd())) {
            throw new BusinessException("已是本周最后一天任务，建议执行一键重排");
        }
        int changed = taskMapper.postponeTask(taskId, goal.getId(), userId, nextDate);
        if (changed <= 0) {
            throw new BusinessException("任务顺延失败，请稍后重试");
        }

        writeEvent(goal.getId(), userId, "postpone", "任务顺延一天：" + task.getTitle());
        refreshGoalMetrics(goal.getId());
        return getDashboard(userId, goal.getWeekStart());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthAutopilotDashboardResponse replan(Long userId, GrowthAutopilotReplanRequest request) {
        GrowthAutopilotReplanRequest body = request == null ? new GrowthAutopilotReplanRequest() : request;
        LocalDate weekStart = normalizeWeekStart(body.getWeekStart());
        GrowthAutopilotGoal goal = goalMapper.selectByUserAndWeek(userId, weekStart);
        if (goal == null) {
            throw new BusinessException("当前周尚未生成自动驾驶计划");
        }

        LocalDate today = LocalDate.now();
        taskMapper.markMissedBeforeDate(goal.getId(), userId, today);
        taskMapper.deleteTodoFromDate(goal.getId(), userId, today);

        List<GrowthAutopilotTask> currentTasks = taskMapper.selectByGoalId(goal.getId());
        int completedScore = currentTasks.stream()
                .filter(item -> "done".equalsIgnoreCase(item.getStatus()))
                .mapToInt(item -> nvl(item.getTaskScore()))
                .sum();
        int targetScore = Math.max(0, nvl(goal.getTotalScoreTarget()));
        int remainingScore = Math.max(0, targetScore - completedScore);

        List<LocalDate> availableDays = listDates(maxDate(today, goal.getWeekStart()), goal.getWeekEnd());
        if (remainingScore > 0 && !availableDays.isEmpty()) {
            List<GrowthAutopilotTask> replanTasks = buildReplanTasks(goal, currentTasks, availableDays, remainingScore);
            if (!replanTasks.isEmpty()) {
                taskMapper.batchInsert(replanTasks);
            }
        }

        refreshGoalMetrics(goal.getId());
        writeEvent(goal.getId(), userId, "replan", "已根据当前进度完成任务重排");
        return getDashboard(userId, goal.getWeekStart());
    }

    private GrowthAutopilotDashboardResponse assembleDashboard(GrowthAutopilotGoal goal, List<GrowthAutopilotTask> tasks) {
        LocalDate today = LocalDate.now();
        GrowthAutopilotDashboardResponse response = new GrowthAutopilotDashboardResponse();
        response.setHasPlan(true);
        response.setWeekStart(formatDate(goal.getWeekStart()));
        response.setWeekEnd(formatDate(goal.getWeekEnd()));
        response.setGeneratedAt(formatDateTime(goal.getGeneratedAt()));

        GrowthAutopilotDashboardResponse.TargetProfile targetProfile = new GrowthAutopilotDashboardResponse.TargetProfile();
        targetProfile.setTargetRole(StrUtil.blankToDefault(goal.getTargetRole(), "通用"));
        targetProfile.setWeeklyHours(nvl(goal.getWeeklyHours()));
        targetProfile.setNote(String.format("按“%s”岗位 + 每周%d小时自动编排任务",
                targetProfile.getTargetRole(), targetProfile.getWeeklyHours()));
        response.setTargetProfile(targetProfile);

        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream().filter(item -> "done".equalsIgnoreCase(item.getStatus())).count();
        int todoTasks = (int) tasks.stream().filter(item -> "todo".equalsIgnoreCase(item.getStatus())).count();
        int missedTasks = (int) tasks.stream().filter(item -> "missed".equalsIgnoreCase(item.getStatus())).count();
        int todayTasks = (int) tasks.stream().filter(item -> today.equals(item.getTaskDate())).count();
        int todayCompleted = (int) tasks.stream()
                .filter(item -> today.equals(item.getTaskDate()) && "done".equalsIgnoreCase(item.getStatus()))
                .count();
        int overdueTasks = (int) tasks.stream()
                .filter(item -> "todo".equalsIgnoreCase(item.getStatus()) && item.getTaskDate() != null && item.getTaskDate().isBefore(today))
                .count();
        int targetScore = tasks.stream().mapToInt(item -> nvl(item.getTaskScore())).sum();
        int doneScore = tasks.stream()
                .filter(item -> "done".equalsIgnoreCase(item.getStatus()))
                .mapToInt(item -> nvl(item.getTaskScore()))
                .sum();
        int completionRate = totalTasks == 0 ? 0 : (int) Math.round((completedTasks * 100.0) / totalTasks);

        String riskLevel = calcRiskLevel(completionRate, overdueTasks, daysLeft(goal.getWeekEnd(), today));
        GrowthAutopilotDashboardResponse.Summary summary = new GrowthAutopilotDashboardResponse.Summary();
        summary.setTotalTasks(totalTasks);
        summary.setCompletedTasks(completedTasks);
        summary.setCompletionRate(clamp(completionRate, 0, 100));
        summary.setTodayTasks(todayTasks);
        summary.setTodayCompleted(todayCompleted);
        summary.setOverdueTasks(overdueTasks);
        summary.setRiskLevel(riskLevel);
        summary.setRiskText(riskText(riskLevel, overdueTasks));
        response.setSummary(summary);

        GrowthAutopilotDashboardResponse.StatusSummary statusSummary = new GrowthAutopilotDashboardResponse.StatusSummary();
        statusSummary.setTodoTasks(todoTasks);
        statusSummary.setDoneTasks(completedTasks);
        statusSummary.setMissedTasks(missedTasks);
        statusSummary.setDoneScore(doneScore);
        statusSummary.setTargetScore(targetScore);
        response.setStatusSummary(statusSummary);

        Map<String, List<GrowthAutopilotTask>> moduleMap = tasks.stream()
                .collect(Collectors.groupingBy(item -> StrUtil.blankToDefault(item.getModuleKey(), "other")));
        List<ModuleTemplate> moduleTemplates = buildModuleTemplates(goal.getTargetRole());
        List<GrowthAutopilotDashboardResponse.ModuleProgress> progressList = new ArrayList<>();
        for (ModuleTemplate template : moduleTemplates) {
            List<GrowthAutopilotTask> moduleTasks = moduleMap.getOrDefault(template.key(), Collections.emptyList());
            int moduleTotal = moduleTasks.size();
            int moduleDone = (int) moduleTasks.stream().filter(item -> "done".equalsIgnoreCase(item.getStatus())).count();

            GrowthAutopilotDashboardResponse.ModuleProgress progress = new GrowthAutopilotDashboardResponse.ModuleProgress();
            progress.setModuleKey(template.key());
            progress.setModuleName(template.name());
            progress.setTotalTasks(moduleTotal);
            progress.setCompletedTasks(moduleDone);
            progress.setCompletionRate(moduleTotal == 0 ? 0 : (int) Math.round((moduleDone * 100.0) / moduleTotal));
            progress.setRoutePath(template.routePath());
            progressList.add(progress);
        }
        response.setModuleProgress(progressList);

        Map<LocalDate, List<GrowthAutopilotTask>> dayTaskMap = tasks.stream()
                .collect(Collectors.groupingBy(GrowthAutopilotTask::getTaskDate));
        List<GrowthAutopilotDashboardResponse.DayTaskBucket> buckets = new ArrayList<>();
        for (LocalDate date : listDates(goal.getWeekStart(), goal.getWeekEnd())) {
            GrowthAutopilotDashboardResponse.DayTaskBucket bucket = new GrowthAutopilotDashboardResponse.DayTaskBucket();
            bucket.setDate(formatDate(date));
            bucket.setDayLabel(dayLabel(date.getDayOfWeek()));
            bucket.setToday(today.equals(date));

            List<GrowthAutopilotDashboardResponse.TaskItem> taskItems = new ArrayList<>();
            for (GrowthAutopilotTask task : dayTaskMap.getOrDefault(date, Collections.emptyList())) {
                GrowthAutopilotDashboardResponse.TaskItem taskItem = new GrowthAutopilotDashboardResponse.TaskItem();
                taskItem.setTaskId(task.getId());
                taskItem.setTaskDate(formatDate(task.getTaskDate()));
                taskItem.setModuleKey(StrUtil.blankToDefault(task.getModuleKey(), ""));
                taskItem.setModuleName(StrUtil.blankToDefault(task.getModuleName(), ""));
                taskItem.setTitle(StrUtil.blankToDefault(task.getTitle(), ""));
                taskItem.setDescription(StrUtil.blankToDefault(task.getDescription(), ""));
                taskItem.setPlannedMinutes(nvl(task.getPlannedMinutes()));
                taskItem.setTaskScore(nvl(task.getTaskScore()));
                taskItem.setPriority(StrUtil.blankToDefault(task.getPriority(), "P2"));
                taskItem.setStatus(StrUtil.blankToDefault(task.getStatus(), "todo"));
                taskItem.setStatusText(statusText(taskItem.getStatus()));
                taskItem.setOverdue("todo".equalsIgnoreCase(taskItem.getStatus()) && date.isBefore(today));
                taskItem.setSource(StrUtil.blankToDefault(task.getSource(), "auto"));
                taskItem.setRoutePath(StrUtil.blankToDefault(task.getRoutePath(), "/"));
                taskItems.add(taskItem);
            }
            bucket.setTasks(taskItems);
            buckets.add(bucket);
        }
        response.setDayBuckets(buckets);

        List<GrowthAutopilotDashboardResponse.DayProgress> dailyProgress = new ArrayList<>();
        for (LocalDate date : listDates(goal.getWeekStart(), goal.getWeekEnd())) {
            List<GrowthAutopilotTask> dayTasks = dayTaskMap.getOrDefault(date, Collections.emptyList());
            int dayTotal = dayTasks.size();
            int dayDone = (int) dayTasks.stream().filter(item -> "done".equalsIgnoreCase(item.getStatus())).count();

            GrowthAutopilotDashboardResponse.DayProgress progress = new GrowthAutopilotDashboardResponse.DayProgress();
            progress.setDate(formatDate(date));
            progress.setDayLabel(dayLabel(date.getDayOfWeek()));
            progress.setTotalTasks(dayTotal);
            progress.setCompletedTasks(dayDone);
            progress.setCompletionRate(dayTotal == 0 ? 0 : (int) Math.round(dayDone * 100.0 / dayTotal));
            dailyProgress.add(progress);
        }
        response.setDailyProgress(dailyProgress);

        response.setQuickActions(buildQuickActions(summary, progressList));
        response.setEvents(buildEventLogs(goal.getId()));
        return response;
    }

    private List<GrowthAutopilotDashboardResponse.QuickAction> buildQuickActions(
            GrowthAutopilotDashboardResponse.Summary summary,
            List<GrowthAutopilotDashboardResponse.ModuleProgress> progressList
    ) {
        List<GrowthAutopilotDashboardResponse.QuickAction> actions = new ArrayList<>();
        if (summary.getOverdueTasks() > 0) {
            GrowthAutopilotDashboardResponse.QuickAction action = new GrowthAutopilotDashboardResponse.QuickAction();
            action.setTitle("先清理逾期任务");
            action.setDescription("当前有 " + summary.getOverdueTasks() + " 个逾期任务，优先恢复执行节奏。");
            action.setRoutePath("/growth-autopilot");
            actions.add(action);
        }

        progressList.stream()
                .filter(item -> item.getTotalTasks() > 0)
                .min(Comparator.comparingInt(GrowthAutopilotDashboardResponse.ModuleProgress::getCompletionRate))
                .ifPresent(lowest -> {
                    GrowthAutopilotDashboardResponse.QuickAction action = new GrowthAutopilotDashboardResponse.QuickAction();
                    action.setTitle("补齐「" + lowest.getModuleName() + "」进度");
                    action.setDescription("该模块完成率仅 " + lowest.getCompletionRate() + "%，建议优先处理。");
                    action.setRoutePath(lowest.getRoutePath());
                    actions.add(action);
                });

        GrowthAutopilotDashboardResponse.QuickAction action = new GrowthAutopilotDashboardResponse.QuickAction();
        action.setTitle("执行一键重排");
        action.setDescription("系统会根据剩余时间自动重排任务，避免本周失速。");
        action.setRoutePath("/growth-autopilot");
        actions.add(action);

        if (actions.size() > 3) {
            return actions.subList(0, 3);
        }
        return actions;
    }

    private List<GrowthAutopilotDashboardResponse.EventLog> buildEventLogs(Long goalId) {
        List<GrowthAutopilotEvent> events = eventMapper.selectLatestByGoalId(goalId, 8);
        List<GrowthAutopilotDashboardResponse.EventLog> logs = new ArrayList<>();
        for (GrowthAutopilotEvent event : events) {
            GrowthAutopilotDashboardResponse.EventLog item = new GrowthAutopilotDashboardResponse.EventLog();
            item.setEventType(StrUtil.blankToDefault(event.getEventType(), ""));
            item.setEventLabel(eventLabel(event.getEventType()));
            item.setDetail(StrUtil.blankToDefault(event.getEventDetail(), ""));
            item.setCreateTime(formatDateTime(event.getCreateTime()));
            logs.add(item);
        }
        return logs;
    }

    private List<GrowthAutopilotTask> buildWeeklyTasks(GrowthAutopilotGoal goal, LocalDate today) {
        List<ModuleTemplate> templates = buildModuleTemplates(goal.getTargetRole());
        int weeklyMinutes = nvl(goal.getWeeklyHours()) * 60;
        LocalDate startDate = maxDate(goal.getWeekStart(), today);
        List<LocalDate> availableDays = listDates(startDate, goal.getWeekEnd());
        if (availableDays.isEmpty()) {
            availableDays = listDates(goal.getWeekStart(), goal.getWeekEnd());
        }

        List<GrowthAutopilotTask> tasks = new ArrayList<>();
        for (int moduleIndex = 0; moduleIndex < templates.size(); moduleIndex++) {
            ModuleTemplate template = templates.get(moduleIndex);
            int moduleMinutes = Math.max(template.minMinutes(), (int) Math.round(weeklyMinutes * template.weight()));
            int taskCount = clamp((int) Math.round(moduleMinutes / 95.0), 1, 4);
            int minutePerTask = clamp((int) Math.round(moduleMinutes * 1.0 / taskCount), 25, 150);

            for (int i = 0; i < taskCount; i++) {
                LocalDate taskDate = availableDays.get((moduleIndex + i) % availableDays.size());
                GrowthAutopilotTask task = new GrowthAutopilotTask();
                task.setGoalId(goal.getId());
                task.setUserId(goal.getUserId());
                task.setModuleKey(template.key());
                task.setModuleName(template.name());
                task.setTaskDate(taskDate);
                task.setTitle(template.titles().get(i % template.titles().size()));
                task.setDescription("预计投入 " + minutePerTask + " 分钟，完成后打卡沉淀关键结论。");
                task.setPlannedMinutes(minutePerTask);
                task.setTaskScore(clamp((int) Math.round(minutePerTask / 24.0), 1, 8));
                task.setPriority(i == 0 ? "P1" : i == 1 ? "P2" : "P3");
                task.setStatus("todo");
                task.setSource("auto");
                task.setRoutePath(template.routePath());
                tasks.add(task);
            }
        }
        tasks.sort(Comparator.comparing(GrowthAutopilotTask::getTaskDate)
                .thenComparing(GrowthAutopilotTask::getPriority)
                .thenComparing(GrowthAutopilotTask::getModuleKey));
        return tasks;
    }

    private List<GrowthAutopilotTask> buildReplanTasks(
            GrowthAutopilotGoal goal,
            List<GrowthAutopilotTask> currentTasks,
            List<LocalDate> availableDays,
            int remainingScore
    ) {
        if (availableDays.isEmpty() || remainingScore <= 0) {
            return Collections.emptyList();
        }

        List<ModuleTemplate> templates = buildModuleTemplates(goal.getTargetRole());
        Map<String, Long> doneCount = currentTasks.stream()
                .filter(item -> "done".equalsIgnoreCase(item.getStatus()))
                .collect(Collectors.groupingBy(GrowthAutopilotTask::getModuleKey, Collectors.counting()));
        Map<String, Long> totalCount = currentTasks.stream()
                .collect(Collectors.groupingBy(GrowthAutopilotTask::getModuleKey, Collectors.counting()));

        List<ModuleTemplate> sortedTemplates = templates.stream()
                .sorted(Comparator.comparingDouble(template -> {
                    long total = totalCount.getOrDefault(template.key(), 0L);
                    if (total <= 0) {
                        return 0.0;
                    }
                    return doneCount.getOrDefault(template.key(), 0L) * 1.0 / total;
                }))
                .toList();

        int taskCount = clamp((int) Math.ceil(remainingScore / 4.0), 1, availableDays.size() * 3);
        int baseMinutes = clamp((int) Math.round((remainingScore * 28.0) / taskCount), 25, 120);

        List<GrowthAutopilotTask> tasks = new ArrayList<>();
        int scoreLeft = remainingScore;
        for (int i = 0; i < taskCount; i++) {
            ModuleTemplate template = sortedTemplates.get(i % sortedTemplates.size());
            int taskScore = i == taskCount - 1 ? scoreLeft : clamp((int) Math.round(remainingScore * 1.0 / taskCount), 1, 8);
            scoreLeft = Math.max(0, scoreLeft - taskScore);

            GrowthAutopilotTask task = new GrowthAutopilotTask();
            task.setGoalId(goal.getId());
            task.setUserId(goal.getUserId());
            task.setModuleKey(template.key());
            task.setModuleName(template.name());
            task.setTaskDate(availableDays.get(i % availableDays.size()));
            task.setTitle("补位冲刺：" + template.replanTitle());
            task.setDescription("系统重排任务，建议优先完成以追回本周进度。");
            task.setPlannedMinutes(baseMinutes);
            task.setTaskScore(taskScore);
            task.setPriority(i < availableDays.size() ? "P1" : "P2");
            task.setStatus("todo");
            task.setSource("replan");
            task.setRoutePath(template.routePath());
            tasks.add(task);
        }
        tasks.sort(Comparator.comparing(GrowthAutopilotTask::getTaskDate)
                .thenComparing(GrowthAutopilotTask::getPriority)
                .thenComparing(GrowthAutopilotTask::getModuleKey));
        return tasks;
    }

    private GrowthAutopilotGoal refreshGoalMetrics(Long goalId) {
        GrowthAutopilotGoal goal = goalMapper.selectById(goalId);
        if (goal == null) {
            throw new BusinessException("周计划不存在");
        }
        List<GrowthAutopilotTask> tasks = taskMapper.selectByGoalId(goalId);
        LocalDate today = LocalDate.now();

        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream().filter(item -> "done".equalsIgnoreCase(item.getStatus())).count();
        int totalScoreTarget = tasks.stream().mapToInt(item -> nvl(item.getTaskScore())).sum();
        int totalScoreCompleted = tasks.stream()
                .filter(item -> "done".equalsIgnoreCase(item.getStatus()))
                .mapToInt(item -> nvl(item.getTaskScore()))
                .sum();
        int completionRate = totalScoreTarget == 0 ? 0 : (int) Math.round((totalScoreCompleted * 100.0) / totalScoreTarget);
        int overdueTasks = (int) tasks.stream()
                .filter(item -> "todo".equalsIgnoreCase(item.getStatus()) && item.getTaskDate() != null && item.getTaskDate().isBefore(today))
                .count();

        goal.setTotalTasks(totalTasks);
        goal.setCompletedTasks(completedTasks);
        goal.setTotalScoreTarget(totalScoreTarget);
        goal.setTotalScoreCompleted(totalScoreCompleted);
        goal.setCompletionRate(clamp(completionRate, 0, 100));
        goal.setRiskLevel(calcRiskLevel(completionRate, overdueTasks, daysLeft(goal.getWeekEnd(), today)));
        goal.setStatus("active");
        goalMapper.updateById(goal);
        return goalMapper.selectById(goalId);
    }

    private void writeEvent(Long goalId, Long userId, String eventType, String detail) {
        GrowthAutopilotEvent event = new GrowthAutopilotEvent();
        event.setGoalId(goalId);
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setEventDetail(detail);
        eventMapper.insert(event);
    }

    private GrowthAutopilotDashboardResponse emptyDashboard(LocalDate weekStart) {
        GrowthAutopilotDashboardResponse response = new GrowthAutopilotDashboardResponse();
        response.setHasPlan(false);
        response.setWeekStart(formatDate(weekStart));
        response.setWeekEnd(formatDate(weekStart.plusDays(6)));
        response.setGeneratedAt(formatDateTime(LocalDateTime.now()));

        GrowthAutopilotDashboardResponse.Summary summary = new GrowthAutopilotDashboardResponse.Summary();
        summary.setRiskLevel("low");
        summary.setRiskText("本周尚未生成自动驾驶计划，先生成周目标即可开始。");
        response.setSummary(summary);

        GrowthAutopilotDashboardResponse.TargetProfile targetProfile = new GrowthAutopilotDashboardResponse.TargetProfile();
        targetProfile.setTargetRole("通用");
        targetProfile.setWeeklyHours(DEFAULT_WEEKLY_HOURS);
        targetProfile.setNote("先设置岗位和每周投入时长，系统会自动拆解今日任务包。");
        response.setTargetProfile(targetProfile);
        return response;
    }

    private List<ModuleTemplate> buildModuleTemplates(String targetRole) {
        String role = StrUtil.blankToDefault(targetRole, "通用").toLowerCase(Locale.ROOT);
        List<ModuleTemplate> templates = new ArrayList<>();
        templates.add(new ModuleTemplate("oj", "OJ 冲刺", 0.26, "/oj", 80,
                List.of("完成 2 道算法题并复盘", "刷 1 道中等题 + 1 道基础题", "回顾错题并重提 1 次"), "OJ 追分任务"));
        templates.add(new ModuleTemplate("interview", "题库学习", 0.22, "/interview", 70,
                List.of("完成 4 题高频题学习", "清理 3 题逾期复习题", "完成 2 题深度复盘"), "题库补齐任务"));
        templates.add(new ModuleTemplate("flashcard", "闪卡记忆", 0.16, "/flashcard/study", 50,
                List.of("完成 30 张闪卡复习", "新增 12 张重点闪卡", "完成 2 轮间隔复习"), "闪卡追赶任务"));
        templates.add(new ModuleTemplate("plan", "计划打卡", 0.14, "/plan", 45,
                List.of("完成 1 次关键计划打卡", "补齐昨日计划复盘", "更新今日执行清单"), "计划执行补位"));
        templates.add(new ModuleTemplate("mock", "模拟面试", 0.14, "/mock-interview/config", 50,
                List.of("完成 1 轮 AI 模拟面试", "复盘 1 份面试报告", "针对薄弱项追问训练"), "模拟面试冲刺"));
        templates.add(new ModuleTemplate("points", "积分打卡", 0.08, "/points", 35,
                List.of("完成今日积分打卡", "补做 1 次高收益积分任务", "复盘积分成长记录"), "积分稳定任务"));

        if (containsAny(role, "算法", "backend", "后端", "java", "golang", "c++")) {
            templates = rebalance(templates, Map.of("oj", 0.33, "interview", 0.24, "flashcard", 0.14, "plan", 0.11, "mock", 0.12, "points", 0.06));
        } else if (containsAny(role, "frontend", "前端", "react", "vue")) {
            templates = rebalance(templates, Map.of("oj", 0.2, "interview", 0.27, "flashcard", 0.2, "plan", 0.13, "mock", 0.14, "points", 0.06));
        } else if (containsAny(role, "测试", "qa", "sdet")) {
            templates = rebalance(templates, Map.of("oj", 0.18, "interview", 0.28, "flashcard", 0.18, "plan", 0.16, "mock", 0.14, "points", 0.06));
        }
        return templates;
    }

    private List<ModuleTemplate> rebalance(List<ModuleTemplate> templates, Map<String, Double> weights) {
        List<ModuleTemplate> result = new ArrayList<>();
        for (ModuleTemplate template : templates) {
            result.add(new ModuleTemplate(
                    template.key(),
                    template.name(),
                    weights.getOrDefault(template.key(), template.weight()),
                    template.routePath(),
                    template.minMinutes(),
                    template.titles(),
                    template.replanTitle()
            ));
        }
        return result;
    }

    private String dayLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private String statusText(String status) {
        return switch (StrUtil.blankToDefault(status, "todo")) {
            case "done" -> "已完成";
            case "missed" -> "已错过";
            default -> "待完成";
        };
    }

    private String calcRiskLevel(int completionRate, int overdueTasks, int daysLeft) {
        if (overdueTasks >= 4 || (daysLeft <= 2 && completionRate < 60)) {
            return "high";
        }
        if (overdueTasks >= 2 || completionRate < 50) {
            return "medium";
        }
        return "low";
    }

    private String riskText(String riskLevel, int overdueTasks) {
        return switch (StrUtil.blankToDefault(riskLevel, "low")) {
            case "high" -> "当前风险较高，建议先执行重排并优先完成 P1 任务。";
            case "medium" -> overdueTasks > 0
                    ? "存在进度偏差，建议先清理逾期任务后继续推进。"
                    : "执行节奏略有波动，建议补齐低完成率模块。";
            default -> "执行节奏稳定，按当前任务包持续推进即可。";
        };
    }

    private String eventLabel(String eventType) {
        return switch (StrUtil.blankToDefault(eventType, "")) {
            case "generate" -> "生成计划";
            case "replan" -> "任务重排";
            case "complete" -> "完成任务";
            case "complete_batch" -> "批量完成";
            case "postpone" -> "任务顺延";
            default -> "系统事件";
        };
    }

    private boolean containsAny(String source, String... keys) {
        if (StrUtil.isBlank(source) || keys == null) {
            return false;
        }
        for (String key : keys) {
            if (StrUtil.isNotBlank(key) && source.contains(key.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private LocalDate normalizeWeekStart(LocalDate date) {
        LocalDate base = date == null ? LocalDate.now() : date;
        return base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private Integer normalizeWeeklyHours(Integer weeklyHours) {
        if (weeklyHours == null || weeklyHours <= 0) {
            return DEFAULT_WEEKLY_HOURS;
        }
        return clamp(weeklyHours, MIN_WEEKLY_HOURS, MAX_WEEKLY_HOURS);
    }

    private String normalizeRole(String role) {
        String value = StrUtil.blankToDefault(role, "通用").trim();
        return value.isEmpty() ? "通用" : value;
    }

    private List<LocalDate> listDates(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            return Collections.emptyList();
        }
        List<LocalDate> result = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            result.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private int daysLeft(LocalDate endDate, LocalDate today) {
        if (endDate == null || today == null) {
            return 0;
        }
        if (today.isAfter(endDate)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(today, endDate) + 1;
    }

    private LocalDate maxDate(LocalDate left, LocalDate right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATETIME_FORMAT);
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ModuleTemplate(
            String key,
            String name,
            double weight,
            String routePath,
            int minMinutes,
            List<String> titles,
            String replanTitle
    ) {
    }
}
