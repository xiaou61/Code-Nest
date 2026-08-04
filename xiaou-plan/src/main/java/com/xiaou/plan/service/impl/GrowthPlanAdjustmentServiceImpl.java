package com.xiaou.plan.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.plan.domain.GrowthAutopilotGoal;
import com.xiaou.plan.domain.GrowthAutopilotRevision;
import com.xiaou.plan.domain.GrowthAutopilotTask;
import com.xiaou.plan.dto.GrowthPlanAdjustmentApplyResult;
import com.xiaou.plan.dto.GrowthPlanAdjustmentCommand;
import com.xiaou.plan.dto.GrowthPlanAdjustmentPreview;
import com.xiaou.plan.dto.GrowthPlanTaskChange;
import com.xiaou.plan.growth.planner.GrowthPlanConstraintPlanner;
import com.xiaou.plan.mapper.GrowthAutopilotEventMapper;
import com.xiaou.plan.mapper.GrowthAutopilotGoalMapper;
import com.xiaou.plan.mapper.GrowthAutopilotRevisionMapper;
import com.xiaou.plan.mapper.GrowthAutopilotTaskMapper;
import com.xiaou.plan.service.GrowthPlanAdjustmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Growth Coach 对已有计划任务的版本化调整实现。
 */
@Service
@RequiredArgsConstructor
public class GrowthPlanAdjustmentServiceImpl implements GrowthPlanAdjustmentService {

    private final GrowthAutopilotGoalMapper goalMapper;
    private final GrowthAutopilotTaskMapper taskMapper;
    private final GrowthAutopilotEventMapper eventMapper;
    private final GrowthAutopilotRevisionMapper revisionMapper;
    private final ObjectMapper objectMapper;

    private final GrowthPlanConstraintPlanner planner = new GrowthPlanConstraintPlanner();

    @Override
    @Transactional(readOnly = true)
    public GrowthPlanAdjustmentPreview preview(Long userId, GrowthPlanAdjustmentCommand command) {
        if (userId == null) {
            throw new BusinessException("用户信息不能为空");
        }
        if (command == null || command.getWeekStart() == null) {
            throw new BusinessException("计划周不能为空");
        }
        GrowthAutopilotGoal goal = goalMapper.selectByUserAndWeek(userId, command.getWeekStart());
        if (goal == null) {
            throw new BusinessException("当前周尚未生成自动驾驶计划");
        }
        return planner.preview(goal, taskMapper.selectByGoalId(goal.getId()), command, LocalDate.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrowthPlanAdjustmentApplyResult apply(Long userId,
                                                  GrowthPlanAdjustmentPreview preview,
                                                  String actionRunId) {
        if (userId == null || preview == null || preview.getGoalId() == null) {
            throw new BusinessException("计划调整预览无效");
        }
        GrowthAutopilotGoal goal = goalMapper.selectById(preview.getGoalId());
        if (goal == null || !userId.equals(goal.getUserId())) {
            throw new BusinessException("当前周计划不存在");
        }

        int baseVersion = normalizePlanVersion(preview.getBasePlanVersion());
        if (normalizePlanVersion(goal.getPlanVersion()) != baseVersion) {
            return versionConflict();
        }

        List<GrowthAutopilotTask> lockedTasks = taskMapper.selectByGoalIdForUpdate(goal.getId());
        if (!matchesPreviewedTodoSnapshot(lockedTasks, preview)) {
            return versionConflict();
        }

        int nextVersion = baseVersion + 1;
        int updated = goalMapper.advancePlanVersion(
                goal.getId(),
                userId,
                baseVersion,
                nextVersion,
                normalizeRole(preview.getTargetRole(), goal.getTargetRole()),
                preview.getBudgetMinutes(),
                hoursFromMinutes(preview.getBudgetMinutes()),
                actionRunId
        );
        if (updated != 1) {
            return versionConflict();
        }

        for (GrowthPlanTaskChange change : preview.getChanges()) {
            applyTaskChange(goal, userId, nextVersion, change);
        }

        GrowthAutopilotGoal refreshedGoal = refreshGoalMetrics(goal.getId());
        writeRevision(refreshedGoal, preview, actionRunId, baseVersion, nextVersion);
        writeEvent(refreshedGoal.getId(), userId, "ai_adjust",
                "AI 成长教练已按 " + preview.getBudgetMinutes() + " 分钟预算调整本周计划");

        GrowthPlanAdjustmentApplyResult result = new GrowthPlanAdjustmentApplyResult();
        result.setApplied(true);
        result.setPlanVersion(nextVersion);
        result.setMessage("计划已更新");
        return result;
    }

    /**
     * Preview 只对当时全部待办任务成立。确认前锁住任务行并逐一核对，避免把新完成、
     * 新增或已失效的任务写回旧计划。
     */
    private boolean matchesPreviewedTodoSnapshot(List<GrowthAutopilotTask> lockedTasks,
                                                 GrowthPlanAdjustmentPreview preview) {
        if (preview.getChanges() == null || preview.getChanges().isEmpty()) {
            return false;
        }
        Map<Long, GrowthPlanTaskChange> changesByTaskId = new HashMap<>();
        for (GrowthPlanTaskChange change : preview.getChanges()) {
            if (change == null || change.getTaskId() == null
                    || !isSupportedOperation(change.getOperation())
                    || changesByTaskId.put(change.getTaskId(), change) != null) {
                return false;
            }
        }

        Set<Long> currentTodoIds = new HashSet<>();
        for (GrowthAutopilotTask task : lockedTasks == null ? List.<GrowthAutopilotTask>of() : lockedTasks) {
            if (!"todo".equalsIgnoreCase(task.getStatus())) {
                continue;
            }
            if (task.getId() == null) {
                return false;
            }
            currentTodoIds.add(task.getId());
            GrowthPlanTaskChange change = changesByTaskId.get(task.getId());
            if (change == null || !matchesResourceSnapshot(task, change)) {
                return false;
            }
        }
        return currentTodoIds.equals(changesByTaskId.keySet());
    }

    private boolean isSupportedOperation(String operation) {
        return "KEEP".equals(operation) || "MOVE".equals(operation) || "SUPERSEDE".equals(operation);
    }

    private boolean matchesResourceSnapshot(GrowthAutopilotTask task, GrowthPlanTaskChange change) {
        String resourceType = StringUtils.hasText(task.getResourceType()) ? task.getResourceType() : "route";
        String resourceId = StringUtils.hasText(task.getResourceId()) ? task.getResourceId() : task.getRoutePath();
        return Objects.equals(resourceType, change.getResourceType())
                && Objects.equals(resourceId, change.getResourceId())
                && Objects.equals(task.getResourceVersion(), change.getResourceVersion())
                && Objects.equals(task.getRoutePath(), change.getRoutePath());
    }

    private void applyTaskChange(GrowthAutopilotGoal goal,
                                 Long userId,
                                 int nextVersion,
                                 GrowthPlanTaskChange change) {
        if (change == null || change.getTaskId() == null || !StringUtils.hasText(change.getOperation())) {
            throw new BusinessException("计划任务变更无效");
        }
        int updated;
        if ("KEEP".equals(change.getOperation()) || "MOVE".equals(change.getOperation())) {
            updated = taskMapper.applyPlanAdjustmentTask(
                    change.getTaskId(),
                    goal.getId(),
                    userId,
                    change.getToDate(),
                    nextVersion,
                    change.getResourceType(),
                    change.getResourceId(),
                    change.getResourceVersion(),
                    change.getReason()
            );
        } else if ("SUPERSEDE".equals(change.getOperation())) {
            updated = taskMapper.supersedePlanAdjustmentTask(
                    change.getTaskId(),
                    goal.getId(),
                    userId,
                    nextVersion,
                    change.getReason()
            );
        } else {
            throw new BusinessException("不支持的计划任务变更");
        }
        if (updated != 1) {
            throw new BusinessException("计划在确认前已发生变化，请重新生成预览");
        }
    }

    private GrowthAutopilotGoal refreshGoalMetrics(Long goalId) {
        GrowthAutopilotGoal goal = goalMapper.selectById(goalId);
        if (goal == null) {
            throw new BusinessException("周计划不存在");
        }
        List<GrowthAutopilotTask> activeTasks = taskMapper.selectByGoalId(goalId).stream()
                .filter(this::isActiveTask)
                .toList();
        int totalTasks = activeTasks.size();
        int completedTasks = (int) activeTasks.stream().filter(this::isDone).count();
        int targetScore = activeTasks.stream().mapToInt(task -> nvl(task.getTaskScore())).sum();
        int completedScore = activeTasks.stream()
                .filter(this::isDone)
                .mapToInt(task -> nvl(task.getTaskScore()))
                .sum();
        int completionRate = targetScore == 0 ? 0 : (int) Math.round(completedScore * 100.0 / targetScore);

        goal.setTotalTasks(totalTasks);
        goal.setCompletedTasks(completedTasks);
        goal.setTotalScoreTarget(targetScore);
        goal.setTotalScoreCompleted(completedScore);
        goal.setCompletionRate(Math.max(0, Math.min(100, completionRate)));
        goal.setRiskLevel(resolveRiskLevel(activeTasks, completionRate, LocalDate.now()));
        goal.setStatus("active");
        goalMapper.updateMetrics(goal);
        return goalMapper.selectById(goalId);
    }

    private void writeRevision(GrowthAutopilotGoal goal,
                               GrowthPlanAdjustmentPreview preview,
                               String actionRunId,
                               int baseVersion,
                               int nextVersion) {
        GrowthAutopilotRevision revision = new GrowthAutopilotRevision();
        revision.setGoalId(goal.getId());
        revision.setUserId(goal.getUserId());
        revision.setVersion(nextVersion);
        revision.setBaseVersion(baseVersion);
        revision.setSource("AI_ADJUST");
        revision.setActionRunId(actionRunId);
        revision.setConstraintJson(toJson(Map.of(
                "budgetMinutes", preview.getBudgetMinutes(),
                "completedTasksPreserved", preview.isCompletedTasksPreserved(),
                "allTasksResourceBacked", preview.isAllTasksResourceBacked()
        )));
        revision.setDiffJson(toJson(preview.getChanges()));
        revision.setSnapshotHash(sha256(toJson(preview)));
        revisionMapper.insert(revision);
    }

    private void writeEvent(Long goalId, Long userId, String eventType, String detail) {
        com.xiaou.plan.domain.GrowthAutopilotEvent event = new com.xiaou.plan.domain.GrowthAutopilotEvent();
        event.setGoalId(goalId);
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setEventDetail(detail);
        eventMapper.insert(event);
    }

    private GrowthPlanAdjustmentApplyResult versionConflict() {
        GrowthPlanAdjustmentApplyResult result = new GrowthPlanAdjustmentApplyResult();
        result.setVersionConflict(true);
        result.setMessage("计划在预览后已变化，请重新生成预览");
        return result;
    }

    private boolean isActiveTask(GrowthAutopilotTask task) {
        return "todo".equalsIgnoreCase(task.getStatus()) || isDone(task);
    }

    private boolean isDone(GrowthAutopilotTask task) {
        return "done".equalsIgnoreCase(task.getStatus());
    }

    private int normalizePlanVersion(Integer value) {
        return value == null || value <= 0 ? 1 : value;
    }

    private int hoursFromMinutes(Integer minutes) {
        int value = minutes == null ? 0 : minutes;
        return Math.max(1, (int) Math.ceil(value / 60.0));
    }

    private String normalizeRole(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private String resolveRiskLevel(List<GrowthAutopilotTask> tasks, int completionRate, LocalDate today) {
        long overdue = tasks.stream()
                .filter(task -> "todo".equalsIgnoreCase(task.getStatus()))
                .filter(task -> task.getTaskDate() != null && task.getTaskDate().isBefore(today))
                .count();
        if (overdue >= 4 || completionRate < 35) {
            return "high";
        }
        if (overdue >= 2 || completionRate < 60) {
            return "medium";
        }
        return "low";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("计划快照序列化失败", exception);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format(Locale.ROOT, "%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
