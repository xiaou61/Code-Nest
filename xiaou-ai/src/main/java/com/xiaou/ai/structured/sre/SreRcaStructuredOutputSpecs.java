package com.xiaou.ai.structured.sre;

import com.xiaou.ai.prompt.sre.SreRcaPromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputSpec;

import java.util.Set;

/**
 * SRE 根因分析结构化输出契约。
 *
 * @author xiaou
 */
public final class SreRcaStructuredOutputSpecs {

    public static final AiStructuredOutputSpec REPORT = AiStructuredOutputSpec.object(
            SreRcaPromptSpecs.INVESTIGATE,
            validator -> validator
                    .requireString("executiveSummary")
                    .requireStringInSet("severityAssessment",
                            Set.of("CRITICAL", "HIGH", "MEDIUM", "LOW", "UNKNOWN"))
                    .requireStringInSet("conclusionStatus",
                            Set.of("SUPPORTED", "PARTIAL", "INSUFFICIENT_EVIDENCE"))
                    .requireObjectArray("observations", item -> item
                            .requireString("statement")
                            .requireStringArray("evidenceIds"))
                    .requireObjectArray("hypotheses", item -> item
                            .requireString("title")
                            .requireString("reasoning")
                            .requireNumberRange("confidence", 0D, 1D)
                            .requireStringArray("evidenceIds")
                            .requireStringArray("counterEvidenceIds")
                            .requireStringArray("nextChecks"))
                    .requireObjectArray("recommendedNextSteps", item -> item
                            .requireString("description")
                            .requireStringInSet("risk", Set.of("READ_ONLY", "PROPOSE_ONLY"))
                            .requireStringArray("evidenceIds"))
                    .requireStringArray("limitations")
    );

    private SreRcaStructuredOutputSpecs() {
    }
}
