package com.xiaou.web.growthcoach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.ai.prompt.growthcoach.GrowthCoachPromptSpecs;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.plan.dto.GrowthAutopilotDashboardResponse;
import com.xiaou.plan.dto.GrowthPlanAdjustmentApplyResult;
import com.xiaou.plan.dto.GrowthPlanAdjustmentCommand;
import com.xiaou.plan.dto.GrowthPlanAdjustmentPreview;
import com.xiaou.plan.service.GrowthAutopilotService;
import com.xiaou.plan.service.GrowthPlanAdjustmentService;
import com.xiaou.web.growthcoach.domain.GrowthCoachActionEvent;
import com.xiaou.web.growthcoach.domain.GrowthCoachActionRun;
import com.xiaou.web.growthcoach.dto.GrowthCoachActionRunResponse;
import com.xiaou.web.growthcoach.dto.GrowthCoachConfirmRequest;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceReference;
import com.xiaou.web.growthcoach.dto.GrowthCoachPreviewRequest;
import com.xiaou.web.growthcoach.dto.GrowthCoachTodayActionResponse;
import com.xiaou.web.growthcoach.intent.GrowthCoachIntent;
import com.xiaou.web.growthcoach.intent.GrowthCoachIntentResolver;
import com.xiaou.web.growthcoach.mapper.GrowthCoachActionEventMapper;
import com.xiaou.web.growthcoach.mapper.GrowthCoachActionRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 用户侧 Growth Coach 用例编排。
 */
@Service
@RequiredArgsConstructor
public class GrowthCoachApplicationService {

    private static final String ACTION_ID = "growth.plan.adjust";
    private static final String STATUS_PREVIEW = "PREVIEW";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_CONFLICTED = "CONFLICTED";

    private final GrowthCoachActionRunMapper actionRunMapper;
    private final GrowthCoachActionEventMapper actionEventMapper;
    private final GrowthCoachIntentResolver intentResolver;
    private final GrowthPlanAdjustmentService planAdjustmentService;
    private final GrowthAutopilotService growthAutopilotService;
    private final GrowthEvidenceQueryService growthEvidenceQueryService;
    private final GrowthSkillInsightService growthSkillInsightService;
    private final GrowthCoachFeatureGuard growthCoachFeatureGuard;
    private final GrowthCoachPlanningLock planningLock;
    private final GrowthCoachPreviewPersistenceService previewPersistenceService;
    private final GrowthCoachMetricsRecorder growthCoachMetricsRecorder;
    private final ObjectMapper objectMapper;

    public GrowthCoachActionRunResponse preview(Long userId, GrowthCoachPreviewRequest request) {
        long startedAt = System.nanoTime();
        String outcome = "failed";
        try {
            if (userId == null || request == null) {
                throw new BusinessException("计划调整请求无效");
            }
            LocalDate weekStart = normalizeCurrentWeek(request.getWeekStart());
            String requestHash = sha256(normalizeMessage(request.getMessage()) + "|" + weekStart);
            GrowthCoachActionRun existing = actionRunMapper.selectByUserAndClientRequest(userId, request.getClientRequestId());
            if (existing != null) {
                if (!requestHash.equals(existing.getRequestHash())) {
                    throw new BusinessException("相同 clientRequestId 不能用于不同的计划调整请求");
                }
                outcome = "idempotent";
                return toResponse(existing, false);
            }
            growthCoachFeatureGuard.checkPreview(userId);

            try (GrowthCoachPlanningLock.LockLease ignored = planningLock.acquire(userId)) {
                GrowthCoachActionRun concurrentRun = actionRunMapper.selectByUserAndClientRequest(
                        userId, request.getClientRequestId()
                );
                if (concurrentRun != null && requestHash.equals(concurrentRun.getRequestHash())) {
                    outcome = "idempotent";
                    return toResponse(concurrentRun, false);
                }
                if (concurrentRun != null) {
                    throw new BusinessException("相同 clientRequestId 不能用于不同的计划调整请求");
                }

                GrowthCoachIntent intent = intentResolver.resolve(request.getMessage());
                GrowthPlanAdjustmentCommand command = new GrowthPlanAdjustmentCommand();
                command.setWeekStart(weekStart);
                command.setAvailableMinutes(resolveBudgetMinutes(userId, weekStart, intent));
                command.setTargetRole(intent.getTargetRole());
                command.setPrioritizeInterview(intent.isPrioritizeInterview());
                command.setPreferredModuleKeys(growthSkillInsightService.getPrioritizedModuleKeys(userId));
                GrowthPlanAdjustmentPreview planPreview = planAdjustmentService.preview(userId, command);

                String intentJson = writeJson(intent);
                String previewJson = writeJson(planPreview);
                GrowthCoachActionRun run = new GrowthCoachActionRun();
                run.setRunId("growth-run-" + UUID.randomUUID().toString().replace("-", ""));
                run.setUserId(userId);
                run.setClientRequestId(request.getClientRequestId());
                run.setActionId(ACTION_ID);
                run.setStatus(STATUS_PREVIEW);
                run.setBasePlanVersion(planPreview.getBasePlanVersion());
                run.setRequestHash(requestHash);
                run.setMessageRedacted("已脱敏的计划调整请求");
                run.setIntentJson(intentJson);
                run.setPreviewJson(previewJson);
                run.setPreviewHash(sha256(previewJson));
                run.setPromptKey(GrowthCoachPromptSpecs.PLAN_ADJUSTMENT_INTENT.key());
                run.setPromptVersion(GrowthCoachPromptSpecs.PLAN_ADJUSTMENT_INTENT.version());
                run.setExpiresAt(LocalDateTime.now().plusMinutes(30));

                try {
                    previewPersistenceService.persistPreview(run);
                    outcome = "preview_created";
                    return toResponse(run, false);
                } catch (DuplicateKeyException exception) {
                    concurrentRun = actionRunMapper.selectByUserAndClientRequest(userId, request.getClientRequestId());
                    if (concurrentRun != null && requestHash.equals(concurrentRun.getRequestHash())) {
                        outcome = "idempotent";
                        return toResponse(concurrentRun, false);
                    }
                    throw exception;
                }
            }
        } finally {
            growthCoachMetricsRecorder.recordAction(ACTION_ID, "preview", outcome, System.nanoTime() - startedAt);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthCoachActionRunResponse confirm(Long userId,
                                                String runId,
                                                GrowthCoachConfirmRequest request) {
        long startedAt = System.nanoTime();
        String outcome = "failed";
        try {
            GrowthCoachActionRun run = requireRunForUpdate(userId, runId);
            if (STATUS_EXECUTED.equals(run.getStatus())) {
                outcome = "idempotent";
                return toResponse(run, true);
            }
            if (!STATUS_PREVIEW.equals(run.getStatus())) {
                outcome = "not_confirmable";
                return toResponse(run, false);
            }
            if (run.getExpiresAt() != null && !run.getExpiresAt().isAfter(LocalDateTime.now())) {
                transition(run, STATUS_EXPIRED, "PREVIEW_EXPIRED", "预览已过期，请重新生成");
                outcome = "expired";
                return toResponse(run, false);
            }
            if (request == null || !StringUtils.hasText(request.getPreviewHash())
                    || !request.getPreviewHash().equals(run.getPreviewHash())) {
                throw new BusinessException("预览摘要不匹配，请重新生成预览");
            }
            growthCoachFeatureGuard.checkConfirm(userId);

            GrowthPlanAdjustmentPreview preview = readJson(run.getPreviewJson(), GrowthPlanAdjustmentPreview.class);
            GrowthPlanAdjustmentApplyResult applyResult = planAdjustmentService.apply(userId, preview, run.getRunId());
            if (applyResult.isVersionConflict()) {
                transition(run, STATUS_CONFLICTED, "PLAN_VERSION_CONFLICT", applyResult.getMessage());
                growthCoachMetricsRecorder.recordConflict(ACTION_ID, "plan_version_or_task_snapshot");
                outcome = "conflicted";
                return toResponse(run, false);
            }
            if (!applyResult.isApplied()) {
                throw new BusinessException("计划调整未执行");
            }

            int marked = actionRunMapper.markExecuted(
                    run.getRunId(),
                    userId,
                    applyResult.getPlanVersion(),
                    writeJson(applyResult)
            );
            if (marked != 1) {
                throw new BusinessException("计划确认状态发生变化，请刷新后重试");
            }
            String previousStatus = run.getStatus();
            run.setStatus(STATUS_EXECUTED);
            run.setTargetPlanVersion(applyResult.getPlanVersion());
            run.setResultJson(writeJson(applyResult));
            run.setExecutedAt(LocalDateTime.now());
            writeEvent(run.getRunId(), previousStatus, STATUS_EXECUTED, "plan_executed", run.getResultJson());
            outcome = "executed";
            return toResponse(run, true);
        } finally {
            growthCoachMetricsRecorder.recordAction(ACTION_ID, "confirm", outcome, System.nanoTime() - startedAt);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public GrowthCoachActionRunResponse cancel(Long userId, String runId) {
        GrowthCoachActionRun run = requireRunForUpdate(userId, runId);
        if (STATUS_PREVIEW.equals(run.getStatus())) {
            transition(run, STATUS_CANCELLED, "", "用户取消预览");
        }
        return toResponse(run, false);
    }

    public GrowthCoachActionRunResponse getRun(Long userId, String runId) {
        GrowthCoachActionRun run = actionRunMapper.selectByRunIdAndUser(runId, userId);
        if (run == null) {
            throw new BusinessException("计划调整记录不存在");
        }
        return toResponse(run, STATUS_EXECUTED.equals(run.getStatus()));
    }

    public GrowthCoachTodayActionResponse getTodayAction(Long userId) {
        GrowthAutopilotDashboardResponse dashboard = growthAutopilotService.getDashboard(userId, null);
        GrowthAutopilotDashboardResponse.TaskItem task = findBestPendingTask(dashboard);
        if (task == null) {
            return null;
        }
        GrowthCoachTodayActionResponse response = new GrowthCoachTodayActionResponse();
        response.setTaskId(task.getTaskId());
        response.setTitle(task.getTitle());
        response.setPlannedMinutes(task.getPlannedMinutes());
        response.setReason(StringUtils.hasText(task.getSelectionReason())
                ? task.getSelectionReason()
                : "当前计划中优先级最高的待完成动作");
        response.setExpectedChange("完成后将更新本周完成进度，并重新计算下一步行动");
        response.setStartRoute(task.getRoutePath());
        List<GrowthEvidenceReference> evidenceRefs = growthEvidenceQueryService.getRecentReferences(userId, 2);
        evidenceRefs = evidenceRefs == null ? List.of() : evidenceRefs;
        response.setEvidenceRefs(evidenceRefs);
        response.setSelectionVersion(selectionVersion(task, dashboard, evidenceRefs));
        return response;
    }

    private GrowthAutopilotDashboardResponse.TaskItem findBestPendingTask(GrowthAutopilotDashboardResponse dashboard) {
        if (dashboard == null || dashboard.getDayBuckets() == null) {
            return null;
        }
        List<GrowthAutopilotDashboardResponse.DayTaskBucket> buckets = dashboard.getDayBuckets().stream()
                .sorted(Comparator.comparing(bucket -> !Boolean.TRUE.equals(bucket.getToday())))
                .toList();
        return buckets.stream()
                .flatMap(bucket -> bucket.getTasks() == null ? java.util.stream.Stream.empty() : bucket.getTasks().stream())
                .filter(task -> "todo".equalsIgnoreCase(task.getStatus()))
                .sorted(Comparator.comparingInt(task -> priorityWeight(task.getPriority())))
                .findFirst()
                .orElse(null);
    }

    private GrowthCoachActionRun requireRunForUpdate(Long userId, String runId) {
        GrowthCoachActionRun run = actionRunMapper.selectByRunIdAndUserForUpdate(runId, userId);
        if (run == null) {
            throw new BusinessException("计划调整记录不存在");
        }
        return run;
    }

    private void transition(GrowthCoachActionRun run, String targetStatus, String errorCode, String errorMessage) {
        int updated = actionRunMapper.updateStatus(
                run.getRunId(), run.getUserId(), run.getStatus(), targetStatus, errorCode, errorMessage
        );
        if (updated != 1) {
            throw new BusinessException("计划调整状态发生变化，请刷新后重试");
        }
        String previousStatus = run.getStatus();
        run.setStatus(targetStatus);
        run.setErrorCode(errorCode);
        run.setErrorMessage(errorMessage);
        writeEvent(run.getRunId(), previousStatus, targetStatus, "status_changed", writeJson(Map.of(
                "errorCode", errorCode,
                "message", errorMessage
        )));
    }

    private GrowthCoachActionRunResponse toResponse(GrowthCoachActionRun run, boolean includeDashboard) {
        GrowthCoachActionRunResponse response = new GrowthCoachActionRunResponse();
        response.setRunId(run.getRunId());
        response.setStatus(run.getStatus());
        response.setBasePlanVersion(run.getBasePlanVersion());
        response.setTargetPlanVersion(run.getTargetPlanVersion());
        response.setIntent(readJsonOrNull(run.getIntentJson(), GrowthCoachIntent.class));
        response.setPreview(readJsonOrNull(run.getPreviewJson(), GrowthPlanAdjustmentPreview.class));
        response.setPreviewHash(run.getPreviewHash());
        response.setExpiresAt(run.getExpiresAt());
        response.setErrorCode(run.getErrorCode());
        response.setErrorMessage(run.getErrorMessage());
        if (includeDashboard) {
            GrowthPlanAdjustmentPreview preview = response.getPreview();
            response.setDashboard(growthAutopilotService.getDashboard(
                    run.getUserId(), preview == null ? null : preview.getWeekStart()
            ));
        }
        return response;
    }

    private int resolveBudgetMinutes(Long userId, LocalDate weekStart, GrowthCoachIntent intent) {
        if (intent != null && intent.getAvailableMinutes() != null && intent.getAvailableMinutes() > 0) {
            return intent.getAvailableMinutes();
        }
        GrowthAutopilotDashboardResponse dashboard = growthAutopilotService.getDashboard(userId, weekStart);
        Integer weeklyHours = dashboard.getTargetProfile() == null ? null : dashboard.getTargetProfile().getWeeklyHours();
        return Math.max(15, (weeklyHours == null ? 8 : weeklyHours) * 60);
    }

    private LocalDate normalizeCurrentWeek(LocalDate requestedWeekStart) {
        LocalDate currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate normalized = (requestedWeekStart == null ? currentWeek : requestedWeekStart)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (!normalized.equals(currentWeek)) {
            throw new BusinessException("当前仅支持调整本周计划");
        }
        return normalized;
    }

    private void writeEvent(String runId,
                            String fromStatus,
                            String toStatus,
                            String eventType,
                            String detailJson) {
        GrowthCoachActionEvent event = new GrowthCoachActionEvent();
        event.setRunId(runId);
        event.setSequenceNo(actionEventMapper.selectNextSequence(runId));
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setEventType(eventType);
        event.setDetailJson(detailJson);
        actionEventMapper.insert(event);
    }

    private String normalizeMessage(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("计划调整数据序列化失败", exception);
        }
    }

    private <T> T readJson(String value, Class<T> type) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("计划调整数据缺失");
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("计划调整数据损坏", exception);
        }
    }

    private <T> T readJsonOrNull(String value, Class<T> type) {
        return StringUtils.hasText(value) ? readJson(value, type) : null;
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

    private int selectionVersion(GrowthAutopilotDashboardResponse.TaskItem task,
                                 GrowthAutopilotDashboardResponse dashboard,
                                 List<GrowthEvidenceReference> evidenceRefs) {
        StringBuilder fingerprint = new StringBuilder();
        fingerprint.append(task.getTaskId()).append('|')
                .append(task.getStatus()).append('|')
                .append(task.getTaskDate()).append('|')
                .append(task.getRoutePath()).append('|')
                .append(task.getSelectionReason()).append('|')
                .append(dashboard == null ? 0 : dashboard.getPlanVersion());
        for (GrowthEvidenceReference evidence : evidenceRefs) {
            if (evidence == null) {
                continue;
            }
            fingerprint.append('|').append(evidence.getEvidenceId())
                    .append('|').append(evidence.getQualityLevel())
                    .append('|').append(evidence.getObservedAt());
        }
        return Math.floorMod(sha256(fingerprint.toString()).hashCode(), Integer.MAX_VALUE);
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
