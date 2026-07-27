package com.xiaou.system.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.system.dto.SreRcaReport;
import com.xiaou.system.dto.SreRcaRunDetail;
import com.xiaou.system.dto.SreRcaRunSummary;
import com.xiaou.system.service.SreIncidentRcaService;
import com.xiaou.system.service.SreRcaTriggerSource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SreRcaAdminControllerTest {

    @Test
    void endpointIsAdminOnlyAndDoesNotLogReportPayload() throws Exception {
        Method method = SreRcaAdminController.class.getMethod("investigate", Long.class);

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        RequireAdmin requireAdmin = method.getAnnotation(RequireAdmin.class);
        Log log = method.getAnnotation(Log.class);

        assertThat(postMapping.value()).containsExactly("/{id}/rca");
        assertThat(requireAdmin).isNotNull();
        assertThat(log).isNotNull();
        assertThat(log.saveRequestData()).isFalse();
        assertThat(log.saveResponseData()).isFalse();
    }

    @Test
    void investigateReturnsReportOrStableNotFoundError() {
        SreIncidentRcaService service = mock(SreIncidentRcaService.class);
        SreRcaAdminController controller = new SreRcaAdminController(service);
        SreRcaReport report = report();
        when(service.investigate(11L, SreRcaTriggerSource.ADMIN_API, 7L)).thenReturn(Optional.of(report));
        when(service.investigate(404L, SreRcaTriggerSource.ADMIN_API, 7L)).thenReturn(Optional.empty());

        try (MockedStatic<StpAdminUtil> stpAdminUtil = mockStatic(StpAdminUtil.class)) {
            stpAdminUtil.when(StpAdminUtil::getLoginIdAsLong).thenReturn(7L);

            Result<SreRcaReport> success = controller.investigate(11L);
            Result<SreRcaReport> missing = controller.investigate(404L);

            assertThat(success.getData()).isSameAs(report);
            assertThat(missing.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIST.getCode());
            assertThat(missing.getMessage()).isEqualTo("事故不存在");
        }
    }

    @Test
    void historyAndDetailEndpointsAreAdminOnly() throws Exception {
        Method history = SreRcaAdminController.class.getMethod("history", Long.class, Integer.class);
        Method detail = SreRcaAdminController.class.getMethod("runDetail", Long.class, Long.class);

        assertThat(history.getAnnotation(GetMapping.class).value()).containsExactly("/{id}/rca-runs");
        assertThat(detail.getAnnotation(GetMapping.class).value()).containsExactly("/{id}/rca-runs/{runId}");
        assertThat(history.getAnnotation(RequireAdmin.class)).isNotNull();
        assertThat(detail.getAnnotation(RequireAdmin.class)).isNotNull();
    }

    @Test
    void historyClampsLimitAndDetailUsesStableNotFoundError() {
        SreIncidentRcaService service = mock(SreIncidentRcaService.class);
        SreRcaAdminController controller = new SreRcaAdminController(service);
        SreRcaRunSummary summary = mock(SreRcaRunSummary.class);
        SreRcaRunDetail detail = mock(SreRcaRunDetail.class);
        when(service.listRuns(11L, 50)).thenReturn(List.of(summary));
        when(service.getRun(11L, 91L)).thenReturn(Optional.of(detail));
        when(service.getRun(11L, 404L)).thenReturn(Optional.empty());

        assertThat(controller.history(11L, 999).getData()).containsExactly(summary);
        assertThat(controller.runDetail(11L, 91L).getData()).isSameAs(detail);
        assertThat(controller.runDetail(11L, 404L).getCode())
                .isEqualTo(ResultCode.DATA_NOT_EXIST.getCode());
        verify(service).listRuns(11L, 50);
    }

    private SreRcaReport report() {
        return new SreRcaReport(
                11L, "SRE-001", "FALLBACK", "INSUFFICIENT_EVIDENCE", "CRITICAL", "证据不足。",
                List.of(), List.of(), List.of(), List.of(), List.of("模型不可用"),
                false, false, LocalDateTime.of(2026, 7, 23, 1, 10));
    }
}
