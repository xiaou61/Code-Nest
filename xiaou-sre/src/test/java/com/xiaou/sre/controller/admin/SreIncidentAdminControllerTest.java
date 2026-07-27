package com.xiaou.sre.controller.admin;

import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.sre.dto.response.SreInvestigationContext;
import com.xiaou.sre.dto.response.SreIncidentSummary;
import com.xiaou.sre.service.SreIncidentEvidenceService;
import com.xiaou.sre.service.SreIncidentService;
import com.xiaou.sre.service.SreInvestigationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SreIncidentAdminControllerTest {

    @Test
    void summaryEndpointIsAdminOnlyGetContract() throws Exception {
        Method method = SreIncidentAdminController.class.getMethod("summary");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        RequireAdmin requireAdmin = method.getAnnotation(RequireAdmin.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/summary");
        assertThat(requireAdmin).isNotNull();
        assertThat(requireAdmin.message()).isEqualTo("查询 SRE 事故汇总需要管理员权限");
    }

    @Test
    void summaryReturnsServiceAggregation() {
        SreIncidentService incidentService = mock(SreIncidentService.class);
        SreIncidentEvidenceService evidenceService = mock(SreIncidentEvidenceService.class);
        SreInvestigationFacade investigationFacade = mock(SreInvestigationFacade.class);
        SreIncidentAdminController controller = new SreIncidentAdminController(
                incidentService, evidenceService, investigationFacade);
        SreIncidentSummary summary = new SreIncidentSummary();
        summary.setActiveCount(3L);
        when(incidentService.summary()).thenReturn(summary);

        Result<SreIncidentSummary> result = controller.summary();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(summary);
        verify(incidentService).summary();
    }

    @Test
    void investigationContextEndpointIsAdminOnlyGetContract() throws Exception {
        Method method = SreIncidentAdminController.class.getMethod("investigationContext", Long.class);

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        RequireAdmin requireAdmin = method.getAnnotation(RequireAdmin.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/{id}/investigation-context");
        assertThat(requireAdmin).isNotNull();
        assertThat(requireAdmin.message()).isEqualTo("查询 SRE 事故调查上下文需要管理员权限");
    }

    @Test
    void investigationContextReturnsBoundedFacadeResult() {
        SreIncidentService incidentService = mock(SreIncidentService.class);
        SreIncidentEvidenceService evidenceService = mock(SreIncidentEvidenceService.class);
        SreInvestigationFacade investigationFacade = mock(SreInvestigationFacade.class);
        SreIncidentAdminController controller = new SreIncidentAdminController(
                incidentService, evidenceService, investigationFacade);
        SreInvestigationContext context = context();
        when(investigationFacade.findByIncidentId(11L)).thenReturn(Optional.of(context));

        Result<SreInvestigationContext> result = controller.investigationContext(11L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(context);
        verify(investigationFacade).findByIncidentId(11L);
    }

    @Test
    void investigationContextReturnsStableNotFoundError() {
        SreIncidentService incidentService = mock(SreIncidentService.class);
        SreIncidentEvidenceService evidenceService = mock(SreIncidentEvidenceService.class);
        SreInvestigationFacade investigationFacade = mock(SreInvestigationFacade.class);
        SreIncidentAdminController controller = new SreIncidentAdminController(
                incidentService, evidenceService, investigationFacade);
        when(investigationFacade.findByIncidentId(404L)).thenReturn(Optional.empty());

        Result<SreInvestigationContext> result = controller.investigationContext(404L);

        assertThat(result.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIST.getCode());
        assertThat(result.getMessage()).isEqualTo("事故不存在");
        assertThat(result.getData()).isNull();
    }

    @Test
    void responseContractHasNoRawPayloadField() {
        SreInvestigationContext context = context();

        assertThat(context.incident().getClass().getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("rawPayload");
        assertThat(context.alerts().get(0).getClass().getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("rawPayload", "labelsJson", "annotationsJson");
        assertThat(context.evidence().get(0).snapshot()).doesNotContainKey("rawPayload");
    }

    private SreInvestigationContext context() {
        return new SreInvestigationContext(
                new SreInvestigationContext.Incident(
                        11L, "SRE-001", "code-nest", "TargetDown", "critical", "OPEN",
                        "target down", null, null, null),
                List.of(new SreInvestigationContext.Alert(
                        21L, "alertmanager", "TargetDown", "FIRING", "critical", "code-nest",
                        "2026-07-23T01:00:00Z", null, null)),
                List.of(new SreInvestigationContext.Evidence(
                        31L, "PROMETHEUS_SNAPSHOT", "target_up", "up{job=\"code-nest\"}",
                        null, "AVAILABLE", Map.of("status", "success"), false)),
                false,
                false,
                LocalDateTime.of(2026, 7, 23, 1, 10)
        );
    }
}
