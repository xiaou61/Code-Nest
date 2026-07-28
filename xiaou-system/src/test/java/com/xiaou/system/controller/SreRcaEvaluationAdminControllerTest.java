package com.xiaou.system.controller;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.annotation.RequireAdmin;
import com.xiaou.common.satoken.StpAdminUtil;
import com.xiaou.system.dto.SreRcaEvaluationCaseSummary;
import com.xiaou.system.dto.SreRcaEvaluationPromotionRequest;
import com.xiaou.system.dto.SreRcaEvaluationRunDetail;
import com.xiaou.system.dto.SreRcaEvaluationRunRequest;
import com.xiaou.system.dto.SreRcaEvaluationSuiteCreateRequest;
import com.xiaou.system.dto.SreRcaEvaluationSuiteVersionPublishRequest;
import com.xiaou.system.service.SreRcaEvaluationService;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SreRcaEvaluationAdminControllerTest {

    @Test
    void mutationEndpointsAreAdminOnlyAndDoNotLogEvaluationPayloads() throws Exception {
        Method promote = SreRcaEvaluationAdminController.class.getMethod(
                "promote", Long.class, Long.class, SreRcaEvaluationPromotionRequest.class);
        Method run = SreRcaEvaluationAdminController.class.getMethod(
                "run", SreRcaEvaluationRunRequest.class);
        Method createSuite = SreRcaEvaluationAdminController.class.getMethod(
                "createSuite", SreRcaEvaluationSuiteCreateRequest.class);
        Method publishVersion = SreRcaEvaluationAdminController.class.getMethod(
                "publishSuiteVersion", Long.class, SreRcaEvaluationSuiteVersionPublishRequest.class);
        Method gate = SreRcaEvaluationAdminController.class.getMethod("gate", Long.class);

        assertThat(promote.getAnnotation(PostMapping.class).value())
                .containsExactly("/incidents/{id}/rca-runs/{runId}/evaluation-cases");
        assertThat(run.getAnnotation(PostMapping.class).value())
                .containsExactly("/rca-evaluations/runs");
        assertThat(createSuite.getAnnotation(PostMapping.class).value())
                .containsExactly("/rca-evaluations/suites");
        assertThat(publishVersion.getAnnotation(PostMapping.class).value())
                .containsExactly("/rca-evaluations/suites/{suiteId}/versions");
        assertThat(gate.getAnnotation(GetMapping.class).value())
                .containsExactly("/rca-evaluations/runs/{runId}/gate");
        assertThat(promote.getAnnotation(RequireAdmin.class)).isNotNull();
        assertThat(run.getAnnotation(RequireAdmin.class)).isNotNull();
        assertThat(createSuite.getAnnotation(RequireAdmin.class)).isNotNull();
        assertThat(publishVersion.getAnnotation(RequireAdmin.class)).isNotNull();
        assertThat(gate.getAnnotation(RequireAdmin.class)).isNotNull();
        assertThat(promote.getAnnotation(Log.class).saveRequestData()).isFalse();
        assertThat(promote.getAnnotation(Log.class).saveResponseData()).isFalse();
        assertThat(run.getAnnotation(Log.class).saveRequestData()).isFalse();
        assertThat(run.getAnnotation(Log.class).saveResponseData()).isFalse();
        assertThat(createSuite.getAnnotation(Log.class).saveRequestData()).isFalse();
        assertThat(createSuite.getAnnotation(Log.class).saveResponseData()).isFalse();
        assertThat(publishVersion.getAnnotation(Log.class).saveRequestData()).isFalse();
        assertThat(publishVersion.getAnnotation(Log.class).saveResponseData()).isFalse();
        assertThat(promote.getParameters()[2].getAnnotation(Valid.class)).isNotNull();
        assertThat(promote.getParameters()[2].getAnnotation(RequestBody.class)).isNotNull();
    }

    @Test
    void authenticatedAdministratorOwnsPromotionAndManualRun() {
        SreRcaEvaluationService service = mock(SreRcaEvaluationService.class);
        SreRcaEvaluationAdminController controller = new SreRcaEvaluationAdminController(service);
        SreRcaEvaluationCaseSummary summary = mock(SreRcaEvaluationCaseSummary.class);
        SreRcaEvaluationRunDetail detail = mock(SreRcaEvaluationRunDetail.class);
        when(service.promote(11L, 91L, 101L, 7L)).thenReturn(Optional.of(summary));
        when(service.run(301L, null, 7L)).thenReturn(detail);

        try (MockedStatic<StpAdminUtil> stpAdminUtil = mockStatic(StpAdminUtil.class)) {
            stpAdminUtil.when(StpAdminUtil::getLoginIdAsLong).thenReturn(7L);

            assertThat(controller.promote(
                    11L, 91L, new SreRcaEvaluationPromotionRequest(101L)).getData()).isSameAs(summary);
            assertThat(controller.run(new SreRcaEvaluationRunRequest(301L)).getData()).isSameAs(detail);
        }

        verify(service).promote(11L, 91L, 101L, 7L);
        verify(service).run(301L, null, 7L);
    }
}
