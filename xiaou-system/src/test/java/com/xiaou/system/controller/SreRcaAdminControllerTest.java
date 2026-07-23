package com.xiaou.system.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.system.dto.SreRcaReport;
import com.xiaou.system.service.SreIncidentRcaService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
        when(service.investigate(11L)).thenReturn(Optional.of(report));
        when(service.investigate(404L)).thenReturn(Optional.empty());

        Result<SreRcaReport> success = controller.investigate(11L);
        Result<SreRcaReport> missing = controller.investigate(404L);

        assertThat(success.getData()).isSameAs(report);
        assertThat(missing.getCode()).isEqualTo(ResultCode.DATA_NOT_EXIST.getCode());
        assertThat(missing.getMessage()).isEqualTo("事故不存在");
    }

    private SreRcaReport report() {
        return new SreRcaReport(
                11L, "SRE-001", "FALLBACK", "INSUFFICIENT_EVIDENCE", "CRITICAL", "证据不足。",
                List.of(), List.of(), List.of(), List.of(), List.of("模型不可用"),
                false, false, LocalDateTime.of(2026, 7, 23, 1, 10));
    }
}
