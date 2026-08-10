package com.xiaou.system.agent.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.system.agent.AgentOperator;
import com.xiaou.system.agent.AgentToolCall;
import com.xiaou.system.agent.AgentToolDefinition;
import com.xiaou.system.domain.SysAgentTask;
import com.xiaou.system.domain.SysAgentTaskEvent;
import com.xiaou.system.domain.SysAgentTaskStep;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentChatResponse;
import com.xiaou.system.dto.AgentTaskCancelRequest;
import com.xiaou.system.dto.AgentTaskCreateRequest;
import com.xiaou.system.dto.AgentTaskEventPageResponse;
import com.xiaou.system.dto.AgentTaskEventResponse;
import com.xiaou.system.dto.AgentTaskInputRequest;
import com.xiaou.system.dto.AgentTaskPauseRequest;
import com.xiaou.system.dto.AgentTaskResponse;
import com.xiaou.system.mapper.SysAgentTaskMapper;
import com.xiaou.system.mapper.SysAgentTaskEventMapper;
import com.xiaou.system.mapper.SysAgentTaskStepMapper;
import com.xiaou.system.service.SysAgentAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.NoTransactionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MySQL-backed lifecycle and compare-and-set transitions for durable agent tasks.
 */
@Slf4j
@Service
public class AgentTaskStateService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final SysAgentTaskMapper taskMapper;
    private final SysAgentTaskStepMapper stepMapper;
    private final SysAgentAuditService auditService;
    private final ObjectMapper objectMapper;
    private final AgentTaskProperties properties;
    private final AgentTaskResponseMapper responseMapper;
    private final AgentTaskMetricsRecorder metricsRecorder;
    private final SysAgentTaskEventMapper eventMapper;

    public AgentTaskStateService(
            SysAgentTaskMapper taskMapper,
            SysAgentTaskStepMapper stepMapper,
            SysAgentAuditService auditService,
            ObjectMapper objectMapper,
            AgentTaskProperties properties,
            AgentTaskResponseMapper responseMapper,
            AgentTaskMetricsRecorder metricsRecorder
    ) {
        this(taskMapper, stepMapper, auditService, objectMapper, properties,
                responseMapper, metricsRecorder, null);
    }

    @Autowired
    public AgentTaskStateService(
            SysAgentTaskMapper taskMapper,
            SysAgentTaskStepMapper stepMapper,
            SysAgentAuditService auditService,
            ObjectMapper objectMapper,
            AgentTaskProperties properties,
            AgentTaskResponseMapper responseMapper,
            AgentTaskMetricsRecorder metricsRecorder,
            SysAgentTaskEventMapper eventMapper
    ) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.responseMapper = responseMapper;
        this.metricsRecorder = metricsRecorder;
        this.eventMapper = eventMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTaskResponse create(AgentTaskCreateRequest request, AgentOperator operator) {
        if (request == null || !StringUtils.hasText(request.getGoal())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_ERROR, "任务目标不能为空");
        }
        if (operator == null || operator.id() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "当前管理员身份不可用");
        }
        LocalDateTime now = LocalDateTime.now();
        SysAgentTask task = new SysAgentTask();
        task.setTaskId("agent-task-" + UUID.randomUUID());
        task.setGoal(limit(request.getGoal().trim(), 4_000));
        task.setSessionId(normalizeNullable(request.getSessionId()));
        task.setStatus(AgentTaskStatus.QUEUED.name());
        task.setOperatorId(operator.id());
        task.setOperatorName(limit(operator.name(), 50));
        task.setMaxSteps(properties.normalizedMaxSteps());
        task.setCompletedSteps(0);
        task.setCurrentStepOrder(0);
        task.setCreatedTime(now);
        task.setUpdatedTime(now);
        if (taskMapper.insert(task) != 1) {
            throw new IllegalStateException("无法创建管理员智能体任务");
        }
        appendEvent(task.getTaskId(), AgentTaskEventType.TASK_CREATED, null,
                "OPERATOR", String.valueOf(operator.id()), null, task.getStatus(),
                Map.of("maxSteps", task.getMaxSteps()));
        return responseMapper.toResponse(task, List.of());
    }

    public List<AgentTaskResponse> listOwned(Long operatorId, String status, Integer limit) {
        String normalizedStatus = normalize(status);
        if (StringUtils.hasText(normalizedStatus) && AgentTaskStatus.from(normalizedStatus) == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_ERROR, "任务状态不合法");
        }
        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 100));
        List<SysAgentTask> tasks = taskMapper.selectOwnedList(operatorId, normalizedStatus, safeLimit);
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        List<AgentTaskResponse> responses = new ArrayList<>();
        for (SysAgentTask task : tasks) {
            responses.add(responseMapper.toResponse(task, safeSteps(task.getTaskId())));
        }
        return responses;
    }

    public AgentTaskResponse detail(String taskId, Long operatorId) {
        SysAgentTask task = requireOwned(taskId, operatorId);
        return responseMapper.toResponse(task, safeSteps(taskId));
    }

    @Transactional(readOnly = true)
    public AgentTaskEventPageResponse events(String taskId, Long operatorId, Long afterCursor, Integer limit) {
        requireOwned(taskId, operatorId);
        long safeAfter = afterCursor == null ? 0L : afterCursor;
        if (safeAfter < 0L) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_ERROR, "事件游标不能为负数");
        }
        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 100));
        List<SysAgentTaskEvent> records = eventMapper == null
                ? List.of()
                : eventMapper.selectOwnedAfter(taskId, operatorId, safeAfter, safeLimit + 1);
        List<SysAgentTaskEvent> safeRecords = records == null ? List.of() : records;
        boolean hasMore = safeRecords.size() > safeLimit;
        List<SysAgentTaskEvent> page = hasMore
                ? safeRecords.subList(0, safeLimit)
                : safeRecords;
        AgentTaskEventPageResponse response = new AgentTaskEventPageResponse();
        response.setEvents(page.stream().map(this::toEventResponse).toList());
        response.setHasMore(hasMore);
        if (hasMore && !page.isEmpty()) {
            response.setNextCursor(page.get(page.size() - 1).getId());
        }
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTaskResponse pause(String taskId, Long operatorId, AgentTaskPauseRequest request) {
        SysAgentTask task = requireOwned(taskId, operatorId);
        AgentTaskStatus current = AgentTaskStatus.from(task.getStatus());
        if (current != AgentTaskStatus.QUEUED && current != AgentTaskStatus.RUNNING) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "当前任务状态不能暂停");
        }
        String reason = request == null || !StringUtils.hasText(request.getReason())
                ? "管理员暂停任务"
                : limit(request.getReason().trim(), 500);
        if (!transitionTask(task, current, AgentTaskStatus.PAUSED, null,
                AgentTaskEventType.TASK_PAUSED, "OPERATOR", String.valueOf(operatorId),
                Map.of("reason", reason))) {
            throw new BusinessException(ResultCode.CONFLICT, "任务状态已变化，暂停未生效");
        }
        return responseForOwned(taskId, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTaskResponse resume(String taskId, Long operatorId) {
        SysAgentTask task = requireOwned(taskId, operatorId);
        if (!AgentTaskStatus.PAUSED.name().equals(task.getStatus())) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "当前任务不在暂停状态");
        }
        if (!transitionTask(task, AgentTaskStatus.PAUSED, AgentTaskStatus.QUEUED, null,
                AgentTaskEventType.TASK_RESUMED, "OPERATOR", String.valueOf(operatorId), Map.of())) {
            throw new BusinessException(ResultCode.CONFLICT, "任务状态已变化，恢复未生效");
        }
        return responseForOwned(taskId, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTaskResponse submitInput(String taskId, Long operatorId, AgentTaskInputRequest request) {
        SysAgentTask task = requireOwned(taskId, operatorId);
        if (!AgentTaskStatus.WAITING_INPUT.name().equals(task.getStatus())) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "当前任务不在等待输入状态");
        }
        Map<String, Object> merged = mergeWorkflowContext(task.getWorkflowContextJson(),
                request == null ? null : request.getInput());
        String contextJson = toJson(merged, properties.normalizedMaxWorkflowContextJsonChars());
        task.setWorkflowContextJson(contextJson);
        appendEvent(taskId, AgentTaskEventType.INPUT_SUBMITTED, null,
                "OPERATOR", String.valueOf(operatorId), AgentTaskStatus.WAITING_INPUT.name(),
                AgentTaskStatus.QUEUED.name(), Map.of("keys", new ArrayList<>(merged.keySet()),
                        "keyCount", merged.size()));
        if (!transitionTask(task, AgentTaskStatus.WAITING_INPUT, AgentTaskStatus.QUEUED, null,
                null, "OPERATOR", String.valueOf(operatorId), Map.of())) {
            throw new BusinessException(ResultCode.CONFLICT, "任务状态已变化，补充输入未生效");
        }
        return responseForOwned(taskId, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean waitForInput(String taskId, String leaseOwner, String reason) {
        SysAgentTask task = taskMapper.selectByTaskId(taskId);
        if (!isRunningLease(task, leaseOwner)) {
            return false;
        }
        return transitionTask(task, AgentTaskStatus.RUNNING, AgentTaskStatus.WAITING_INPUT, leaseOwner,
                AgentTaskEventType.TASK_WAITING_INPUT, "WORKER", leaseOwner,
                Map.of("reason", limit(reason, 500)));
    }

    public Map<String, Object> readWorkflowContext(SysAgentTask task) {
        if (task == null || !StringUtils.hasText(task.getWorkflowContextJson())) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(task.getWorkflowContextJson());
            if (root == null || !root.isObject()) {
                return Map.of();
            }
            return objectMapper.convertValue(root, MAP_TYPE);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            log.warn("管理员智能体任务上下文解析失败，taskId={}, reason={}",
                    task.getTaskId(), exception.getClass().getSimpleName());
            return Map.of();
        }
    }

    public List<String> listClaimableIds(int limit) {
        List<String> ids = taskMapper.selectClaimableIds(Math.max(1, Math.min(limit, 20)));
        return ids == null ? List.of() : ids;
    }

    public List<String> listStaleRunningIds(LocalDateTime staleBefore, int limit) {
        if (staleBefore == null) {
            return List.of();
        }
        List<String> ids = taskMapper.selectStaleRunningIds(
                staleBefore,
                Math.max(1, Math.min(limit, 20))
        );
        return ids == null ? List.of() : ids;
    }

    public boolean renewLease(String taskId, String leaseOwner) {
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(leaseOwner)) {
            return false;
        }
        return taskMapper.heartbeat(taskId, leaseOwner, LocalDateTime.now()) == 1;
    }

    public long countQueuedTasks() {
        return Math.max(0L, taskMapper.countByStatus(AgentTaskStatus.QUEUED.name()));
    }

    @Transactional(rollbackFor = Exception.class)
    public SysAgentTask claim(String taskId, String leaseOwner) {
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(leaseOwner)) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (taskMapper.claim(taskId, limit(leaseOwner.trim(), 120), now) != 1) {
            return null;
        }
        appendEvent(taskId, AgentTaskEventType.TASK_CLAIMED, null,
                "WORKER", limit(leaseOwner.trim(), 120), AgentTaskStatus.QUEUED.name(),
                AgentTaskStatus.RUNNING.name(), Map.of());
        return taskMapper.selectByTaskId(taskId);
    }

    public SysAgentTask load(String taskId) {
        return taskMapper.selectByTaskId(taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTaskSnapshot loadExecutionSnapshot(String taskId, String leaseOwner) {
        LocalDateTime now = LocalDateTime.now();
        if (taskMapper.heartbeat(taskId, leaseOwner, now) != 1) {
            return null;
        }
        SysAgentTask task = taskMapper.selectByTaskId(taskId);
        if (!isRunningLease(task, leaseOwner)) {
            return null;
        }
        return new AgentTaskSnapshot(task, safeSteps(taskId));
    }

    public AgentToolCall readStoredCall(SysAgentTaskStep step) {
        if (step == null || !StringUtils.hasText(step.getToolName())) {
            throw new IllegalArgumentException("持久化任务步骤缺少工具名称");
        }
        AgentToolCall call = new AgentToolCall();
        call.setToolName(step.getToolName());
        call.setSummary(step.getInputSummary());
        try {
            call.setInput(StringUtils.hasText(step.getInputJson())
                    ? objectMapper.readValue(step.getInputJson(), MAP_TYPE)
                    : new LinkedHashMap<>());
            return call;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("持久化任务步骤输入无法解析", e);
        }
    }

    public boolean hasFingerprint(String taskId, AgentToolCall call) {
        String fingerprint = AgentTaskFingerprint.of(call.getToolName(), call.getInput(), objectMapper);
        return stepMapper.existsFingerprint(taskId, fingerprint) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTaskStepExecution startStep(
            String taskId,
            String leaseOwner,
            AgentToolCall call,
            AgentToolDefinition definition,
            boolean fallback
    ) {
        AgentTaskSnapshot snapshot = loadExecutionSnapshot(taskId, leaseOwner);
        if (snapshot == null) {
            return null;
        }
        SysAgentTask task = snapshot.task();
        SysAgentTaskStep latest = snapshot.latestStep();
        if (latest != null && AgentTaskStepStatus.PENDING.name().equals(latest.getStatus())) {
            return startPendingStep(task, latest, leaseOwner);
        }
        if (call == null || definition == null || !StringUtils.hasText(call.getToolName())) {
            throw new IllegalArgumentException("任务步骤缺少已注册工具调用");
        }

        String fingerprint = AgentTaskFingerprint.of(definition.getName(), call.getInput(), objectMapper);
        if (stepMapper.existsFingerprint(taskId, fingerprint) > 0) {
            throw new IllegalStateException("DUPLICATE_TASK_STEP");
        }

        LocalDateTime now = LocalDateTime.now();
        SysAgentTaskStep step = new SysAgentTaskStep();
        step.setTaskId(taskId);
        step.setStepOrder(value(task.getCurrentStepOrder()) + 1);
        step.setToolName(definition.getName());
        step.setInputJson(toJson(call.getInput(), properties.normalizedMaxResultJsonChars()));
        step.setInputFingerprint(fingerprint);
        step.setInputSummary(limit(call.getSummary(), 500));
        step.setPlannerFallback(fallback);
        step.setRiskLevel(definition.getRiskLevel());
        step.setRiskCategory(definition.getRiskCategory());
        step.setStatus(AgentTaskStepStatus.PENDING.name());
        step.setCreatedTime(now);
        step.setUpdatedTime(now);
        if (stepMapper.insert(step) != 1) {
            throw new IllegalStateException("无法持久化管理员智能体任务步骤");
        }
        appendEvent(taskId, AgentTaskEventType.STEP_STARTED, step.getStepOrder(),
                "WORKER", leaseOwner, null, AgentTaskStepStatus.PENDING.name(),
                Map.of("toolName", limit(definition.getName(), 160), "phase", "planned"));

        task.setCurrentStepOrder(step.getStepOrder());
        if (!transitionTask(task, AgentTaskStatus.RUNNING, AgentTaskStatus.RUNNING, leaseOwner)) {
            throw new IllegalStateException("任务状态已变化，步骤没有开始执行");
        }
        return startPendingStep(task, step, leaseOwner);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeStep(
            String taskId,
            String leaseOwner,
            int stepOrder,
            AgentTaskStepStatus expectedStepStatus,
            AgentChatResponse response,
            AgentTaskStatus nextTaskStatus
    ) {
        SysAgentTaskStep step = requireStep(taskId, stepOrder);
        LocalDateTime now = LocalDateTime.now();
        step.setExpectedStatus(expectedStepStatus.name());
        step.setStatus(AgentTaskStepStatus.COMPLETED.name());
        applyResponse(step, response, now);
        if (transitionStep(step) != 1) {
            return false;
        }
        recordStepOutcome(AgentTaskStepStatus.COMPLETED);

        SysAgentTask task = taskMapper.selectByTaskId(taskId);
        if (task == null || !AgentTaskStatus.RUNNING.name().equals(task.getStatus())) {
            return false;
        }
        task.setCompletedSteps(value(task.getCompletedSteps()) + 1);
        task.setPendingAuditId(null);
        if (nextTaskStatus == AgentTaskStatus.COMPLETED) {
            task.setTerminalCode("COMPLETED");
            task.setTerminalReason(limit(response == null ? null : response.getAnswer(), 1_000));
        }
        return transitionTask(task, AgentTaskStatus.RUNNING, nextTaskStatus, leaseOwner);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean pauseForConfirmation(
            String taskId,
            String leaseOwner,
            int stepOrder,
            AgentChatResponse response
    ) {
        SysAgentTaskStep step = requireStep(taskId, stepOrder);
        LocalDateTime now = LocalDateTime.now();
        step.setExpectedStatus(AgentTaskStepStatus.RUNNING.name());
        step.setStatus(AgentTaskStepStatus.WAITING_CONFIRMATION.name());
        step.setAuditId(response == null ? null : response.getAuditId());
        step.setConfirmationText(response != null && response.getConfirmation() != null
                ? limit(response.getConfirmation().getRequiredText(), 200)
                : null);
        step.setTraceId(response == null ? null : response.getTraceId());
        step.setResultSummary(limit(response == null ? null : response.getAnswer(), properties.normalizedMaxSummaryChars()));
        step.setResultJson(toResponseJson(response));
        step.setUpdatedTime(now);
        if (transitionStep(step) != 1) {
            return false;
        }

        SysAgentTask task = taskMapper.selectByTaskId(taskId);
        if (task == null || !AgentTaskStatus.RUNNING.name().equals(task.getStatus())) {
            cancelWaitingStep(step);
            return false;
        }
        task.setPendingAuditId(response == null ? null : response.getAuditId());
        boolean paused = transitionTask(task, AgentTaskStatus.RUNNING,
                AgentTaskStatus.WAITING_CONFIRMATION, leaseOwner);
        if (paused) {
            recordStepOutcome(AgentTaskStepStatus.WAITING_CONFIRMATION);
            recordMetric(metricsRecorder::recordConfirmationWait);
        }
        return paused;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean failStep(
            String taskId,
            String leaseOwner,
            int stepOrder,
            AgentTaskStepStatus expectedStepStatus,
            AgentChatResponse response,
            String terminalCode,
            String terminalReason
    ) {
        SysAgentTaskStep step = requireStep(taskId, stepOrder);
        LocalDateTime now = LocalDateTime.now();
        step.setExpectedStatus(expectedStepStatus.name());
        step.setStatus(AgentTaskStepStatus.FAILED.name());
        applyResponse(step, response, now);
        step.setErrorMessage(limit(response == null ? terminalReason : response.getErrorMessage(), 1_000));
        if (transitionStep(step) != 1) {
            return false;
        }
        recordStepOutcome(AgentTaskStepStatus.FAILED);
        return failTask(taskId, leaseOwner, terminalCode, terminalReason);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeTask(String taskId, String leaseOwner, String reason) {
        SysAgentTask task = taskMapper.selectByTaskId(taskId);
        if (!isRunningLease(task, leaseOwner)) {
            return false;
        }
        task.setTerminalCode("COMPLETED");
        task.setTerminalReason(limit(reason, 1_000));
        return transitionTask(task, AgentTaskStatus.RUNNING, AgentTaskStatus.COMPLETED, leaseOwner);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean failTask(String taskId, String leaseOwner, String code, String reason) {
        SysAgentTask task = taskMapper.selectByTaskId(taskId);
        if (task == null || !AgentTaskStatus.RUNNING.name().equals(task.getStatus())) {
            return false;
        }
        task.setTerminalCode(limit(code, 80));
        task.setTerminalReason(limit(reason, 1_000));
        return transitionTask(task, AgentTaskStatus.RUNNING, AgentTaskStatus.FAILED, leaseOwner);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTaskConfirmationClaim beginConfirmation(String taskId, Long operatorId) {
        SysAgentTask task = requireOwned(taskId, operatorId);
        if (!AgentTaskStatus.WAITING_CONFIRMATION.name().equals(task.getStatus())
                || !StringUtils.hasText(task.getPendingAuditId())) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "当前任务不在等待确认状态");
        }
        SysAgentTaskStep step = stepMapper.selectByAuditId(task.getPendingAuditId());
        if (step == null || !taskId.equals(step.getTaskId())
                || !AgentTaskStepStatus.WAITING_CONFIRMATION.name().equals(step.getStatus())) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "任务待确认步骤状态已变化");
        }

        String leaseOwner = "confirmation-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        task.setLeaseOwner(leaseOwner);
        task.setClaimedAt(now);
        task.setHeartbeatAt(now);
        if (!transitionTask(task, AgentTaskStatus.WAITING_CONFIRMATION, AgentTaskStatus.RUNNING, null)) {
            throw new BusinessException(ResultCode.CONFLICT, "任务确认状态已被其他请求处理");
        }

        step.setExpectedStatus(AgentTaskStepStatus.WAITING_CONFIRMATION.name());
        step.setStatus(AgentTaskStepStatus.CONFIRMING.name());
        step.setUpdatedTime(now);
        if (transitionStep(step) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "任务待确认步骤已被其他请求处理");
        }
        return new AgentTaskConfirmationClaim(task, step, leaseOwner);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean restoreConfirmationWait(AgentTaskConfirmationClaim claim) {
        if (claim == null) {
            return false;
        }
        SysAgentTaskStep step = requireStep(claim.task().getTaskId(), claim.step().getStepOrder());
        step.setExpectedStatus(AgentTaskStepStatus.CONFIRMING.name());
        step.setStatus(AgentTaskStepStatus.WAITING_CONFIRMATION.name());
        step.setUpdatedTime(LocalDateTime.now());
        if (transitionStep(step) != 1) {
            return false;
        }
        SysAgentTask task = taskMapper.selectByTaskId(claim.task().getTaskId());
        if (task == null) {
            return false;
        }
        task.setPendingAuditId(step.getAuditId());
        boolean restored = transitionTask(
                task,
                AgentTaskStatus.RUNNING,
                AgentTaskStatus.WAITING_CONFIRMATION,
                claim.leaseOwner()
        );
        if (!restored) {
            cancelWaitingStep(step);
        }
        return restored;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markRequiresReview(
            String taskId,
            String leaseOwner,
            Integer stepOrder,
            AgentTaskStepStatus expectedStepStatus,
            String code,
            String reason
    ) {
        if (stepOrder != null) {
            SysAgentTaskStep step = requireStep(taskId, stepOrder);
            step.setExpectedStatus(expectedStepStatus.name());
            step.setStatus(AgentTaskStepStatus.REQUIRES_REVIEW.name());
            step.setErrorMessage(limit(reason, 1_000));
            step.setCompletedAt(LocalDateTime.now());
            step.setUpdatedTime(LocalDateTime.now());
            if (transitionStep(step) != 1) {
                return false;
            }
            recordStepOutcome(AgentTaskStepStatus.REQUIRES_REVIEW);
        }
        SysAgentTask task = taskMapper.selectByTaskId(taskId);
        if (task == null || !AgentTaskStatus.RUNNING.name().equals(task.getStatus())) {
            return false;
        }
        task.setTerminalCode(limit(code, 80));
        task.setTerminalReason(limit(reason, 1_000));
        return transitionTask(task, AgentTaskStatus.RUNNING, AgentTaskStatus.REQUIRES_REVIEW, leaseOwner);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentTaskResponse cancel(String taskId, Long operatorId, AgentTaskCancelRequest request) {
        SysAgentTask existing = requireOwned(taskId, operatorId);
        String reason = request == null || !StringUtils.hasText(request.getReason())
                ? "管理员取消任务"
                : limit(request.getReason().trim(), 1_000);
        LocalDateTime now = LocalDateTime.now();
        List<SysAgentTaskStep> unstartedSteps = safeSteps(taskId).stream()
                .filter(step -> AgentTaskStepStatus.PENDING.name().equals(step.getStatus())
                        || AgentTaskStepStatus.WAITING_CONFIRMATION.name().equals(step.getStatus()))
                .toList();
        if (taskMapper.cancelOwned(taskId, operatorId, reason, now) != 1) {
            throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "当前任务状态不能取消");
        }
        stepMapper.cancelUnstarted(taskId);
        appendEvent(taskId, AgentTaskEventType.TASK_CANCELLED, null,
                "OPERATOR", String.valueOf(operatorId), existing.getStatus(),
                AgentTaskStatus.CANCELLED.name(), Map.of("reason", reason));
        for (SysAgentTaskStep step : unstartedSteps) {
            appendEvent(taskId, AgentTaskEventType.STEP_CANCELLED, step.getStepOrder(),
                    "OPERATOR", String.valueOf(operatorId), step.getStatus(),
                    AgentTaskStepStatus.CANCELLED.name(), Map.of());
        }
        cancelPendingAudit(existing.getPendingAuditId());
        SysAgentTask updated = taskMapper.selectByTaskId(taskId);
        if (updated == null) {
            existing.setStatus(AgentTaskStatus.CANCELLED.name());
            existing.setCancelledBy(operatorId);
            existing.setCancelReason(reason);
            existing.setCancelledAt(now);
            existing.setCompletedAt(now);
            updated = existing;
        }
        AgentTaskResponse response = responseMapper.toResponse(updated, safeSteps(taskId));
        recordMetric(metricsRecorder::recordCancellation);
        recordTaskOutcome(updated, AgentTaskStatus.CANCELLED, now);
        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public RecoveryOutcome recoverOne(String taskId, LocalDateTime staleBefore) {
        SysAgentTask task = taskMapper.selectByTaskId(taskId);
        if (!isStaleRunningTask(task, staleBefore)) {
            return RecoveryOutcome.IGNORED;
        }
        SysAgentTaskStep latest = stepMapper.selectLatest(taskId);
        if (latest == null || AgentTaskStepStatus.COMPLETED.name().equals(latest.getStatus())
                || AgentTaskStepStatus.PENDING.name().equals(latest.getStatus())) {
            return transitionRecoveredTask(task, AgentTaskStatus.QUEUED, staleBefore)
                    ? RecoveryOutcome.RECOVERED : RecoveryOutcome.IGNORED;
        }
        if (AgentTaskStepStatus.WAITING_CONFIRMATION.name().equals(latest.getStatus())) {
            task.setPendingAuditId(latest.getAuditId());
            return transitionRecoveredTask(task, AgentTaskStatus.WAITING_CONFIRMATION, staleBefore)
                    ? RecoveryOutcome.RECOVERED : RecoveryOutcome.IGNORED;
        }
        if (AgentTaskStepStatus.RUNNING.name().equals(latest.getStatus())) {
            if (isReadonly(latest)) {
                latest.setExpectedStatus(AgentTaskStepStatus.RUNNING.name());
                latest.setStatus(AgentTaskStepStatus.PENDING.name());
                latest.setStartedAt(null);
                latest.setUpdatedTime(LocalDateTime.now());
                if (transitionStep(latest) != 1) {
                    return RecoveryOutcome.IGNORED;
                }
                return transitionRecoveredTask(task, AgentTaskStatus.QUEUED, staleBefore)
                        ? RecoveryOutcome.RECOVERED : RecoveryOutcome.IGNORED;
            }
            return recoveryReview(task, latest, AgentTaskStepStatus.RUNNING,
                    "AMBIGUOUS_WRITE_RECOVERY", "非只读步骤中断，禁止自动重放。无明确终态，需要人工复核。",
                    staleBefore);
        }
        if (AgentTaskStepStatus.CONFIRMING.name().equals(latest.getStatus())) {
            return recoverConfirming(task, latest, staleBefore);
        }
        return recoveryReview(task, latest, AgentTaskStepStatus.from(latest.getStatus()),
                "INVALID_RECOVERY_STATE", "任务恢复时检测到不一致的步骤状态，需要人工复核。",
                staleBefore);
    }

    private RecoveryOutcome recoverConfirming(
            SysAgentTask task,
            SysAgentTaskStep step,
            LocalDateTime staleBefore
    ) {
        AgentAuditResponse audit = StringUtils.hasText(step.getAuditId())
                ? auditService.getByAuditId(step.getAuditId())
                : null;
        if (audit == null) {
            return recoveryReview(task, step, AgentTaskStepStatus.CONFIRMING,
                    "MISSING_CONFIRMATION_AUDIT", "确认中的写步骤缺少审计记录，需要人工复核。",
                    staleBefore);
        }
        return switch (normalize(audit.getStatus())) {
            case "PREVIEW" -> restoreRecoveredWaiting(task, step, staleBefore);
            case "EXECUTED" -> reconcileRecoveredAudit(task, step, audit, true, staleBefore);
            case "FAILED" -> reconcileRecoveredAudit(task, step, audit, false, staleBefore);
            case "CONFIRMED" -> recoveryReview(task, step, AgentTaskStepStatus.CONFIRMING,
                    "AMBIGUOUS_CONFIRMED_WRITE", "写步骤已经确认但没有持久化终态，禁止自动重放。",
                    staleBefore);
            default -> recoveryReview(task, step, AgentTaskStepStatus.CONFIRMING,
                    "INVALID_CONFIRMATION_AUDIT", "确认中的写步骤审计状态不明确，需要人工复核。",
                    staleBefore);
        };
    }

    private RecoveryOutcome restoreRecoveredWaiting(
            SysAgentTask task,
            SysAgentTaskStep step,
            LocalDateTime staleBefore
    ) {
        step.setExpectedStatus(AgentTaskStepStatus.CONFIRMING.name());
        step.setStatus(AgentTaskStepStatus.WAITING_CONFIRMATION.name());
        step.setUpdatedTime(LocalDateTime.now());
        if (transitionStep(step) != 1) {
            return RecoveryOutcome.IGNORED;
        }
        task.setPendingAuditId(step.getAuditId());
        return transitionRecoveredTask(task, AgentTaskStatus.WAITING_CONFIRMATION, staleBefore)
                ? RecoveryOutcome.RECOVERED : RecoveryOutcome.IGNORED;
    }

    private RecoveryOutcome reconcileRecoveredAudit(
            SysAgentTask task,
            SysAgentTaskStep step,
            AgentAuditResponse audit,
            boolean success,
            LocalDateTime staleBefore
    ) {
        LocalDateTime now = LocalDateTime.now();
        step.setExpectedStatus(AgentTaskStepStatus.CONFIRMING.name());
        step.setStatus(success ? AgentTaskStepStatus.COMPLETED.name() : AgentTaskStepStatus.FAILED.name());
        step.setResultSummary(limit(audit.getSummary(), properties.normalizedMaxSummaryChars()));
        step.setResultJson(limit(audit.getResultJson(), properties.normalizedMaxResultJsonChars()));
        step.setErrorMessage(limit(audit.getErrorMessage(), 1_000));
        step.setCompletedAt(now);
        step.setUpdatedTime(now);
        if (transitionStep(step) != 1) {
            return RecoveryOutcome.IGNORED;
        }
        if (success) {
            task.setCompletedSteps(value(task.getCompletedSteps()) + 1);
            task.setPendingAuditId(null);
            if (!transitionRecoveredTask(task, AgentTaskStatus.QUEUED, staleBefore)) {
                return RecoveryOutcome.IGNORED;
            }
            recordStepOutcome(AgentTaskStepStatus.COMPLETED);
            return RecoveryOutcome.RECOVERED;
        }
        task.setTerminalCode("AUDITED_STEP_FAILED");
        task.setTerminalReason(limit(audit.getErrorMessage(), 1_000));
        if (!transitionRecoveredTask(task, AgentTaskStatus.FAILED, staleBefore)) {
            return RecoveryOutcome.IGNORED;
        }
        recordStepOutcome(AgentTaskStepStatus.FAILED);
        return RecoveryOutcome.RECOVERED;
    }

    private RecoveryOutcome recoveryReview(
            SysAgentTask task,
            SysAgentTaskStep step,
            AgentTaskStepStatus expectedStepStatus,
            String code,
            String reason,
            LocalDateTime staleBefore
    ) {
        boolean stepRecovered = false;
        if (expectedStepStatus != null) {
            step.setExpectedStatus(expectedStepStatus.name());
            step.setStatus(AgentTaskStepStatus.REQUIRES_REVIEW.name());
            step.setErrorMessage(limit(reason, 1_000));
            step.setCompletedAt(LocalDateTime.now());
            step.setUpdatedTime(LocalDateTime.now());
            if (transitionStep(step) != 1) {
                return RecoveryOutcome.IGNORED;
            }
            stepRecovered = true;
        }
        task.setTerminalCode(code);
        task.setTerminalReason(reason);
        if (!transitionRecoveredTask(task, AgentTaskStatus.REQUIRES_REVIEW, staleBefore)) {
            return RecoveryOutcome.IGNORED;
        }
        if (stepRecovered) {
            recordStepOutcome(AgentTaskStepStatus.REQUIRES_REVIEW);
        }
        return RecoveryOutcome.REQUIRES_REVIEW;
    }

    private boolean transitionRecoveredTask(
            SysAgentTask task,
            AgentTaskStatus nextStatus,
            LocalDateTime staleBefore
    ) {
        String leaseOwner = task.getLeaseOwner();
        return transitionTask(
                task,
                AgentTaskStatus.RUNNING,
                nextStatus,
                leaseOwner,
                taskEventType(AgentTaskStatus.RUNNING, nextStatus),
                "SYSTEM",
                "STALE_RECOVERY",
                Map.of("staleBefore", staleBefore.toString()),
                staleBefore
        );
    }

    private AgentTaskStepExecution startPendingStep(SysAgentTask task, SysAgentTaskStep step, String leaseOwner) {
        LocalDateTime now = LocalDateTime.now();
        step.setExpectedStatus(AgentTaskStepStatus.PENDING.name());
        step.setStatus(AgentTaskStepStatus.RUNNING.name());
        step.setStartedAt(now);
        step.setUpdatedTime(now);
        if (transitionStep(step) != 1) {
            return null;
        }
        if (taskMapper.heartbeat(task.getTaskId(), leaseOwner, now) != 1) {
            throw new IllegalStateException("任务租约已失效，步骤没有开始执行");
        }
        return new AgentTaskStepExecution(step, readStoredCall(step));
    }

    private boolean transitionTask(
            SysAgentTask task,
            AgentTaskStatus expectedStatus,
            AgentTaskStatus nextStatus,
            String expectedLeaseOwner
    ) {
        String actorType = StringUtils.hasText(expectedLeaseOwner) ? "WORKER" : "SYSTEM";
        return transitionTask(task, expectedStatus, nextStatus, expectedLeaseOwner,
                taskEventType(expectedStatus, nextStatus), actorType, expectedLeaseOwner, Map.of());
    }

    private boolean transitionTask(
            SysAgentTask task,
            AgentTaskStatus expectedStatus,
            AgentTaskStatus nextStatus,
            String expectedLeaseOwner,
            AgentTaskEventType eventType,
            String actorType,
            String actorId,
            Map<String, Object> detail
    ) {
        return transitionTask(task, expectedStatus, nextStatus, expectedLeaseOwner,
                eventType, actorType, actorId, detail, null);
    }

    private boolean transitionTask(
            SysAgentTask task,
            AgentTaskStatus expectedStatus,
            AgentTaskStatus nextStatus,
            String expectedLeaseOwner,
            AgentTaskEventType eventType,
            String actorType,
            String actorId,
            Map<String, Object> detail,
            LocalDateTime staleBefore
    ) {
        LocalDateTime now = LocalDateTime.now();
        task.setExpectedStatus(expectedStatus.name());
        task.setExpectedLeaseOwner(expectedLeaseOwner);
        task.setStatus(nextStatus.name());
        task.setUpdatedTime(now);
        if (nextStatus == AgentTaskStatus.RUNNING) {
            task.setHeartbeatAt(now);
        } else {
            task.setLeaseOwner(null);
            task.setClaimedAt(null);
            task.setHeartbeatAt(null);
        }
        if (nextStatus.terminal()) {
            task.setCompletedAt(now);
        }
        boolean updated = (staleBefore == null
                ? taskMapper.transition(task)
                : taskMapper.transitionStale(task, staleBefore)) == 1;
        if (!updated) {
            markRollbackOnly();
            return false;
        }
        if (eventType != null && expectedStatus != nextStatus) {
            appendEvent(task.getTaskId(), eventType, null,
                    actorType, actorId, expectedStatus.name(), nextStatus.name(), detail);
        }
        if (nextStatus.terminal()) {
            recordTaskOutcome(task, nextStatus, now);
            if (nextStatus == AgentTaskStatus.REQUIRES_REVIEW) {
                recordMetric(metricsRecorder::recordReviewRequired);
            }
        }
        return true;
    }

    private int transitionStep(SysAgentTaskStep step) {
        int updated = stepMapper.transition(step);
        if (updated != 1) {
            markRollbackOnly();
            return updated;
        }
        AgentTaskEventType eventType = stepEventType(step.getStatus());
        if (eventType != null) {
            appendEvent(step.getTaskId(), eventType, step.getStepOrder(),
                    "WORKER", null, step.getExpectedStatus(), step.getStatus(), stepEventDetail(step));
        }
        return updated;
    }

    private AgentTaskEventType taskEventType(AgentTaskStatus expected, AgentTaskStatus next) {
        if (expected == next) {
            return null;
        }
        return switch (next) {
            case RUNNING -> expected == AgentTaskStatus.QUEUED
                    ? AgentTaskEventType.TASK_CLAIMED : AgentTaskEventType.TASK_STATE_CHANGED;
            case QUEUED -> AgentTaskEventType.TASK_QUEUED;
            case WAITING_CONFIRMATION -> AgentTaskEventType.TASK_WAITING_CONFIRMATION;
            case WAITING_INPUT -> AgentTaskEventType.TASK_WAITING_INPUT;
            case PAUSED -> AgentTaskEventType.TASK_PAUSED;
            case COMPLETED -> AgentTaskEventType.TASK_COMPLETED;
            case CANCELLED -> AgentTaskEventType.TASK_CANCELLED;
            case FAILED -> AgentTaskEventType.TASK_FAILED;
            case REQUIRES_REVIEW -> AgentTaskEventType.TASK_REQUIRES_REVIEW;
        };
    }

    private AgentTaskEventType stepEventType(String status) {
        AgentTaskStepStatus stepStatus = AgentTaskStepStatus.from(status);
        if (stepStatus == null) {
            return AgentTaskEventType.STEP_STATE_CHANGED;
        }
        return switch (stepStatus) {
            case PENDING, RUNNING -> AgentTaskEventType.STEP_STARTED;
            case WAITING_CONFIRMATION -> AgentTaskEventType.STEP_WAITING_CONFIRMATION;
            case COMPLETED -> AgentTaskEventType.STEP_COMPLETED;
            case FAILED -> AgentTaskEventType.STEP_FAILED;
            case CANCELLED -> AgentTaskEventType.STEP_CANCELLED;
            case REQUIRES_REVIEW -> AgentTaskEventType.STEP_REQUIRES_REVIEW;
            case CONFIRMING -> AgentTaskEventType.STEP_STATE_CHANGED;
        };
    }

    private Map<String, Object> stepEventDetail(SysAgentTaskStep step) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("toolName", limit(step.getToolName(), 160));
        if (StringUtils.hasText(step.getResultSummary())) {
            detail.put("resultSummary", limit(step.getResultSummary(), 300));
        }
        if (StringUtils.hasText(step.getErrorMessage())) {
            detail.put("error", limit(step.getErrorMessage(), 300));
        }
        return detail;
    }

    private void appendEvent(
            String taskId,
            AgentTaskEventType eventType,
            Integer stepOrder,
            String actorType,
            String actorId,
            String fromStatus,
            String toStatus,
            Map<String, Object> detail
    ) {
        if (eventMapper == null || eventType == null) {
            return;
        }
        SysAgentTaskEvent event = new SysAgentTaskEvent();
        event.setTaskId(limit(taskId, 80));
        event.setEventType(eventType.name());
        event.setStepOrder(stepOrder);
        event.setActorType(limit(actorType, 32));
        event.setActorId(limit(actorId, 120));
        event.setFromStatus(limit(fromStatus, 32));
        event.setToStatus(limit(toStatus, 32));
        event.setDetailJson(toJson(detail == null ? Map.of() : detail, 4_000));
        event.setCreatedTime(LocalDateTime.now());
        if (eventMapper.insert(event) != 1) {
            throw new IllegalStateException("无法持久化管理员智能体任务事件");
        }
    }

    private AgentTaskResponse responseForOwned(String taskId, Long operatorId) {
        SysAgentTask updated = taskMapper.selectByTaskId(taskId);
        if (updated == null) {
            updated = requireOwned(taskId, operatorId);
        }
        return responseMapper.toResponse(updated, safeSteps(taskId));
    }

    private AgentTaskEventResponse toEventResponse(SysAgentTaskEvent event) {
        AgentTaskEventResponse response = new AgentTaskEventResponse();
        response.setCursor(event.getId());
        response.setTaskId(event.getTaskId());
        response.setEventType(event.getEventType());
        response.setStepOrder(event.getStepOrder());
        response.setActorType(event.getActorType());
        response.setActorId(event.getActorId());
        response.setFromStatus(event.getFromStatus());
        response.setToStatus(event.getToStatus());
        response.setCreatedTime(event.getCreatedTime());
        if (StringUtils.hasText(event.getDetailJson())) {
            try {
                Map<String, Object> detail = objectMapper.readValue(event.getDetailJson(), MAP_TYPE);
                response.setDetail(detail == null ? Map.of() : detail);
            } catch (JsonProcessingException | IllegalArgumentException exception) {
                response.setDetail(Map.of("summary", "事件详情无法读取"));
            }
        }
        return response;
    }

    private void applyResponse(SysAgentTaskStep step, AgentChatResponse response, LocalDateTime now) {
        step.setAuditId(response == null ? step.getAuditId() : response.getAuditId());
        step.setTraceId(response == null ? null : response.getTraceId());
        step.setResultSummary(limit(response == null ? null : response.getAnswer(), properties.normalizedMaxSummaryChars()));
        step.setResultJson(toResponseJson(response));
        step.setErrorMessage(limit(response == null ? null : response.getErrorMessage(), 1_000));
        step.setCompletedAt(now);
        step.setUpdatedTime(now);
    }

    private String toResponseJson(AgentChatResponse response) {
        if (response == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.getStatus());
        result.put("answer", response.getAnswer());
        result.put("artifacts", response.getArtifacts());
        result.put("nextActions", response.getNextActions());
        result.put("errorCode", response.getErrorCode());
        result.put("errorMessage", response.getErrorMessage());
        return toJson(result, properties.normalizedMaxResultJsonChars());
    }

    private String toJson(Object value, int maxChars) {
        try {
            return limit(objectMapper.writeValueAsString(value == null ? Map.of() : value), maxChars);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("任务步骤 JSON 序列化失败", e);
        }
    }

    private SysAgentTask requireOwned(String taskId, Long operatorId) {
        SysAgentTask task = taskMapper.selectOwned(taskId, operatorId);
        if (task == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "任务不存在");
        }
        return task;
    }

    private SysAgentTaskStep requireStep(String taskId, int stepOrder) {
        SysAgentTaskStep step = stepMapper.selectByTaskAndOrder(taskId, stepOrder);
        if (step == null) {
            throw new IllegalStateException("任务步骤不存在: " + taskId + "/" + stepOrder);
        }
        return step;
    }

    private List<SysAgentTaskStep> safeSteps(String taskId) {
        List<SysAgentTaskStep> steps = stepMapper.selectByTaskId(taskId);
        return steps == null ? List.of() : steps;
    }

    private void cancelPendingAudit(String auditId) {
        if (!StringUtils.hasText(auditId)) {
            return;
        }
        try {
            AgentAuditResponse audit = auditService.getByAuditId(auditId);
            if (audit != null && "PREVIEW".equals(audit.getStatus())) {
                auditService.cancel(auditId, "管理员取消持久化智能体任务");
            }
        } catch (RuntimeException e) {
            log.warn("取消任务待确认审计失败，auditId={}, reason={}", auditId, e.getClass().getSimpleName());
        }
    }

    private void cancelWaitingStep(SysAgentTaskStep step) {
        step.setExpectedStatus(AgentTaskStepStatus.WAITING_CONFIRMATION.name());
        step.setStatus(AgentTaskStepStatus.CANCELLED.name());
        step.setCompletedAt(LocalDateTime.now());
        step.setUpdatedTime(LocalDateTime.now());
        if (transitionStep(step) == 1) {
            recordStepOutcome(AgentTaskStepStatus.CANCELLED);
        }
    }

    private void recordStepOutcome(AgentTaskStepStatus status) {
        recordMetric(() -> metricsRecorder.recordStepOutcome(status));
    }

    private void recordTaskOutcome(SysAgentTask task, AgentTaskStatus status, LocalDateTime finishedAt) {
        LocalDateTime startedAt = task == null ? null : task.getCreatedTime();
        long durationNanos = startedAt == null || finishedAt == null
                ? 0L
                : Math.max(0L, Duration.between(startedAt, finishedAt).toNanos());
        recordMetric(() -> metricsRecorder.recordTaskOutcome(status, durationNanos));
    }

    private void recordMetric(Runnable recorder) {
        try {
            recorder.run();
        } catch (RuntimeException exception) {
            log.warn("管理员智能体任务指标记录失败: reason={}", exception.getClass().getSimpleName());
        }
    }

    private boolean isRunningLease(SysAgentTask task, String leaseOwner) {
        return task != null
                && AgentTaskStatus.RUNNING.name().equals(task.getStatus())
                && StringUtils.hasText(leaseOwner)
                && leaseOwner.equals(task.getLeaseOwner());
    }

    private boolean isStaleRunningTask(SysAgentTask task, LocalDateTime staleBefore) {
        if (task == null || staleBefore == null
                || !AgentTaskStatus.RUNNING.name().equals(task.getStatus())) {
            return false;
        }
        LocalDateTime heartbeat = task.getHeartbeatAt() != null
                ? task.getHeartbeatAt()
                : (task.getClaimedAt() != null ? task.getClaimedAt() : task.getUpdatedTime());
        return heartbeat != null && !heartbeat.isAfter(staleBefore);
    }

    private boolean isReadonly(SysAgentTaskStep step) {
        return "readonly".equalsIgnoreCase(normalize(step.getRiskLevel()))
                && "READONLY".equalsIgnoreCase(normalize(step.getRiskCategory()));
    }

    private int value(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private Map<String, Object> mergeWorkflowContext(String existingJson, Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_ERROR, "补充输入不能为空");
        }
        Map<String, Object> merged = new LinkedHashMap<>();
        if (StringUtils.hasText(existingJson)) {
            try {
                JsonNode existing = objectMapper.readTree(existingJson);
                validateWorkflowNode(existing, 0, "context");
                if (existing != null && existing.isObject()) {
                    merged.putAll(objectMapper.convertValue(existing, MAP_TYPE));
                }
            } catch (JsonProcessingException | IllegalArgumentException exception) {
                throw new BusinessException(ResultCode.OPERATION_NOT_ALLOWED, "任务已有上下文无法安全读取");
            }
        }
        try {
            JsonNode inputNode = objectMapper.valueToTree(input);
            validateWorkflowNode(inputNode, 0, "input");
            merged.putAll(objectMapper.convertValue(inputNode, MAP_TYPE));
            JsonNode mergedNode = objectMapper.valueToTree(merged);
            validateWorkflowNode(mergedNode, 0, "context");
            String serialized = objectMapper.writeValueAsString(merged);
            if (serialized.length() > properties.normalizedMaxWorkflowContextJsonChars()) {
                throw new BusinessException(ResultCode.PARAM_VALIDATE_ERROR, "补充输入序列化后超过大小限制");
            }
            return merged;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.PARAM_VALIDATE_ERROR, "补充输入必须是有限 JSON 对象");
        }
    }

    private void validateWorkflowNode(JsonNode node, int depth, String path) {
        if (node == null || node.isNull()) {
            return;
        }
        if (depth > properties.normalizedMaxWorkflowContextDepth()) {
            throw new IllegalArgumentException("workflow context depth exceeded");
        }
        if (node.isObject()) {
            if (node.size() > properties.normalizedMaxWorkflowContextKeys()) {
                throw new IllegalArgumentException("workflow context key count exceeded");
            }
            java.util.Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (!field.getKey().matches("[A-Za-z][A-Za-z0-9_.-]{0,63}")) {
                    throw new IllegalArgumentException("workflow context key is invalid");
                }
                validateWorkflowNode(field.getValue(), depth + 1, path + "." + field.getKey());
            }
            return;
        }
        if (node.isArray()) {
            if (node.size() > properties.normalizedMaxWorkflowContextCollectionItems()) {
                throw new IllegalArgumentException("workflow context collection size exceeded");
            }
            for (JsonNode item : node) {
                validateWorkflowNode(item, depth + 1, path + "[]");
            }
            return;
        }
        if (node.isTextual() && node.textValue().length() > properties.normalizedMaxWorkflowContextStringChars()) {
            throw new IllegalArgumentException("workflow context string size exceeded");
        }
        if (!node.isValueNode()) {
            throw new IllegalArgumentException("workflow context node is unsupported");
        }
    }

    private void markRollbackOnly() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (NoTransactionException | IllegalStateException ignored) {
            // Unit-level callers may invoke the state service without a transaction proxy.
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    protected enum RecoveryOutcome {
        RECOVERED,
        REQUIRES_REVIEW,
        IGNORED
    }
}
