package com.xiaou.sre.worker;

import com.xiaou.sre.config.SreOutboxProperties;
import com.xiaou.sre.metrics.SreMetricsRecorder;
import com.xiaou.sre.metrics.SreOutboxMetricsRecorder;
import com.xiaou.sre.service.SreOutboxClaimService;
import com.xiaou.sre.service.SreOutboxEventProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SreOutboxWorkerSpringWiringTest {

    @Test
    void springCanResolveProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SreOutboxProperties.class, SreOutboxProperties::new);
            context.registerBean(SreOutboxClaimService.class, () -> mock(SreOutboxClaimService.class));
            context.registerBean(SreOutboxEventProcessor.class, () -> mock(SreOutboxEventProcessor.class));
            context.registerBean(SreMetricsRecorder.class, () -> mock(SreMetricsRecorder.class));
            context.registerBean(SreOutboxMetricsRecorder.class, SreOutboxMetricsRecorder::noop);
            context.register(SreOutboxWorker.class);

            assertDoesNotThrow(context::refresh);
            assertNotNull(context.getBean(SreOutboxWorker.class));
        }
    }
}
