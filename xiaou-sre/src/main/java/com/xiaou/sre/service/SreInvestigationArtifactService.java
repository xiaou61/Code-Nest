package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreInvestigationArtifact;
import com.xiaou.sre.dto.request.SreInvestigationArtifactCapture;

import java.util.Optional;

/**
 * 脱敏模型输入与 provenance 的不可变持久化边界。
 *
 * @author xiaou
 */
public interface SreInvestigationArtifactService {

    Optional<SreInvestigationArtifact> capture(SreInvestigationArtifactCapture capture);

    Optional<SreInvestigationArtifact> findByIncidentIdAndRunId(Long incidentId, Long runId);
}
