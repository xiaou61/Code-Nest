package com.xiaou.web.growthcoach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.plan.dto.GrowthAutopilotDashboardResponse;
import com.xiaou.plan.dto.GrowthPlanAdjustmentApplyResult;
import com.xiaou.plan.dto.GrowthPlanAdjustmentPreview;
import com.xiaou.plan.service.GrowthAutopilotService;
import com.xiaou.plan.service.GrowthPlanAdjustmentService;
import com.xiaou.web.growthcoach.domain.GrowthCoachActionRun;
import com.xiaou.web.growthcoach.dto.GrowthCoachActionRunResponse;
import com.xiaou.web.growthcoach.dto.GrowthCoachConfirmRequest;
import com.xiaou.web.growthcoach.dto.GrowthEvidenceReference;
import com.xiaou.web.growthcoach.dto.GrowthCoachPreviewRequest;
import com.xiaou.web.growthcoach.intent.GrowthCoachIntent;
import com.xiaou.web.growthcoach.intent.GrowthCoachIntentResolver;
import com.xiaou.web.growthcoach.mapper.GrowthCoachActionEventMapper;
import com.xiaou.web.growthcoach.mapper.GrowthCoachActionRunMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthCoachApplicationServiceTest {

    @Mock
    private GrowthCoachActionRunMapper actionRunMapper;
    @Mock
    private GrowthCoachActionEventMapper actionEventMapper;
    @Mock
    private GrowthCoachIntentResolver intentResolver;
    @Mock
    private GrowthPlanAdjustmentService planAdjustmentService;
    @Mock
    private GrowthAutopilotService growthAutopilotService;
    @Mock
    private GrowthEvidenceQueryService growthEvidenceQueryService;
    @Mock
    private GrowthSkillInsightService growthSkillInsightService;
    @Mock
    private GrowthCoachFeatureGuard growthCoachFeatureGuard;
    @Mock
    private GrowthCoachPlanningLock planningLock;
    @Mock
    private GrowthCoachPlanningLock.LockLease planningLease;
    @Mock
    private GrowthCoachPreviewPersistenceService previewPersistenceService;
    @Mock
    private GrowthCoachMetricsRecorder growthCoachMetricsRecorder;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private GrowthCoachApplicationService service;

    @BeforeEach
    void setUp() {
        service = new GrowthCoachApplicationService(
                actionRunMapper,
                actionEventMapper,
                intentResolver,
                planAdjustmentService,
                growthAutopilotService,
                growthEvidenceQueryService,
                growthSkillInsightService,
                growthCoachFeatureGuard,
                planningLock,
                previewPersistenceService,
                growthCoachMetricsRecorder,
                objectMapper
        );
    }

    @Test
    void reusesExistingPreviewForTheSameClientRequestWithoutCallingPlannerAgain() throws Exception {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String message = "这周只剩 3 小时";
        GrowthCoachActionRun existing = previewRun("growth-run-existing", message, weekStart);
        when(actionRunMapper.selectByUserAndClientRequest(7L, "request-12345678")).thenReturn(existing);

        GrowthCoachPreviewRequest request = new GrowthCoachPreviewRequest();
        request.setMessage(message);
        request.setWeekStart(weekStart);
        request.setClientRequestId("request-12345678");

        GrowthCoachActionRunResponse response = service.preview(7L, request);

        assertThat(response.getRunId()).isEqualTo("growth-run-existing");
        assertThat(response.getStatus()).isEqualTo("PREVIEW");
        verify(intentResolver, never()).resolve(anyString());
        verify(planAdjustmentService, never()).preview(anyLong(), any());
    }

    @Test
    void marksRunConflictedWhenThePlanVersionChangedBeforeConfirm() throws Exception {
        GrowthCoachActionRun run = previewRun("growth-run-conflict", "这周只剩 3 小时", currentWeek());
        run.setPreviewHash("preview-hash");
        when(actionRunMapper.selectByRunIdAndUserForUpdate("growth-run-conflict", 7L)).thenReturn(run);
        when(planAdjustmentService.apply(anyLong(), any(), anyString())).thenReturn(versionConflict());
        when(actionRunMapper.updateStatus(anyString(), anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1);
        when(actionEventMapper.selectNextSequence("growth-run-conflict")).thenReturn(1);

        GrowthCoachConfirmRequest request = new GrowthCoachConfirmRequest();
        request.setPreviewHash("preview-hash");

        GrowthCoachActionRunResponse response = service.confirm(7L, "growth-run-conflict", request);

        assertThat(response.getStatus()).isEqualTo("CONFLICTED");
        assertThat(response.getErrorCode()).isEqualTo("PLAN_VERSION_CONFLICT");
        verify(actionRunMapper, never()).markExecuted(anyString(), anyLong(), any(), anyString());
    }

    @Test
    void doesNotResolveIntentWhenPreviewIsDisabled() {
        LocalDate weekStart = currentWeek();
        GrowthCoachPreviewRequest request = new GrowthCoachPreviewRequest();
        request.setMessage("这周只剩 3 小时");
        request.setWeekStart(weekStart);
        request.setClientRequestId("request-disabled-1234");
        when(actionRunMapper.selectByUserAndClientRequest(7L, "request-disabled-1234")).thenReturn(null);
        org.mockito.Mockito.doThrow(new BusinessException("AI 成长教练预览暂未开放"))
                .when(growthCoachFeatureGuard).checkPreview(7L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.preview(7L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预览暂未开放");

        verify(intentResolver, never()).resolve(anyString());
    }

    @Test
    void doesNotResolveIntentWhenAnotherPlanningRequestForTheUserIsRunning() {
        GrowthCoachPreviewRequest request = new GrowthCoachPreviewRequest();
        request.setMessage("这周只剩 3 小时");
        request.setWeekStart(currentWeek());
        request.setClientRequestId("request-busy-123456");
        when(actionRunMapper.selectByUserAndClientRequest(7L, "request-busy-123456")).thenReturn(null);
        org.mockito.Mockito.doThrow(new BusinessException("正在生成本周计划预览，请稍后再试"))
                .when(planningLock).acquire(7L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.preview(7L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正在生成");

        verify(intentResolver, never()).resolve(anyString());
        verify(planAdjustmentService, never()).preview(anyLong(), any());
    }

    @Test
    void persistsTheGeneratedPreviewWhileHoldingTheUserPlanningLease() {
        LocalDate weekStart = currentWeek();
        GrowthCoachPreviewRequest request = new GrowthCoachPreviewRequest();
        request.setMessage("这周只剩 3 小时，下周要面试 Java 后端岗位");
        request.setWeekStart(weekStart);
        request.setClientRequestId("request-create-123456");
        GrowthCoachIntent intent = new GrowthCoachIntent();
        intent.setAvailableMinutes(180);
        intent.setTargetRole("Java 后端");
        intent.setPrioritizeInterview(true);
        GrowthPlanAdjustmentPreview preview = new GrowthPlanAdjustmentPreview();
        preview.setGoalId(100L);
        preview.setWeekStart(weekStart);
        preview.setBasePlanVersion(4);
        preview.setBudgetMinutes(180);
        preview.setPlannedMinutes(180);
        preview.setTargetRole("Java 后端");

        when(actionRunMapper.selectByUserAndClientRequest(7L, "request-create-123456")).thenReturn(null);
        when(planningLock.acquire(7L)).thenReturn(planningLease);
        when(intentResolver.resolve(request.getMessage())).thenReturn(intent);
        when(planAdjustmentService.preview(anyLong(), any())).thenReturn(preview);

        GrowthCoachActionRunResponse response = service.preview(7L, request);

        assertThat(response.getStatus()).isEqualTo("PREVIEW");
        verify(previewPersistenceService).persistPreview(any(GrowthCoachActionRun.class));
        verify(planningLease).close();
        verify(growthCoachMetricsRecorder).recordAction(
                eq("growth.plan.adjust"), eq("preview"), eq("preview_created"), anyLong()
        );
    }

    @Test
    void doesNotApplyPlanWhenConfirmIsDisabled() throws Exception {
        GrowthCoachActionRun run = previewRun("growth-run-disabled", "这周只剩 3 小时", currentWeek());
        run.setPreviewHash("preview-hash");
        when(actionRunMapper.selectByRunIdAndUserForUpdate("growth-run-disabled", 7L)).thenReturn(run);
        GrowthCoachConfirmRequest request = new GrowthCoachConfirmRequest();
        request.setPreviewHash("preview-hash");
        org.mockito.Mockito.doThrow(new BusinessException("AI 成长教练确认执行暂未开放"))
                .when(growthCoachFeatureGuard).checkConfirm(7L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.confirm(7L, "growth-run-disabled", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("确认执行暂未开放");

        verify(planAdjustmentService, never()).apply(anyLong(), any(), anyString());
    }

    @Test
    void selectsTheCurrentPlanActionWithItsStoredReason() {
        GrowthAutopilotDashboardResponse.TaskItem task = new GrowthAutopilotDashboardResponse.TaskItem();
        task.setTaskId(88L);
        task.setTitle("完成 Java 并发专题练习");
        task.setStatus("todo");
        task.setPriority("P1");
        task.setPlannedMinutes(90);
        task.setSelectionReason("面试临近，优先保留该任务");
        task.setRoutePath("/interview");
        GrowthAutopilotDashboardResponse.DayTaskBucket bucket = new GrowthAutopilotDashboardResponse.DayTaskBucket();
        bucket.setToday(true);
        bucket.setTasks(List.of(task));
        GrowthAutopilotDashboardResponse dashboard = new GrowthAutopilotDashboardResponse();
        dashboard.setDayBuckets(List.of(bucket));
        when(growthAutopilotService.getDashboard(7L, null)).thenReturn(dashboard);
        GrowthEvidenceReference evidenceReference = new GrowthEvidenceReference();
        evidenceReference.setEvidenceId("evidence-interview-1");
        evidenceReference.setEvidenceType("INTERVIEW_SCORE");
        when(growthEvidenceQueryService.getRecentReferences(7L, 2)).thenReturn(List.of(evidenceReference));

        var action = service.getTodayAction(7L);

        assertThat(action.getTaskId()).isEqualTo(88L);
        assertThat(action.getReason()).isEqualTo("面试临近，优先保留该任务");
        assertThat(action.getExpectedChange()).contains("重新计算下一步行动");
        assertThat(action.getStartRoute()).isEqualTo("/interview");
        assertThat(action.getEvidenceRefs()).extracting(GrowthEvidenceReference::getEvidenceId)
                .containsExactly("evidence-interview-1");
    }

    @Test
    void changesTodayActionVersionWhenItsEvidenceChanges() {
        GrowthAutopilotDashboardResponse.TaskItem task = new GrowthAutopilotDashboardResponse.TaskItem();
        task.setTaskId(88L);
        task.setStatus("todo");
        task.setTaskDate(currentWeek().toString());
        task.setPriority("P1");
        task.setRoutePath("/interview");
        task.setSelectionReason("面试临近，优先保留该任务");
        GrowthAutopilotDashboardResponse.DayTaskBucket bucket = new GrowthAutopilotDashboardResponse.DayTaskBucket();
        bucket.setToday(true);
        bucket.setTasks(List.of(task));
        GrowthAutopilotDashboardResponse dashboard = new GrowthAutopilotDashboardResponse();
        dashboard.setPlanVersion(4);
        dashboard.setDayBuckets(List.of(bucket));
        when(growthAutopilotService.getDashboard(7L, null)).thenReturn(dashboard);

        GrowthEvidenceReference firstEvidence = new GrowthEvidenceReference();
        firstEvidence.setEvidenceId("evidence-interview-1");
        GrowthEvidenceReference secondEvidence = new GrowthEvidenceReference();
        secondEvidence.setEvidenceId("evidence-interview-2");
        when(growthEvidenceQueryService.getRecentReferences(7L, 2))
                .thenReturn(List.of(firstEvidence), List.of(secondEvidence));

        int firstVersion = service.getTodayAction(7L).getSelectionVersion();
        int secondVersion = service.getTodayAction(7L).getSelectionVersion();

        assertThat(secondVersion).isNotEqualTo(firstVersion);
    }

    private GrowthCoachActionRun previewRun(String runId, String message, LocalDate weekStart) throws Exception {
        GrowthCoachIntent intent = new GrowthCoachIntent();
        intent.setAvailableMinutes(180);
        intent.setTargetRole("Java 后端");
        GrowthPlanAdjustmentPreview preview = new GrowthPlanAdjustmentPreview();
        preview.setGoalId(100L);
        preview.setWeekStart(weekStart);
        preview.setBasePlanVersion(4);
        preview.setBudgetMinutes(180);
        preview.setPlannedMinutes(180);

        GrowthCoachActionRun run = new GrowthCoachActionRun();
        run.setRunId(runId);
        run.setUserId(7L);
        run.setClientRequestId("request-12345678");
        run.setStatus("PREVIEW");
        run.setBasePlanVersion(4);
        run.setRequestHash(sha256(message + "|" + weekStart));
        run.setIntentJson(objectMapper.writeValueAsString(intent));
        run.setPreviewJson(objectMapper.writeValueAsString(preview));
        run.setPreviewHash("preview-hash");
        run.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        return run;
    }

    private GrowthPlanAdjustmentApplyResult versionConflict() {
        GrowthPlanAdjustmentApplyResult result = new GrowthPlanAdjustmentApplyResult();
        result.setVersionConflict(true);
        result.setMessage("计划在预览后已变化，请重新生成预览");
        return result;
    }

    private LocalDate currentWeek() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
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
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
