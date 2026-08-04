package com.xiaou.ai.structured.sre;

import com.xiaou.ai.prompt.sre.SreInvestigationPromptSpecs;
import com.xiaou.ai.structured.AiStructuredOutputSpec;

import java.util.Set;

/**
 * SRE 有界只读调查计划结构化输出契约。
 *
 * @author xiaou
 */
public final class SreInvestigationStructuredOutputSpecs {

    public static final AiStructuredOutputSpec PLAN = AiStructuredOutputSpec.object(
            SreInvestigationPromptSpecs.PLAN,
            validator -> validator
                    .requireStringInSet("decision", Set.of("INVESTIGATE", "STOP"))
                    .requireString("reason")
                    .requireStringArray("toolKeys")
    );

    private SreInvestigationStructuredOutputSpecs() {
    }
}
