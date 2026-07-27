package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreInvestigationArtifact;
import com.xiaou.sre.domain.SreInvestigationRun;
import com.xiaou.sre.dto.request.SreInvestigationArtifactCapture;
import com.xiaou.sre.mapper.SreInvestigationArtifactMapper;
import com.xiaou.sre.mapper.SreInvestigationRunMapper;
import com.xiaou.sre.service.impl.SreInvestigationArtifactServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreInvestigationArtifactServiceImplTest {

    @Mock
    private SreInvestigationRunMapper runMapper;

    @Mock
    private SreInvestigationArtifactMapper artifactMapper;

    @Test
    void capturePersistsImmutableBoundedReplayInputAndProvenance() {
        SreInvestigationArtifactServiceImpl service = service();
        String contextJson = "{\"incident\":{\"id\":11},\"password\":\"[REDACTED]\"}";
        when(runMapper.selectByIncidentIdAndId(11L, 91L)).thenReturn(run());
        doAnswer(invocation -> {
            invocation.<SreInvestigationArtifact>getArgument(0).setId(201L);
            return 1;
        }).when(artifactMapper).insert(any(SreInvestigationArtifact.class));

        Optional<SreInvestigationArtifact> saved = service.capture(new SreInvestigationArtifactCapture(
                11L,
                91L,
                contextJson,
                true,
                "sre.incident.rca:v1",
                "xiaou://ai/structured-output/sre.incident.rca:v1",
                "openai-compatible",
                "configured-model",
                "runtime-model",
                "SUCCESS"
        ));

        assertThat(saved).isPresent();
        ArgumentCaptor<SreInvestigationArtifact> captor =
                ArgumentCaptor.forClass(SreInvestigationArtifact.class);
        verify(artifactMapper).insert(captor.capture());
        SreInvestigationArtifact artifact = captor.getValue();
        assertThat(artifact.getId()).isEqualTo(201L);
        assertThat(artifact.getIncidentId()).isEqualTo(11L);
        assertThat(artifact.getRunId()).isEqualTo(91L);
        assertThat(artifact.getContextJson()).isEqualTo(contextJson);
        assertThat(artifact.getContextLength()).isEqualTo(contextJson.length());
        assertThat(artifact.getContextSha256()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(artifact.getContextTruncated()).isTrue();
        assertThat(artifact.getPromptId()).isEqualTo("sre.incident.rca:v1");
        assertThat(artifact.getSchemaId())
                .isEqualTo("xiaou://ai/structured-output/sre.incident.rca:v1");
        assertThat(artifact.getProvider()).isEqualTo("openai-compatible");
        assertThat(artifact.getConfiguredModel()).isEqualTo("configured-model");
        assertThat(artifact.getActualModel()).isEqualTo("runtime-model");
        assertThat(artifact.getInvocationOutcome()).isEqualTo("SUCCESS");
        assertThat(artifact.getCreatedAt()).isNotNull();
    }

    @Test
    void captureRejectsOversizedOrObviouslyUnredactedCredentials() {
        SreInvestigationArtifactServiceImpl service = service();
        when(runMapper.selectByIncidentIdAndId(11L, 91L)).thenReturn(run());

        assertThatThrownBy(() -> service.capture(new SreInvestigationArtifactCapture(
                11L, 91L, "x".repeat(60_001), false,
                "sre.incident.rca:v1", "xiaou://schema", "provider", "model", null, "SUCCESS")))
                .isInstanceOf(SreValidationException.class)
                .hasMessage("调查回放上下文大小不合法");
        assertThatThrownBy(() -> service.capture(new SreInvestigationArtifactCapture(
                11L, 91L, "{\"message\":\"Bearer super-secret-token\"}", false,
                "sre.incident.rca:v1", "xiaou://schema", "provider", "model", null, "SUCCESS")))
                .isInstanceOf(SreValidationException.class)
                .hasMessage("调查回放上下文包含未脱敏凭据");
        assertThatThrownBy(() -> service.capture(new SreInvestigationArtifactCapture(
                11L, 91L, "{\"password\":\"do-not-store\"}", false,
                "sre.incident.rca:v1", "xiaou://schema", "provider", "model", null, "SUCCESS")))
                .isInstanceOf(SreValidationException.class)
                .hasMessage("调查回放上下文包含未脱敏凭据");

        verify(artifactMapper, never()).insert(any());
    }

    @Test
    void captureAndReadCannotCrossIncidentBoundary() {
        SreInvestigationArtifactServiceImpl service = service();
        when(runMapper.selectByIncidentIdAndId(12L, 91L)).thenReturn(null);

        assertThat(service.capture(new SreInvestigationArtifactCapture(
                12L, 91L, "{}", false,
                "sre.incident.rca:v1", "xiaou://schema", "provider", "model", null, "SUCCESS")))
                .isEmpty();
        assertThat(service.findByIncidentIdAndRunId(12L, 91L)).isEmpty();

        verify(artifactMapper, never()).insert(any());
        verify(artifactMapper, never()).selectByIncidentIdAndRunId(any(), any());
    }

    private SreInvestigationArtifactServiceImpl service() {
        return new SreInvestigationArtifactServiceImpl(runMapper, artifactMapper);
    }

    private SreInvestigationRun run() {
        SreInvestigationRun run = new SreInvestigationRun();
        run.setId(91L);
        run.setIncidentId(11L);
        return run;
    }
}
