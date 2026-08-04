package com.xiaou.sre.service.impl;

import com.xiaou.common.utils.JsonUtils;
import com.xiaou.sre.config.SreOperationalEvidenceProperties;
import com.xiaou.sre.config.SreRcaEvaluationProperties;
import com.xiaou.sre.domain.SreAlertEvent;
import com.xiaou.sre.domain.SreIncident;
import com.xiaou.sre.domain.SreOutboxEvent;
import com.xiaou.sre.service.SreOperationalEvidence;
import com.xiaou.sre.service.SreOperationalEvidenceCollector;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads a fixed classpath runbook catalog and immutable build provenance.
 *
 * @author xiaou
 */
@Service
public class SreOperationalEvidenceCollectorImpl implements SreOperationalEvidenceCollector {

    static final String DEPLOYMENT_SOURCE_TYPE = "DEPLOYMENT_SNAPSHOT";
    static final String RUNBOOK_SOURCE_TYPE = "RUNBOOK_SNAPSHOT";
    private static final String CATALOG_RESOURCE = "sre/runbooks.json";
    private static final int MAX_CATALOG_BYTES = 128_000;
    private static final int MAX_SNAPSHOT_LENGTH = 32_000;

    private final SreOperationalEvidenceProperties properties;
    private final SreRcaEvaluationProperties evaluationProperties;
    private final List<RunbookDefinition> runbooks;

    public SreOperationalEvidenceCollectorImpl(SreOperationalEvidenceProperties properties,
                                               SreRcaEvaluationProperties evaluationProperties) {
        this.properties = properties;
        this.evaluationProperties = evaluationProperties;
        this.runbooks = loadRunbooks();
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public List<SreOperationalEvidence> collect(SreOutboxEvent event,
                                                SreAlertEvent alertEvent,
                                                SreIncident incident) {
        if (!isEnabled()) {
            return List.of();
        }

        List<SreOperationalEvidence> evidence = new ArrayList<>(2);
        evidence.add(deploymentEvidence());
        RunbookDefinition runbook = selectRunbook(alertEvent, incident);
        if (runbook != null) {
            evidence.add(runbookEvidence(runbook));
        }
        return List.copyOf(evidence);
    }

    private SreOperationalEvidence deploymentEvidence() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schema", "code-nest.sre.deployment-snapshot.v1");
        snapshot.put("environment", properties.normalizedEnvironment());
        snapshot.put("buildVersion", evaluationProperties.normalizedBuildVersion());
        snapshot.put("buildId", evaluationProperties.normalizedBuildId());
        snapshot.put("sourceRevision", evaluationProperties.normalizedSourceRevision());
        snapshot.put("readOnly", true);
        return evidence(
                DEPLOYMENT_SOURCE_TYPE,
                evaluationProperties.normalizedBuildId(),
                "deployment.provenance",
                snapshot);
    }

    private SreOperationalEvidence runbookEvidence(RunbookDefinition runbook) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schema", "code-nest.sre.runbook-snapshot.v1");
        snapshot.put("key", runbook.getKey());
        snapshot.put("title", runbook.getTitle());
        snapshot.put("objective", runbook.getObjective());
        snapshot.put("readOnly", true);
        snapshot.put("triageSteps", List.copyOf(runbook.getTriageSteps()));
        snapshot.put("escalationSignals", List.copyOf(runbook.getEscalationSignals()));
        return evidence(
                RUNBOOK_SOURCE_TYPE,
                runbook.getKey(),
                "runbook.catalog:" + runbook.getKey(),
                snapshot);
    }

    private SreOperationalEvidence evidence(String sourceType,
                                            String sourceRef,
                                            String query,
                                            Map<String, Object> snapshot) {
        String snapshotJson = JsonUtils.toJsonString(snapshot);
        if (!StringUtils.hasText(snapshotJson) || snapshotJson.length() > MAX_SNAPSHOT_LENGTH) {
            throw new IllegalStateException("SRE operational evidence exceeds the bounded snapshot size");
        }
        return new SreOperationalEvidence(sourceType, sourceRef, query, snapshotJson);
    }

    private RunbookDefinition selectRunbook(SreAlertEvent alertEvent, SreIncident incident) {
        String service = normalize(incident == null ? null : incident.getService());
        String alertName = normalize(alertEvent == null ? null : alertEvent.getAlertName());
        RunbookDefinition selected = null;
        int selectedScore = -1;
        for (RunbookDefinition runbook : runbooks) {
            int serviceScore = matchScore(runbook.getServices(), service);
            int alertScore = matchScore(runbook.getAlertNames(), alertName);
            if (serviceScore < 0 || alertScore < 0) {
                continue;
            }
            int score = serviceScore + alertScore;
            if (score > selectedScore) {
                selected = runbook;
                selectedScore = score;
            }
        }
        return selected;
    }

    private int matchScore(List<String> candidates, String value) {
        if (candidates == null || candidates.isEmpty()) {
            return -1;
        }
        boolean wildcard = false;
        for (String candidate : candidates) {
            String normalized = normalize(candidate);
            if ("*".equals(normalized)) {
                wildcard = true;
            } else if (normalized.equals(value)) {
                return 2;
            }
        }
        return wildcard ? 0 : -1;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "unknown";
    }

    private List<RunbookDefinition> loadRunbooks() {
        ClassPathResource resource = new ClassPathResource(CATALOG_RESOURCE);
        try (InputStream stream = resource.getInputStream()) {
            byte[] bytes = stream.readNBytes(MAX_CATALOG_BYTES + 1);
            if (bytes.length == 0 || bytes.length > MAX_CATALOG_BYTES) {
                throw new IllegalStateException("SRE runbook catalog is empty or too large");
            }
            RunbookCatalog catalog = JsonUtils.parseObject(
                    new String(bytes, StandardCharsets.UTF_8), RunbookCatalog.class);
            if (catalog == null || catalog.getRunbooks() == null || catalog.getRunbooks().isEmpty()) {
                throw new IllegalStateException("SRE runbook catalog has no definitions");
            }
            catalog.getRunbooks().forEach(this::validateRunbook);
            return List.copyOf(catalog.getRunbooks());
        } catch (IOException exception) {
            throw new IllegalStateException("SRE runbook catalog cannot be loaded", exception);
        }
    }

    private void validateRunbook(RunbookDefinition runbook) {
        if (runbook == null
                || !runbook.getKey().matches("[a-z][a-z0-9-]{2,63}")
                || !StringUtils.hasText(runbook.getTitle())
                || !StringUtils.hasText(runbook.getObjective())
                || runbook.getServices() == null
                || runbook.getAlertNames() == null
                || runbook.getTriageSteps() == null
                || runbook.getTriageSteps().isEmpty()
                || runbook.getTriageSteps().size() > 12
                || runbook.getEscalationSignals() == null
                || runbook.getEscalationSignals().size() > 12) {
            throw new IllegalStateException("SRE runbook catalog contains an invalid definition");
        }
    }

    @Data
    public static class RunbookCatalog {
        private String schema;
        private List<RunbookDefinition> runbooks;
    }

    @Data
    public static class RunbookDefinition {
        private String key;
        private List<String> services;
        private List<String> alertNames;
        private String title;
        private String objective;
        private List<String> triageSteps;
        private List<String> escalationSignals;
    }
}
