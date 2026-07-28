package com.xiaou.system.service;

import com.xiaou.ai.support.AiExecutionResult;
import com.xiaou.system.dto.SreRcaReport;

/**
 * 线上 RCA 与离线评测共用的只读分析边界。
 *
 * @author xiaou
 */
public interface SreRcaAnalyzer {

    AiExecutionResult<SreRcaReport> analyze(SreRcaAnalysisInput input);

    SreRcaReport fallback(SreRcaAnalysisInput input);

    String promptId();

    String schemaId();
}
