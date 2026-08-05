package com.xiaou.sre.service.rca;

import com.xiaou.sre.dto.rca.SreRcaReport;

/**
 * 线上 RCA 与离线评测共用的只读分析边界。
 *
 * @author xiaou
 */
public interface SreRcaAnalyzer {

    SreModelExecution<SreRcaReport> analyze(SreRcaAnalysisInput input);

    SreRcaReport fallback(SreRcaAnalysisInput input);

    String promptId();

    String schemaId();
}
