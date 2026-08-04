package com.xiaou.sre.service;

import com.xiaou.sre.config.SreOperationalEvidenceProperties;
import com.xiaou.sre.config.SreRcaEvaluationProperties;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.service.impl.SreOperationalEvidenceCollectorImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SreOperationalEvidenceCollectorImplTest {

    @Test
    void shouldCreateBoundedDeploymentAndMatchingRunbookEvidence() {
        SreOperationalEvidenceProperties properties = new SreOperationalEvidenceProperties();
        properties.setEnabled(true);
        properties.setEnvironment("production\nignored");
        SreRcaEvaluationProperties evaluationProperties = new SreRcaEvaluationProperties();
        evaluationProperties.setSourceRevision("013d71337de33948536e8c2da97cad86a0939668");
        evaluationProperties.setBuildId("v2.5.1-013d713");
        evaluationProperties.setBuildVersion("2.5.1");
        SreOperationalEvidenceCollectorImpl collector = new SreOperationalEvidenceCollectorImpl(
                properties, evaluationProperties);
        SreAlertEvent alert = new SreAlertEvent();
        alert.setAlertName("CodeNestTargetDown");
        SreIncident incident = new SreIncident();
        incident.setService("code-nest");

        List<SreOperationalEvidence> result = collector.collect(new SreOutboxEvent(), alert, incident);

        assertThat(result).extracting(SreOperationalEvidence::sourceType)
                .containsExactly("DEPLOYMENT_SNAPSHOT", "RUNBOOK_SNAPSHOT");
        assertThat(result.get(0).snapshotJson())
                .contains("013d71337de33948536e8c2da97cad86a0939668", "v2.5.1-013d713", "2.5.1")
                .doesNotContain("\n");
        assertThat(result.get(1).sourceRef()).isEqualTo("code-nest-availability");
        assertThat(result.get(1).snapshotJson())
                .contains("Code Nest availability", "readOnly")
                .doesNotContain("password", "token", "secret");
        assertThat(result).allSatisfy(evidence -> {
            assertThat(evidence.snapshotJson()).hasSizeLessThanOrEqualTo(32_000);
            assertThat(evidence.query()).doesNotContain("shell", "sql");
        });
    }

    @Test
    void disabledCollectorReturnsNoEvidence() {
        SreOperationalEvidenceProperties properties = new SreOperationalEvidenceProperties();
        SreOperationalEvidenceCollectorImpl collector = new SreOperationalEvidenceCollectorImpl(
                properties, new SreRcaEvaluationProperties());

        assertThat(collector.collect(new SreOutboxEvent(), new SreAlertEvent(), new SreIncident()))
                .isEmpty();
    }
}
