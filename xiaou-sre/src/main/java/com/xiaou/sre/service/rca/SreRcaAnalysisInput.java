package com.xiaou.sre.service.rca;

import com.xiaou.sre.dto.rca.SreRcaReport;

import java.util.List;

/**
 * 一次线上分析或离线回放所需的冻结 RCA 输入。
 *
 * @author xiaou
 */
public record SreRcaAnalysisInput(
        String contextJson,
        Long incidentId,
        String incidentNo,
        String severity,
        List<SreRcaReport.EvidenceReference> evidenceReferences,
        boolean contextTruncated
) {

    public SreRcaAnalysisInput {
        evidenceReferences = evidenceReferences == null ? List.of() : List.copyOf(evidenceReferences);
    }
}
